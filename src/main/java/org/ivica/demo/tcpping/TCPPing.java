package org.ivica.demo.tcpping;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ivica.demo.tcpping.catcher.Catcher;
import org.ivica.demo.tcpping.pitcher.Pitcher;

public class TCPPing {

	private static final int DEFAULT_MSG_PER_SECOND = 1;
	private static final int MIN_MESSAGE_SIZE = 50;
	private static final int MAX_MESSAGE_SIZE = 3000;
	private static final int DEFAULT_MESSAGE_SIZE = 300;
	private static Options options = new Options();
	private static Logger logger = LogManager.getLogger(TCPPing.class.getName());

	@SuppressWarnings("static-access")
	private static CommandLine parseCommandLine(String[] args) throws ParseException {

		// add options
		options.addOption("p", false, "Naèin rada kao Pitcher");
		options.addOption("c", false, "Naèin rada kao Catcher");

		Option port = OptionBuilder.withArgName("port").hasArg()
		        .withDescription("[Pitcher] TCP socket port koji æe se koristiti za connect\n[Catcher] TCP socket port koji æe se koristiti za listen")
		        .create("port");
		options.addOption(port);

		Option bind = OptionBuilder.withArgName("ip_address").hasArg().withDescription("[Catcher] TCP socket bind adresa na kojoj æe biti pokrenut listen")
		        .create("bind");
		options.addOption(bind);

		Option mps = OptionBuilder.withArgName("rate").hasArg().withDescription("[Pitcher] brzina slanja izražena u „messages per second“\nDefault: 1")
		        .create("mps");
		options.addOption(mps);

		Option size = OptionBuilder.withArgName("size").hasArg().withDescription("[Pitcher] dužina poruke\nMinimum: 50, Maximum: 3000,	Default: 300")
		        .create("size");
		options.addOption(size);

		CommandLineParser parser = new BasicParser();
		return parser.parse(options, args);
	}

	private static void showUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("TCPPing", options);
	}

	public static void main(String[] args) {

		try {
			CommandLine cmd = TCPPing.parseCommandLine(args);
			if (cmd.hasOption("p") && cmd.hasOption("port")) {
				String[] unassignedArgs = cmd.getArgs();
				if (unassignedArgs.length != 1)
					showUsage();

				int mps = cmd.hasOption("mps") ? Integer.parseInt(cmd.getOptionValue("mps")) : DEFAULT_MSG_PER_SECOND;

				int size;
				if (cmd.hasOption("size")) {
					size = Integer.parseInt(cmd.getOptionValue("size"));
					if (!(size >= MIN_MESSAGE_SIZE && size <= MAX_MESSAGE_SIZE)) {
						size = DEFAULT_MESSAGE_SIZE;
					}
				} else {
					size = DEFAULT_MESSAGE_SIZE;
				}

				Pitcher pitcher = new Pitcher(unassignedArgs[0], Integer.parseInt(cmd.getOptionValue("port")), mps, size);
				pitcher.startPitching();
			} else if (cmd.hasOption("c") && cmd.hasOption("port") && cmd.hasOption("bind")) {
				Catcher catcher = new Catcher(cmd.getOptionValue("bind"), Integer.parseInt(cmd.getOptionValue("port")));
				catcher.startCatching();
			} else {
				showUsage();
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
			showUsage();
		}
	}
}
