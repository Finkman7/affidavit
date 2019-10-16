package affidavit.main;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import affidavit.Affidavit;
import affidavit.config.CommandLineHandler;
import affidavit.config.Config;
import affidavit.config.DataSets;
import affidavit.data.Database;
import affidavit.data.Table;
import affidavit.db.DBConnectionManager;
import affidavit.util.L;
import affidavit.util.Timer;

public class Main {
	private static final DataSets dataSet = DataSets.VANILLA;

	public static void main(String[] args) throws SQLException, ParseException {
		System.out.println("Parallelism Level: " + ForkJoinPool.getCommonPoolParallelism());
		if (args.length == 0) {
			start(dataSet.PATH_TO_DB, dataSet.SOURCE_TABLE_NAME, dataSet.TARGET_TABLE_NAME);
		} else {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(CommandLineHandler.get(), args);
			CommandLineHandler.handle(cmd);
		}
	}

	public static void start(String pathToDb, String sourceTableName, String targetTableName) throws SQLException {
		L.logln("Connecting to Database...");
		long time = System.nanoTime();
		Connection conn = DBConnectionManager.connect(pathToDb);
		L.logln("Connecting took " + (0.000001 * (System.nanoTime() - time)) + " ms");

		L.log("Reading Source Table... ");
		Timer.start("readSourceTable");
		Table sourceTable = Database.readTable(sourceTableName, conn);
		L.logln(" took " + Timer.getMilliSecondsWithUnit("readSourceTable"));

		L.log("Reading Target Table... ");
		Timer.start("readTargetTable");
		Table targetTable = Database.readTable(targetTableName, conn);
		L.logln(" took " + Timer.getMilliSecondsWithUnit("readTargetTable"));

		L.log("Determining columns to ignore... ");
		Timer.start("findColumnsToIgnore");
		Collection<Integer> ignoredSourceColumns = Database.identifyEmptyColumns(sourceTableName, conn);
		Collection<Integer> ignoredTargetColumns = Database.identifyEmptyColumns(targetTableName, conn);
		if (!ignoredTargetColumns.containsAll(ignoredSourceColumns)) {
			System.err.println("Unsolvable: empty source column that is not empty in target: "
					+ Arrays.toString(ignoredSourceColumns.toArray()) + " versus "
					+ Arrays.toString(ignoredTargetColumns.toArray()));
			System.exit(-1);
		}
		L.logln(" took " + Timer.getMilliSecondsWithUnit("findColumnsToIgnore") + ". Ignoring "
				+ Arrays.toString(ignoredSourceColumns.toArray()));
		DBConnectionManager.disconnect(conn);
		Collection<Integer> columnsToAssign = IntStream.rangeClosed(0, sourceTable.columnCount() - 1)
				.filter(c -> !ignoredSourceColumns.contains(c)).mapToObj(Integer::valueOf).collect(Collectors.toList());

		// Testcase testCase = TestcaseGenerator.with(0.3, 0.2,
		// 0.5).generateFrom(sourceTable, null, columnsToAssign,
		// ignoredSourceColumns);
		// Affidavit algorithm = new Affidavit(testCase);
		Affidavit algorithm = new Affidavit(sourceTable, targetTable, columnsToAssign, ignoredSourceColumns);
		L.log("///////////////////// AFFIDAVIT ///////////////////// ");
		System.out.println(Config.INITIALIZATION_STRATEGY + " on " + dataSet + " /////");
		Timer.start("affidavit");
		algorithm.solve();
		L.logln("Total runtime: " + Timer.getMilliSecondsWithUnit("affidavit"));
	}

}
