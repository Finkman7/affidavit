package affidavit.eval.gen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import affidavit.Affidavit;
import affidavit.config.DataSets;
import affidavit.data.Database;
import affidavit.data.Table;
import affidavit.data.ValuePair;
import affidavit.db.DBConnectionManager;
import affidavit.eval.Testcase;
import affidavit.eval.TestcaseSetting;
import affidavit.search.Blocker;
import affidavit.search.StateEvaluator;
import affidavit.search.state.State;
import affidavit.transformations.IDTransformation;
import affidavit.transformations.MapTransformation;
import affidavit.transformations.OperationalTransformation;
import affidavit.transformations.TransformationFactory;
import affidavit.transformations.UnsuitableTransformationException;
import affidavit.util.L;
import affidavit.util.Random;

public class TestcaseGenerator {
	private static final int				appendedRandomIDs		= 1;
	// Number of test cases per dataset
	private int								N						= 10;
	private final String					targetPath;
	private double							sourceNoiseFraction;
	private double							targetNoiseFraction;
	private double							transformationFraction;
	private Integer[]						sourceIDs				= IntStream.range(0, appendedRandomIDs).boxed()
			.collect(Collectors.toList()).toArray(new Integer[appendedRandomIDs]);
	private Integer[]						targetIDs				= IntStream.range(0, appendedRandomIDs).boxed()
			.collect(Collectors.toList()).toArray(new Integer[appendedRandomIDs]);
	private Map<Integer, Set<ValuePair>>	appendedIDValuePairs	= IntStream.range(0, appendedRandomIDs).boxed()
			.collect(Collectors.toMap(i -> i, i -> new HashSet<>()));

	public static void main(String[] args) throws SQLException, ParseException, IOException {
		if (args.length > 0) {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(CommandLineHandler.getOptions(), args);
			CommandLineHandler.handle(cmd);
		}
	}

	public TestcaseGenerator(String targetPath, int N) {
		this.targetPath = targetPath;
		this.N = N;
	}

