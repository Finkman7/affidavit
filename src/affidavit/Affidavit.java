package affidavit;

import java.sql.SQLException;
import java.util.Collection;

import affidavit.data.Table;
import affidavit.eval.Testcase;
import affidavit.search.Search;
import affidavit.search.state.State;
import affidavit.search.state.StateFactory;

public class Affidavit {
	public static Affidavit				ENVIRONMENT;
	public final StateFactory			STATE_FACTORY;
	public final Table					SOURCE_TABLE;
	public final Table					TARGET_TABLE;
	public final Collection<Integer>	IGNORED_COLUMNS;
	public final Collection<Integer>	COLUMNS_TO_ASSIGN;

	public Affidavit(Table sourceTable, Table targetTable, Collection<Integer> columnsToAssign,
			Collection<Integer> ignoredColumns) throws SQLException {
		Affidavit.ENVIRONMENT = this;
		SOURCE_TABLE = sourceTable;
		TARGET_TABLE = targetTable;
		COLUMNS_TO_ASSIGN = columnsToAssign;
		IGNORED_COLUMNS = ignoredColumns;
		STATE_FACTORY = new StateFactory(sourceTable, targetTable);
	}

	public Affidavit(Testcase testCase) {
		Affidavit.ENVIRONMENT = this;
		SOURCE_TABLE = testCase.sourceTable;
		TARGET_TABLE = testCase.targetTable;
		COLUMNS_TO_ASSIGN = testCase.getColumnsToAssign();
		IGNORED_COLUMNS = testCase.getColumnsToIgnore();
		STATE_FACTORY = new StateFactory(testCase.sourceTable, testCase.targetTable);
	}

	public State solve() {
		State endState = (new Search()).findEndState();

		// StateRefiner.refine(endState);
		// endState = PostProcessing.process(endState);
		// L.logln("\nPost Processing Result:\n" + endState.toVerboseString());

		return endState;
	}

	public int sourceLineCount() {
		return SOURCE_TABLE.lineCount();
	}

	public int targetLineCount() {
		return TARGET_TABLE.lineCount();
	}

	public int columnCount() {
		return SOURCE_TABLE.columnCount();
	}

	public int smallerLineCount() {
		return Math.min(sourceLineCount(), targetLineCount());
	}
}
