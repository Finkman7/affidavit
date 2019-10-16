package affidavit.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

public class Config {
	// Parameters
	public static InitializationStrategy	INITIALIZATION_STRATEGY			= InitializationStrategy.SINGLE_IDs;
	public static double					ALPHA							= 0.5;
	public static double					NOISE							= 0.9;
	public static double					CONFIDENCE						= 0.95;
	public static int						MAX_MATCHING_BLOCK_SIZE			= 100000;
	public static int						BRANCHING_FACTOR				= 2;
	public static int						QUEUE_WIDTH						= 3;

	// Refinements
	public static int						MIN_REFINEMENT_GAIN				= 1;
	public static int						MAX_ATTRIBUTES_PER_REFINEMENT	= 1;
	public static final boolean				ALLOW_REFINMENTS_IN_ENDSTATE	= true;

	// Misc
	public static boolean					VERBOSE_MODE					= true;
	public static boolean					PRINT_MAPS						= true;

	public static char						CSV_SEPARATOR_CHAR				= ',';

	// Paths
	public static final String				TMP_DB_PATH						= "csv.db";
	public static final String				TMP_SOURCE_NAME					= "S_CSV";
	public static final String				TMP_TARGET_NAME					= "T_CSV";

	// Filtering of Tables
	public final static boolean				TABLE_WHITE_LISTING				= true;
	public final static String[]			TABLE_WHITE_LIST				= { "uniprot" };
	public final static String[]			WHITE_LIST_FILES				= {};
	public final static boolean				TABLE_BLACK_LISTING				= false;
	public final static String[]			TABLE_BLACK_LIST				= { "Table" };
	public final static String[]			BLACK_LIST_FILES				= {};
	public final static FilenameFilter		fileNameFilter					= fileNameFilter();
	public static final int					MAX_EVALUATIONS					= 100;

	public static Collection<String>		ALLOWED_TABLES;
	public static Collection<String>		BLOCKED_TABLES;

	private static FilenameFilter fileNameFilter() {
		return new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (!name.toLowerCase().endsWith(".csv")) {
					return false;
				}

				String tableName = name.substring(0, name.length() - 11);

				if (tableName.startsWith("-") || tableName.contains("#")) {
					return false;
				}

				if (Config.tableFiltered(tableName)) {
					return false;
				} else {
					return true;
				}
			}
		};
	}

	public static boolean tableFiltered(String tableName) {
		if (Config.TABLE_BLACK_LISTING) {
			if (Config.BLOCKED_TABLES == null) {
				Config.initBlockedTables();
			}

			if (Config.BLOCKED_TABLES.contains(tableName)) {
				return true;
			}
		}

		if (Config.TABLE_WHITE_LISTING) {
			if (Config.ALLOWED_TABLES == null) {
				Config.initAllowedTables();
			}

			if (!Config.ALLOWED_TABLES.contains(tableName)) {
				return true;
			}
		}

		return false;
	}

	private static void initAllowedTables() {
		Config.ALLOWED_TABLES = new TreeSet<>();
		Config.ALLOWED_TABLES.addAll(Arrays.asList(Config.TABLE_WHITE_LIST));

		for (String whiteListfileName : WHITE_LIST_FILES) {
			try {
				Config.ALLOWED_TABLES.addAll(extractTableNames(whiteListfileName));
			} catch (IOException e) {
				System.err.println("White List File " + whiteListfileName + " could not be opened.");
			}
		}
	}

	private static void initBlockedTables() {
		Config.BLOCKED_TABLES = new TreeSet<>();
		Config.BLOCKED_TABLES.addAll(Arrays.asList(Config.TABLE_BLACK_LIST));

		for (String blackListfileName : BLACK_LIST_FILES) {
			try {
				Config.BLOCKED_TABLES.addAll(extractTableNames(blackListfileName));
			} catch (IOException e) {
				System.err.println("Black List File " + blackListfileName + " could not be opened.");
			}
		}
	}

	private static Collection<? extends String> extractTableNames(String whiteListfileName)
			throws FileNotFoundException, IOException {
		Collection<String> tableNames = new HashSet<>();

		try (BufferedReader in = new BufferedReader(new FileReader(new File(whiteListfileName)))) {
			String line;

			while ((line = in.readLine()) != null) {
				tableNames.add(line);
			}
		}

		return tableNames;
	}

	public static String description() {
		return INITIALIZATION_STRATEGY + "_" + MAX_ATTRIBUTES_PER_REFINEMENT + "-att-refinements";
	}
}
