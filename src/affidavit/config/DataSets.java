package affidavit.config;

public enum DataSets {
	VANILLA("S_T", "T_T", "data\\db.db"), FULLY_EQUAL("A_T7NZTR", "B_T7NZTR",
			"C:\\Users\\mfink\\git\\TT\\TT\\data\\S4_Full\\SQLite_DB.db"), SMALL_SNP("A_T7THTR", "B_T7THTR",
					"C:\\Users\\mfink\\git\\TT\\TT\\data\\S4_Full\\SQLite_DB.db"), BIG("A_T512W", "B_T512W",
							"C:\\Users\\mfink\\git\\TT\\TT\\data\\S4_Full\\SQLite_DB.db"), MINI("S_Mini", "T_Mini",
									"data/minimal/minimal.db"), FD30("S_FD30", "S_FD30", "data/fd/fd.db"), SCHOOL(
											"S_School_results", "S_hepatitis",
											"data/fd/fd.db"), UNIPROT200("S_uniprot", "S_uniprot", "data/fd/fd.db"),
	// iris
	IRIS("S_iris", "S_iris", "data/fd/fd.db"),
	// balance-scale
	BALANCE("S_balance", "S_balance", "data/fd/fd.db"),
	// chess
	CHESS("S_chess", "S_chess", "data/fd/fd.db"),
	// abalone
	ABALONE("S_abalone", "S_abalone", "data/fd/fd.db"),
	// nursery
	NURSERY("S_nursery", "S_nursery", "data/fd/fd.db"),
	// breast-cancer
	BREAST("S_breast", "S_breast", "data/fd/fd.db"),
	// bridges
	BRIDGES("S_bridges", "S_bridges", "data/fd/fd.db"),
	// echocardiogram
	ECHO("S_echo", "S_echo", "data/fd/fd.db"),
	// adult
	ADULT("S_adult", "S_adult", "data/fd/fd.db"),
	// letter
	LETTER("S_letter", "S_letter", "data/fd/fd.db"),
	// ncvoter
	NCVOTER19("S_ncvoter_19c", "S_ncvoter_19c", "data/fd/fd.db"),
	// not used in the original paper !
	NCVOTER22("S_ncvoter_22c", "S_ncvoter_22c", "data/fd/fd.db"), // created in database, test generation and evaluation
																	// not yet done, stopped after several minutes
	// hepatitis
	HEPATITIS("S_hepatitis", "S_hepatitis", "data/fd/fd.db"),
	// horse
	HORSE("S_horse", "S_horse", "data/fd/fd.db"),
	// fd-reduced-30, the only synthetic dataset
	FDREDUCED("S_fd_reduced_30", "S_fd_reduced_30", "data/fd/fd.db"), // created in database, test generation and
																		// evaluation not yet done, stopped after
																		// several minutes
	// plista
	PLISTA("S_plista", "S_plista", "data/fd/fd.db"),
	// flight
	FLIGHT1K("S_flight_1k", "S_fd_flight_1k", "data/fd/fd.db"),
	// not used in original publication!
	FLIGHT500K("S_flight_500k", "S_flight_500k", "data/fd/fd.db"), // created in database, test generation and
																	// evaluation not yet done, stopped after several
																	// minutes
	// uniprot
	UNIPROT("S_uniprot", "S_uniprot", "data/fd/fd.db"); // created in database and created test, evaluation on test did
														// not finish but might ...;

	public String	SOURCE_TABLE_NAME;
	public String	TARGET_TABLE_NAME;
	public String	PATH_TO_DB;

	DataSets(String sourceTableName, String targetTableName, String pathToDB) {
		SOURCE_TABLE_NAME = sourceTableName;
		TARGET_TABLE_NAME = targetTableName;
		PATH_TO_DB = pathToDB;
	}
}