package org.ivica.demo.tcpping.catcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ping service
 * 
 * @author IVICA
 * 
 */
public class Catcher {

	private int port;
	private String address;
	private static Logger logger = LogManager.getLogger(Catcher.class.getName());

	/**
	 * Catcher constructor
	 * 
	 */
	public Catcher(String address, int port) {
		this.address = address;
		this.port = port;
	}

	/**
	 * Starts the ping catching by opening a blocking server socket channel
	 * and listening for connections.
	 * Returns the received packet to the sender, ie. pitcher.
	 */
	public void startCatching() {
		
		// only one client is expected
		SocketChannel clientChannel = null;
		try (ServerSocketChannel serverChannel = ServerSocketChannel.open();) {
			
			SocketAddress sockeaddress = new InetSocketAddress(address, port);
		    serverChannel.socket().bind(sockeaddress);
		    logger.info("Catcher started on {}:{}", address, port);
		    clientChannel = serverChannel.accept();
			logger.info("Accepted client connection {}", clientChannel);
			
		    ByteBuffer message = ByteBuffer.allocate(4);
		    // read the length of ping packets 
			clientChannel.read(message);
			int len = message.getInt(0);
			logger.debug("Ping packets size will be {} bytes", len);
			
			while (true) {
				message = ByteBuffer.allocate(len);
				int readBytes = clientChannel.read(message);
				if (readBytes == -1) break;
				message.flip();
				
				// record the absolute time when the pitchers ping packet reached
				// the catcher, this method of using the system time is unreliable because there is 
				// no guarantee the clocks are synchronized on the pitcher and catcher hosts
				long timeAToB = System.currentTimeMillis();
				logger.debug("Host B timestamp: {}", timeAToB);
				logger.debug("Read {} bytes", readBytes);
				long messageId = message.getLong();
				long hostATimestamp = message.getLong();
				logger.debug("Received message with Id: {}", messageId);
				logger.debug("Received message with host A timestamp: {}", hostATimestamp);
					
				// put the measured A to B time in the message at offset 16
				// the buffer is already positioned at position 16 due to
				// two getLong-s
				message.putLong(timeAToB);
				
				// fill up the rest of the message with useless data
				while (message.hasRemaining())
					message.put((byte) 0xFF);
				
				message.flip();
				
				// simulate the loss of packet with id 2
				//long id = message.getLong(0);
				//if (id == 2)
				//	continue;
					
				clientChannel.write(message);
			}
		} catch (Exception e) {
			logger.fatal(e.getMessage());
			e.printStackTrace();
		} finally {
			if (clientChannel != null)
	            try {
	                clientChannel.close();
                } catch (IOException e1) {
                	logger.fatal(e1.getMessage());
                }
		}
	}
}
