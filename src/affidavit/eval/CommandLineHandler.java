package affidavit.eval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import affidavit.config.Config;
import affidavit.config.DataSets;

public class CommandLineHandler {
	public static Options getOptions() {
		Options options = new Options();

		options.addOption("t", true, "Target parent folder with test cases");
		options.addOption("f", true, "File with data set names to evaluate");
		options.addOption("n0", true, "Start Number of Test Cases");
		options.addOption("n", true, "End Number of Test Cases (inclusive)");
		options.addOption("c", true, "File with names of settings to evaluate");
		options.addOption("a", true, "File with names of algorithm settings to evaluate");
		options.addOption("b", true, "Branching Factor");
		options.addOption("q", true, "Queue Size");
		options.addOption("bsize", true, "Maximum Block Size for Matching");
		options.addOption("v", false, "Verbose Mode");
		options.addOption("vmaps", false, "Verbose Maps Mode");

		return options;
	}

	public static void handle(CommandLine cmd) throws IOException, SQLException {
		String datasetPath = cmd.getOptionValue("t");
		String nameFile = cmd.getOptionValue("f");
		String rStartString = cmd.getOptionValue("n0");
		String rEndString = cmd.getOptionValue("n");
		String queueSizeString = cmd.getOptionValue("q");
		String branchingFactorString = cmd.getOptionValue("b");
		String blockSizeString = cmd.getOptionValue("bsize");

		Collection<TestcaseSetting> settings = readTestcaseSettings(cmd.getOptionValue("c"));

		Collection<AlgorithmSetting> algoSettings = new ArrayList<>();
		if (cmd.hasOption("a")) {
			algoSettings.addAll(readAlgorithmSettings(cmd.getOptionValue("a")));
		} else {
			algoSettings = Arrays.asList(AlgorithmSetting.values());
		}

		Collection<DataSets> dataSets = readDataSets(nameFile);
		int rStart = Integer.valueOf(rStartString);
		int rEnd = Integer.valueOf(rEndString);
		Config.QUEUE_WIDTH = Integer.valueOf(queueSizeString);
		Config.BRANCHING_FACTOR = Integer.valueOf(branchingFactorString);
		Config.MAX_MATCHING_BLOCK_SIZE = Integer.valueOf(blockSizeString);

		if (cmd.hasOption("v")) {
			Config.VERBOSE_MODE = true;
		} else {
			Config.VERBOSE_MODE = false;
		}

		if (cmd.hasOption("vmaps")) {
			Config.PRINT_MAPS = true;
		} else {
			Config.PRINT_MAPS = false;
		}

		Evaluator.evaluateSuite(datasetPath, dataSets, settings, algoSettings, rStart, rEnd);
	}

	private static Collection<? extends AlgorithmSetting> readAlgorithmSettings(String nameFile) throws IOException {
		Collection<AlgorithmSetting> settings = new ArrayList<>();

		try (BufferedReader in = new BufferedReader(new FileReader(nameFile))) {
			String settingName;
			while ((settingName = in.readLine()) != null) {
				if (!settingName.startsWith("#") && !settingName.startsWith("//")) {
					try {
						settings.add(AlgorithmSetting.valueOf(settingName));
					} catch (IllegalArgumentException e) {
						System.out.println(settingName + " is not a valid algortihm setting enum name.");
					}
				}
			}
		}

		return settings;
	}

	private static Collection<TestcaseSetting> readTestcaseSettings(String nameFile) throws IOException {
		Collection<TestcaseSetting> settings = new ArrayList<>();

		try (BufferedReader in = new BufferedReader(new FileReader(nameFile))) {
			String settingName;
			while ((settingName = in.readLine()) != null) {
				if (!settingName.startsWith("#") && !settingName.startsWith("//")) {
					try {
						String[] tokens = settingName.split("\\s");
						settings.add(new TestcaseSetting(Double.valueOf(tokens[0]), Double.valueOf(tokens[1])));
					} catch (Exception e) {
						System.out.println(settingName + " is not a valid setting enum name.");
					}
				}
			}
		}

		return settings;
	}

	private static Collection<DataSets> readDataSets(String nameFile) throws IOException {
		Collection<DataSets> dataSets = new ArrayList<>();

		try (BufferedReader in = new BufferedReader(new FileReader(nameFile))) {
			String dataSetName;
			while ((dataSetName = in.readLine()) != null) {
				if (!dataSetName.startsWith("#") && !dataSetName.startsWith("//")) {
					try {
						dataSets.add(DataSets.valueOf(dataSetName));
					} catch (IllegalArgumentException e) {
						System.out.println(dataSetName + " is not a valid data set enum name.");
					}
				}
			}
		}

		return dataSets;
	}
}