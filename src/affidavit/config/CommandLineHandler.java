package affidavit.config;

import java.io.File;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import affidavit.db.BuildDatabase;
import affidavit.main.Main;

public class CommandLineHandler {
	public static Options get() {
		Options options = new Options();

		options.addOption("s", true, "Source CSV");
		options.addOption("t", true, "Target CSV");
		options.addOption("sep", true, "CSV Separator");
		options.addOption("v", false, "Verbose Mode");

		options.addOption("init", true, "Initialization Strategy");
		options.addOption("noise", true, "Theta");
		options.addOption("conf", true, "Confidence");
		options.addOption("a", true, "Alpha");
		options.addOption("q", true, "Queue Width");
		options.addOption("b", true, "Branching Factor");
		options.addOption("bsize", true, "Maximum Block Size for Overlap Sampling");

		return options;
	}

	public static void handle(CommandLine cmd) throws SQLException {
		if (cmd.hasOption("init")) {
			Config.INITIALIZATION_STRATEGY = InitializationStrategy.valueOf(cmd.getOptionValue("init"));
		}

		if (cmd.hasOption("noise")) {
			Config.NOISE = Double.valueOf(cmd.getOptionValue("noise"));
		}

		if (cmd.hasOption("conf")) {
			Config.CONFIDENCE = Double.valueOf(cmd.getOptionValue("conf"));
		}

		if (cmd.hasOption("a")) {
			Config.ALPHA = Integer.valueOf(cmd.getOptionValue("a"));
		}

		if (cmd.hasOption("q")) {
			Config.QUEUE_WIDTH = Integer.valueOf(cmd.getOptionValue("q"));
		}

		if (cmd.hasOption("b")) {
			Config.BRANCHING_FACTOR = Integer.valueOf(cmd.getOptionValue("b"));
		}

		if (cmd.hasOption("bsize")) {
			Config.MAX_MATCHING_BLOCK_SIZE = Integer.valueOf(cmd.getOptionValue("bsize"));
		}

		if (cmd.hasOption("v")) {
			Config.VERBOSE_MODE = true;
		}

		if (cmd.hasOption("sep")) {
			Config.CSV_SEPARATOR_CHAR = cmd.getOptionValue("sep").charAt(0);
		}

		String sourceCSV = cmd.getOptionValue("s");
		String targetCSV = cmd.getOptionValue("t");
		BuildDatabase.temporarilyFrom(new File(sourceCSV), new File(targetCSV));
		Main.start(Config.TMP_DB_PATH, Config.TMP_SOURCE_NAME, Config.TMP_TARGET_NAME);
	}
}
