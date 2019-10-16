package affidavit.eval;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import affidavit.Affidavit;
import affidavit.config.Config;
import affidavit.config.DataSets;
import affidavit.data.Table;
import affidavit.search.Blocker;
import affidavit.search.StateEvaluator;
import affidavit.search.state.State;
import affidavit.transformations.Transformation;
import affidavit.util.L;
import affidavit.util.Timer;

public class Evaluator {
	private Testcase						testCase;
	private long							runtime;

	private static final DataSets			dataSet				= DataSets.CHESS;
	private static final TestcaseSetting	testCaseSetting		= new TestcaseSetting(0.5, 0.8);
	private static final AlgorithmSetting	algorithmSetting	= AlgorithmSetting.SINGLE_IDs;
	private static final String				targetPath			= "testCases";
	private static final int				runsPerTestCase		= 5;

	public Evaluator(Testcase testCase) {
		this.testCase = testCase;
	}

	public static void main(String[] args) throws ParseException, IOException, SQLException {
		if (args.length == 0) {
			// evaluateSuite(new File("test_cases/CHESS"));
		} else {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(CommandLineHandler.getOptions(), args);
			CommandLineHandler.handle(cmd);
		}
	}

	private static void evaluateSingleTestCase() {
		int number = 2;
		File testCaseFile = new File(targetPath + File.separator + dataSet.toString() + File.separator
				+ testCaseSetting.toString() + File.separator + dataSet.toString() + "_" + testCaseSetting.toString()
				+ "_" + String.format("%03d", number) + ".obj");
		Testcase testCase = null;
		try {
			testCase = Testcase.readFrom(testCaseFile);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		EvaluationResult result = evaluateTestCaseWithSetting(testCase, null, algorithmSetting);
		System.out.println(result);
	}

	public static void evaluateSuite(String datasetPath, Collection<DataSets> dataSets, Collection<TestcaseSetting> settings, Collection<AlgorithmSetting> algoSettings, int nStart, int nEnd) {
		for (DataSets dataSet : dataSets) {
			String dataSetName = dataSet.toString();
			System.out.println("~~~~~~~~~~~ Evaluating " + dataSetName);

			for (TestcaseSetting setting : settings) {
				System.out.println("--------- " + setting);

				Map<AlgorithmSetting, List<EvaluationResult>> caseResults = algoSettings.stream()
						.collect(Collectors.toMap(a -> a, a -> new ArrayList<>()));
				for (int i = nStart; i <= nEnd; i++) {
					System.out.println("---------------- " + i + "/" + nEnd);
					File testCaseFile = new File(
							datasetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
							dataSetName + "_" + setting.toString() + "_" + String.format("%03d", i) + ".obj");
					Testcase testCase = null;
					try {
						testCase = Testcase.readFrom(testCaseFile);
						Affidavit env = new Affidavit(testCase);
						Blocker.buildBlocksFor(testCase.endState, testCase.sourceTable.rows, testCase.targetTable.rows);
						StateEvaluator.evaluateState(testCase.endState, false);
					} catch (ClassNotFoundException | IOException e) {
						System.err.println("Could not read dataset at " + testCaseFile.getAbsolutePath());
						continue;
					}

					for (AlgorithmSetting algoSetting : algoSettings) {
						System.out.println("#################" + algoSetting);
						File resultFile = new File(testCaseFile.getParentFile(),
								algoSetting + "_" + FilenameUtils.removeExtension(testCaseFile.getName()) + ".tsv");
						EvaluationResult result = evaluateTestCaseWithSetting(testCase, resultFile, algoSetting);
						caseResults.get(algoSetting).add(result);
						try {
							result.writeTo(new File(
									datasetPath + File.separator + dataSet.toString() + File.separator
											+ setting.toString(),
									"Result_" + algoSetting + "_" + dataSet.toString() + "_" + setting.toString()
											+ ".tsv"),
									String.format("%03d", i));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				for (AlgorithmSetting algoSetting : algoSettings) {
					EvaluationResult avgOverCases = EvaluationResult.merged(caseResults.get(algoSetting));
					try {
						avgOverCases.writeTo(new File(
								datasetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
								"Result_" + algoSetting + "_" + dataSet.toString() + "_" + setting.toString() + ".tsv"),
								"avg");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void evaluateSuite(File dir) {
		System.out.println("Evaluating Suite at " + dir.getAbsolutePath());

		Arrays.stream(AlgorithmSetting.values()).forEach(setting -> {
			System.out.println("Evaluating setting " + setting);

			List<EvaluationResult> caseResults = new ArrayList<>();

			for (File f : dir.listFiles(file -> file.getName().endsWith(".obj"))) {
				Testcase testCase = null;
				try {
					testCase = Testcase.readFrom(f);
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
					continue;
				}

				System.out.println("Evaluating test case " + testCase.name);

				File resultFile = new File(dir + File.separator + "Results",
						setting + "_" + FilenameUtils.removeExtension(f.getName()) + ".tsv");
				EvaluationResult result = evaluateTestCaseWithSetting(testCase, resultFile, setting);
				caseResults.add(result);
			}

			EvaluationResult avgOverCases = EvaluationResult.merged(caseResults);
			try {
				avgOverCases.writeTo(new File(dir + File.separator + "Results", setting + "_AVERAGE.tsv"), "avg");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private static EvaluationResult evaluateTestCaseWithSetting(Testcase testCase, File resultFile, AlgorithmSetting setting) {
		setting.apply();

		Evaluator evaluator = new Evaluator(testCase);
		EvaluationResult result = evaluator.evaluate("-");
		return result;
	}

	public EvaluationResult evaluate(String runName) {
		Affidavit algorithm = new Affidavit(testCase);
		L.log("///////////////////// AFFIDAVIT EVALUATION ///////////////////// ");
		System.out.println("Run " + runName + " with " + Config.description() + " on " + testCase.name + " /////");
		Timer.start("affidavit");
		State endState = algorithm.solve();
		runtime = Timer.getMilliSeconds("affidavit");
		System.out.println("\nTotal Runtime: " + Timer.milliSecondsToString(runtime));

		EvaluationResult result = evaluateEndstate(endState);

		System.out.println("\nTest Case: ");
		System.out.println(testCase);

		System.out.println("\nResult: ");
		System.out.println(endState.toVerboseString());

		System.out.println("\n" + result);
		return result;
	}

	private EvaluationResult evaluateEndstate(State endState) {
		long scoreDelta = endState.getCosts() - testCase.endState.getCosts();
		double scoreDeltaPercent = 100 * ((double) scoreDelta) / testCase.endState.getCosts();

		long alignedDelta = endState.getMetrics().alignedCount() - testCase.endState.getMetrics().alignedCount();
		double alignedDeltaPercent = 100 * ((double) alignedDelta) / testCase.endState.getMetrics().alignedCount();

		double accuracy = 0;
		accuracy = calculateAccuracy(endState);

		testCase.endState.getBlockingResult().writeTo("data/refernce_after.txt");
		endState.getBlockingResult().writeTo("data/result_after.txt");

		State reference = testCase.endState;
		reference = reference.removeAssignment(0);
		// reference = reference.removeAssignment(1);
		Blocker.buildBlocksFor(reference);

		endState = endState.removeAssignment(0);
		// endState = endState.removeAssignment(1);
		Blocker.buildBlocksFor(endState);

		reference.getBlockingResult().writeTo("data/refernce_f_after.txt");
		endState.getBlockingResult().writeTo("data/result_f_after.txt");

		Map<String[], Collection<String[]>> targetFromSourceAlignmentResult = endState.getBlockingResult()
				.getTargetFromSourceAlignment();
		Map<String[], Collection<String[]>> targetFromSourceAlignmentReference = reference.getBlockingResult()
				.getTargetFromSourceAlignment();

		System.err
				.println(targetFromSourceAlignmentReference.size() + " vs. " + targetFromSourceAlignmentResult.size());
		System.err.println("Out of " + Affidavit.ENVIRONMENT.targetLineCount());

		long correct = targetFromSourceAlignmentResult.entrySet().stream().filter(e -> {
			return targetFromSourceAlignmentReference.containsKey(e.getKey());
		}).count();

		double precision = targetFromSourceAlignmentResult.size() > 0
				? 1.0 * correct / targetFromSourceAlignmentResult.size()
				: 0;
		double recall = targetFromSourceAlignmentReference.size() > 0
				? 1.0 * correct / targetFromSourceAlignmentReference.size()
				: 0;
		double f1 = precision > 0 && recall > 0 ? 2 * precision * recall / (precision + recall) : 0.0;

		return new EvaluationResult(testCase.name, Config.description(), accuracy, runtime, precision, recall, f1,
				scoreDelta, scoreDeltaPercent, alignedDelta, alignedDeltaPercent);
	}

	private double calculateAccuracy(State endState) {
		int correctCells = 0;
		int cellCount = 0;

		for (String[] coreRecord : testCase.sourceTable.rows) {
			if (testCase.sourceNoise.contains(coreRecord)) {
				continue;
			}

			String[] referenceRecord = new String[coreRecord.length];
			for (Entry<Integer, Transformation> e : testCase.endState.getBlockingCriteria().entrySet()) {
				referenceRecord[e.getKey()] = e.getValue().applyTo(coreRecord[e.getKey()]);
			}

			String[] resultRecord = new String[coreRecord.length];
			for (Entry<Integer, Transformation> e : endState.getBlockingCriteria().entrySet()) {
				resultRecord[e.getKey()] = e.getValue().applyTo(coreRecord[e.getKey()]);
			}

			for (int i = 1; i < coreRecord.length; i++) {
				if (testCase.columnsToIgnore.contains(i)) {
					continue;
				}

				if (referenceRecord[i].equals(resultRecord[i])) {
					correctCells++;
				}
				cellCount++;
			}
		}

		return ((double) correctCells) / cellCount;
	}

	private static Testcase scaleTestCase(Testcase original, double scaling) {
		if (scaling >= 1) {
			return original;
		}

		List<String[]> oldCore = original.sourceTable.rows.stream().filter(r -> !original.sourceNoise.contains(r))
				.collect(Collectors.toList());
		List<String[]> newCore = new ArrayList<>(oldCore.subList(0, (int) (original.sourceNoise.size() * scaling)));
		List<String[]> newCoreImage = newCore.stream().map(r -> {
			String[] transformed = new String[r.length];

			for (int k = 0; k < r.length; k++) {
				if (original.transformations.containsKey(k)) {
					transformed[k] = original.transformations.get(k).applyTo(r[k]);
				} else {
					transformed[k] = r[k];
				}
			}

			return transformed;
		}).collect(Collectors.toList());

		List<String[]> sourceNoise = new ArrayList<>(
				original.sourceNoise.subList(0, (int) (original.sourceNoise.size() * scaling)));
		List<String[]> targetNoise = new ArrayList<>(
				original.targetNoise.subList(0, (int) (original.sourceNoise.size() * scaling)));

		Table sourceTable = new Table(original.sourceTable.name, original.sourceTable.headers, newCore);
		Table targetTable = new Table(original.targetTable.name, original.targetTable.headers, newCoreImage);

		Testcase adjusted = new Testcase(original.name, sourceTable, targetTable, original.transformations,
				original.columnsToAssign, original.columnsToIgnore, sourceNoise, targetNoise, original.endState);
		Affidavit env = new Affidavit(adjusted);

		adjusted.endState = original.endState.clone();

		Blocker.buildBlocksFor(adjusted.endState, adjusted.sourceTable.rows, adjusted.targetTable.rows);
		adjusted.endState.trimMapTransformations();
		StateEvaluator.evaluateState(adjusted.endState, false);

		return adjusted;
	}
}