	public void generateSuite(String dbPath, Collection<DataSets> dataSets, Collection<TestcaseSetting> settings, boolean translateTargetNoise, double maxDistinctValueFraction)
			throws SQLException {
		L.logln("Connecting to Database...");
		long time = System.nanoTime();
		Connection conn = DBConnectionManager.connect(dbPath);
		L.logln("Connecting took " + (0.000001 * (System.nanoTime() - time)) + " ms");

		for (DataSets dataSet : dataSets) {
			System.out.println("~~~~~~~~~~~~~~~ Creating Test Cases for " + dataSet);

			System.out.println("Reading Table " + dataSet.SOURCE_TABLE_NAME + "...");
			Table sourceTable = Database.readTable(dataSet.SOURCE_TABLE_NAME, conn);
			if (appendedRandomIDs > 0) {
				System.out.println("Adding " + appendedRandomIDs + " ID columns at front.");
				sourceTable = enhanceWithRandomIDs(sourceTable);
			}

			Collection<Integer> ignoredSourceColumns = Database.identifyEmptyColumns(dataSet.SOURCE_TABLE_NAME, conn)
					.stream().map(attribute -> attribute + appendedRandomIDs).collect(Collectors.toList());
			ignoredSourceColumns.addAll(filterIDAttributes(sourceTable, maxDistinctValueFraction));

			Collection<Integer> columnsToAssign = IntStream.rangeClosed(0, sourceTable.columnCount() - 1)
					.filter(c -> !ignoredSourceColumns.contains(c)).mapToObj(Integer::valueOf)
					.collect(Collectors.toList());

			for (TestcaseSetting setting : settings) {
				System.out.println("-------- Setting " + setting);

				this.sourceNoiseFraction = setting.noiseFraction;
				this.targetNoiseFraction = setting.noiseFraction;
				this.transformationFraction = setting.transformationFraction;

				for (int i = 1; i <= N; i++) {
					System.out.println("--------------- " + i + "/" + N + ":");

					String testCaseName = dataSet.toString() + "_" + setting.toString() + "_"
							+ String.format("%03d", i);
					Collections.shuffle(sourceTable.rows);
					Testcase testCase = generateFrom(sourceTable, columnsToAssign, ignoredSourceColumns, testCaseName,
							translateTargetNoise);

					L.logln("Writing " + testCaseName);
					testCase.endState.writeTo(new File(
							targetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
							testCaseName + "_Solution.txt"));
					testCase.endState.setBlockingResult(null);
					testCase.writeTo(new File(
							targetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
							testCaseName + ".obj"));
					try {
						testCase.writeCoreTo(new File(
								targetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
								testCaseName + "_Source_Core" + ".csv"));
						testCase.writeCoreImageTo(new File(
								targetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
								testCaseName + "_Target_Core_Image" + ".csv"));
						testCase.writeSourceNoiseTo(new File(
								targetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
								testCaseName + "_Source_Noise" + ".csv"));
						testCase.writeTargetNoiseTo(new File(
								targetPath + File.separator + dataSet.toString() + File.separator + setting.toString(),
								testCaseName + "_Target_Noise" + ".csv"));
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		DBConnectionManager.disconnect(conn);
	}

	private Collection<? extends Integer> filterIDAttributes(Table sourceTable, double maxDistinctValueFraction) {
		Collection<Integer> keyAttributes = IntStream.range(appendedRandomIDs, sourceTable.headers.length).filter(a -> {
			long distinctValues = sourceTable.rows.stream().map(r -> r[a]).distinct().count();

			if (distinctValues >= maxDistinctValueFraction * sourceTable.rows.size()) {
				System.out.println(a + " is key attribute (" + distinctValues + ")");
				return true;
			}

			return false;
		}).boxed().collect(Collectors.toList());

		return keyAttributes;
	}

	private Table enhanceWithRandomIDs(Table sourceTable) {
		String[] headers = new String[sourceTable.headers.length + appendedRandomIDs];
		for (int i = 0; i < appendedRandomIDs; i++) {
			headers[i] = "Random" + i;
		}
		System.arraycopy(sourceTable.headers, 0, headers, appendedRandomIDs, sourceTable.headers.length);

		List<String[]> records = new ArrayList<>(sourceTable.rows.size());
		int count = 0;
		for (String[] record : sourceTable.rows) {
			String[] enhanced = new String[record.length + appendedRandomIDs];
			for (int i = 0; i < appendedRandomIDs; i++) {
				enhanced[i] = String.valueOf(count);
			}
			count++;
			System.arraycopy(record, 0, enhanced, appendedRandomIDs, record.length);
			records.add(enhanced);
		}

		return new Table(sourceTable.name, headers, records);
	}

	public Testcase generateFrom(Table table, Collection<Integer> columnsToAssign, Collection<Integer> ignoredSourceColumns, String name, boolean translateTargetNoise)
			throws SQLException {
		sourceIDs = IntStream.range(0, appendedRandomIDs).boxed().collect(Collectors.toList())
				.toArray(new Integer[appendedRandomIDs]);
		targetIDs = IntStream.range(0, appendedRandomIDs).boxed().collect(Collectors.toList())
				.toArray(new Integer[appendedRandomIDs]);
		appendedIDValuePairs = IntStream.range(0, appendedRandomIDs).boxed()
				.collect(Collectors.toMap(i -> i, i -> new HashSet<>()));

		int noiseSize = (int) (this.sourceNoiseFraction * table.lineCount());
		List<String[]> sourceNoise = new ArrayList<>(table.rows.subList(0, noiseSize));
		List<String[]> targetNoise = new ArrayList<>(table.rows.subList(noiseSize, 2 * noiseSize));
		List<String[]> sourceRows = new ArrayList<>(table.rows.subList(2 * noiseSize, table.rows.size()));
		Map<Integer, OperationalTransformation> transformations;
		do {
			transformations = sampleTransformations(sourceRows, targetNoise);
		} while (transformations.keySet().size() >= columnsToAssign.size() - appendedRandomIDs);
		System.out.println("Sampled Transformations");
		fillAppendedIDAttributes(sourceRows, sourceNoise);
		List<String[]> targetRows = translate(sourceRows, transformations, true);
		if (translateTargetNoise) {
			targetNoise = translate(targetNoise, transformations, false);
			targetRows.addAll(targetNoise);
		} else {
			targetRows.addAll(targetNoise);
		}

		for (int i = 0; i < appendedRandomIDs; i++) {
			try {
				transformations.put(i, new MapTransformation(appendedIDValuePairs.get(i)));
			} catch (UnsuitableTransformationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		sourceRows.addAll(sourceNoise);
		Table sourceTable = new Table(table.name, table.headers, sourceRows);
		Table targetTable = new Table(table.name, table.headers, targetRows);
		Affidavit a = new Affidavit(sourceTable, targetTable, columnsToAssign, ignoredSourceColumns);
		State endState = a.STATE_FACTORY.create(false);
		for (int attribute : columnsToAssign) {
			if (transformations.containsKey(attribute)) {
				endState = endState.extend(attribute, transformations.get(attribute), false);
			} else {
				endState = endState.extend(attribute, IDTransformation.INSTANCE, false);
			}
		}

		Blocker.buildBlocksFor(endState);
		endState.trimMapTransformations();
		StateEvaluator.evaluateState(endState, false);
		System.out.println("Created Alignment");
		return new Testcase(name, sourceTable, targetTable, transformations, columnsToAssign, ignoredSourceColumns,
				sourceNoise, targetNoise, endState);
	}

	private void fillAppendedIDAttributes(List<String[]> sourceRows, List<String[]> targetNoise) {
		for (int a = 0; a < appendedRandomIDs; a++) {
			for (String[] r : sourceRows) {
				r[a] = String.valueOf(sourceIDs[a]);
				sourceIDs[a]++;
			}
			for (String[] r : targetNoise) {
				r[a] = String.valueOf(sourceIDs[a]);
				sourceIDs[a]++;
			}
		}
	}

	private List<String[]> translate(List<String[]> sourceRows, Map<Integer, OperationalTransformation> transformations, boolean rememberTargetIDMapping) {
		List<String[]> transformedRows = new ArrayList<>(sourceRows.size());

		Collections.shuffle(sourceRows);

		for (String[] row : sourceRows) {
			String[] transformed = new String[row.length];

			for (int i = 0; i < row.length; i++) {
				if (transformations.containsKey(i)) {
					transformed[i] = transformations.get(i).applyTo(row[i]);
				} else {
					transformed[i] = row[i];
				}
			}

			for (int a = 0; a < appendedRandomIDs; a++) {
				transformed[a] = String.valueOf(targetIDs[a]);
				targetIDs[a]++;
				if (rememberTargetIDMapping) {
					appendedIDValuePairs.get(a).add(new ValuePair(row[a], transformed[a]));
				}
			}

			transformedRows.add(transformed);
		}

		return transformedRows;
	}

	private Map<Integer, OperationalTransformation> sampleTransformations(List<String[]> sourceRows, List<String[]> targetNoise) {
		Map<Integer, OperationalTransformation> t = new HashMap<>();

		for (int i = appendedRandomIDs; i < sourceRows.get(0).length; i++) {
			if (Random.instance.nextDouble() < transformationFraction) {
				final int j = i;
				List<String> columnValues = sourceRows.stream().map(row -> row[j]).filter(v -> !v.isEmpty())
						.collect(Collectors.toList());
				columnValues.addAll(
						targetNoise.stream().map(row -> row[j]).filter(v -> !v.isEmpty()).collect(Collectors.toList()));

				if (columnValues.size() < 0.05 * sourceRows.size() + targetNoise.size()) {
					continue;
				}

				t.put(i, TransformationFactory.createRandomTransformation(columnValues));
			}
		}

		return t;
	}

}
