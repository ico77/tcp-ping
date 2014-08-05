package org.ivica.demo.tcpping.pitcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class responsible for generating ping packets and generating statistics for
 * measuring the round trip time for the generated ping packets
 * 
 * @author IVICA
 * 
 */
public class Pitcher {

	private int port;
	private int mps;
	private int size;
	private String host;

	private static final long PING_DURATION = 30;
	private volatile long messageId = 0;
	private long maxRtt = -1;
	private List<PingPacketStatistics> rttStatistics = new LinkedList<PingPacketStatistics>();
	private static Logger logger = LogManager.getLogger(Pitcher.class.getName());

	/**
	 * Pitcher constructor
	 * 
	 * @param host
	 *            the remote host to whom ping packets will be sent to
	 * @param port
	 *            the port on the remote host
	 * @param mps
	 *            messages per second, the number of ping packets generated
	 *            every second
	 * @param size
	 *            the size of a generated ping packet
	 */
	public Pitcher(String host, int port, int mps, int size) {
		this.port = port;
		this.mps = mps;
		this.size = size;
		this.host = host;
	}

	/**
	 * Generates a new message id
	 * 
	 * @return new message id
	 */
	private long getNextMessageId() {
		return ++messageId;
	}

	/**
	 * Get last message id
	 * 
	 * @return last message id
	 */
	private long getMessageId() {
		return messageId;
	}

	/**
	 * Stores statistics for a single ping packet.
	 * 
	 * @param messageId
	 *            the message id
	 * @param stats
	 *            statistics for a ping packet
	 */
	private void putItemToRttStatistics(PingPacketStatistics stats) {
		synchronized (rttStatistics) {
			rttStatistics.add(stats);
		}
	}

	private void calculateRttStatistics() {
		Calendar cal = Calendar.getInstance();
		cal.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		logger.info("\n********************** {} *****************************", sdf.format(cal.getTime()));

		synchronized (rttStatistics) {
			logger.info("Total number of messages sent so far: {}", getMessageId());
			logger.info("Number of messages received in the previous second: {}", rttStatistics.size());

			if (rttStatistics.size() == 0)
				return;

			long sumRtt = 0;
			long sumAToB = 0;
			long sumBToA = 0;

			for (PingPacketStatistics stat : rttStatistics) {
				long currentRtt = stat.getRttTimestamp() - stat.getHostATimestamp();
				if (currentRtt > maxRtt) {
					maxRtt = currentRtt;
				}

				sumRtt += currentRtt;
				sumAToB += stat.getHostBTimestamp() - stat.getHostATimestamp();
				sumBToA += stat.getRttTimestamp() - stat.getHostBTimestamp();
			}

			logger.info("Max RTT: {} ms, Avg RTT: {} ms, Avg A->B: {} ms, Avg B->A: {} ms", maxRtt, sumRtt / rttStatistics.size(),
			        sumAToB / rttStatistics.size(), sumBToA / rttStatistics.size());
			rttStatistics.clear();
		}
	}

	/**
	 * Generates ping packets at regular intervals specified by the pitchers mps
	 * field. Sends the packets to the host specified by the pitchers host field
	 * using port specified by the pitchers port field. Stores the measured
	 * statistics in pitchers rttStatistics field
	 */
	public void startPitching() {
		try (SocketChannel client = SocketChannel.open();) {

			client.configureBlocking(false);
			client.connect(new InetSocketAddress(host, port));

			while (!client.finishConnect()) {
				// waste cpu cycles
			}

			// create a ScheduledExecutorService
			// it will allocate threads for:
			// -sending and receiving ping packets at regular intervals,
			// -calculating statistics
			// -stoping the pitcher after PING_DURATION seconds
			final ScheduledExecutorService service = Executors.newScheduledThreadPool(4);

			// send the length of ping packets to the remote host
			ByteBuffer message = ByteBuffer.allocate(4);
			message.putInt(size);
			message.flip();

			client.write(message);

			service.scheduleAtFixedRate(new Runnable() {
				public void run() {
					// prepare ping packet
					ByteBuffer message = ByteBuffer.allocate(size);
					long nextMessageId = getNextMessageId();
					message.putLong(nextMessageId);
					message.putLong(System.currentTimeMillis());
					while (message.hasRemaining())
						message.put((byte) 0xFF);
					message.flip();

					// sending the ping packet and waiting for the response
					// with a 5 second timeout
					try {
						client.write(message);

						message.clear();

						Selector selector = Selector.open();
						// register the intent to read the response
						client.register(selector, SelectionKey.OP_READ);
						// wait for 5 seconds
						selector.select(5000);

						int readBytes = -1;
						Set<SelectionKey> selectedKeys = selector.selectedKeys();
						Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

						while (keyIterator.hasNext()) {
							SelectionKey key = keyIterator.next();

							if (key.isReadable()) {
								// the channel is ready for reading
								readBytes = client.read(message);
							}

							keyIterator.remove();
						}

						logger.debug("Read {} bytes", readBytes);
						if (readBytes == -1)
							logger.info("Did not receive response for message {}", nextMessageId);
						else if (readBytes != size)
							logger.info("Received {}, expected {} bytes", readBytes, size);
						else {
							message.flip();
							long rttTimestamp = System.currentTimeMillis();
							long messageId = message.getLong();
							long hostATimestamp = message.getLong();
							long hostBTimestamp = message.getLong();

							logger.debug("Received message with Id: {}", messageId);
							logger.debug("Received message with host A timestamp: {}", hostATimestamp);
							logger.debug("Received message with host B timestamp: {}", hostBTimestamp);
							logger.debug("Received message with host RTT timestamp: {}", rttTimestamp);

							// create a RTT statistics object for the current
							// ping packet
							PingPacketStatistics currentStats = new PingPacketStatistics(messageId, hostATimestamp, hostBTimestamp, rttTimestamp);
							putItemToRttStatistics(currentStats);
						}
					} catch (IOException e) {
						logger.fatal(e.getMessage());
						service.shutdownNow();
					}

				}
			}, 0, Math.round(1d / mps * 1000), TimeUnit.MILLISECONDS);

			service.scheduleAtFixedRate(new Runnable() {
				public void run() {
					calculateRttStatistics();
				}
			}, 1, 1, TimeUnit.SECONDS);

			// stop all activities after PING_DURATION seconds
			service.schedule(new Runnable() {
				public void run() {
					calculateRttStatistics();
					service.shutdown();
				}
			}, PING_DURATION, TimeUnit.SECONDS);

			service.awaitTermination(PING_DURATION + 5, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
		}
	}

	/**
	 * Immutable class responsible for recording round trip time statistics for
	 * a single ping packet
	 * 
	 * @author IVICA
	 * 
	 */
	private class PingPacketStatistics {

		private long messageId;
		private long hostATimestamp;
		private long hostBTimestamp;
		private long rttTimestamp;

		public long getHostATimestamp() {
			return hostATimestamp;
		}

		public long getHostBTimestamp() {
			return hostBTimestamp;
		}

		public long getRttTimestamp() {
			return rttTimestamp;
		}

		public long getMessageId() {
			return messageId;
		}

		public PingPacketStatistics(long messageId, long hostATimestamp, long hostBTimestamp, long rttTimestamp) {
			this.messageId = messageId;
			this.hostATimestamp = hostATimestamp;
			this.hostBTimestamp = hostBTimestamp;
			this.rttTimestamp = rttTimestamp;
		}
	}
}
