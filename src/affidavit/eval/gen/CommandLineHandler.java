package affidavit.eval.gen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import affidavit.config.Config;
import affidavit.config.DataSets;
import affidavit.eval.TestcaseSetting;

public class CommandLineHandler {
	public static Options getOptions() {
		Options options = new Options();

		options.addOption("db", true, "Database with data sets to transform");
		options.addOption("t", true, "Target parent folder where to put the test cases");
		options.addOption("f", true, "File with data set names to create test cases for");
		options.addOption("n", true, "Number of test cases to create per data set");
		options.addOption("c", true, "File with names of Evaluation Settings");
		options.addOption("tn", false, "Translate Target Noise");
		options.addOption("k", true, "Filter all attributes with distinct value fraction bigger k");

		return options;
	}

	public static void handle(CommandLine cmd) throws IOException, SQLException {
		String dbPath = cmd.getOptionValue("db");
		String outputPath = cmd.getOptionValue("t");
		String nameFile = cmd.getOptionValue("f");
		String nString = cmd.getOptionValue("n");

		Collection<TestcaseSetting> settings = readTestcaseSettings(cmd.getOptionValue("c"));

		Collection<DataSets> dataSets = readDataSets(nameFile);
		int n = Integer.valueOf(nString);
		boolean translateTargetNoise = cmd.hasOption("tn");

		double k = 2;
		if (cmd.hasOption("k")) {
			k = Double.valueOf(cmd.getOptionValue("k"));
		}

		TestcaseGenerator gen = new TestcaseGenerator(outputPath, n);
		Config.PRINT_MAPS = true;
		gen.generateSuite(dbPath, dataSets, settings, translateTargetNoise, k);
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
						e.printStackTrace();
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