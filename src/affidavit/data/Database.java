package affidavit.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import affidavit.config.Config;

public class Database {
	public static Table readTable(String sourceTableName, Connection conn) throws SQLException {
		String sql = "SELECT * FROM " + sourceTableName;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		String[] headers = Database.getColumnNames(sourceTableName, conn).toArray(new String[1]);
		ArrayList<String[]> rows = new ArrayList<String[]>();
		while (rs.next()) {
			String[] row = new String[headers.length];

			for (int i = 0; i < headers.length; i++) {
				row[i] = rs.getString(i + 1);
			}

			rows.add(row);
		}

		return new Table(sourceTableName, headers, rows);
	}

	public static List<String> getColumnNames(String tableName, Connection conn) {
		List<String> sourceColumnNames = new ArrayList<String>();

		Statement st;
		try {
			st = conn.createStatement();
			String sql = "PRAGMA table_info('" + tableName + "');";
			ResultSet rs = st.executeQuery(sql);

			while (rs.next()) {
				sourceColumnNames.add(rs.getString(2));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sourceColumnNames;
	}

	public static Set<String> collectTableNames(String prefix, boolean filtered, Connection conn) {
		Set<String> tableNames = new TreeSet<String>();

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE '" + prefix
					+ "\\_%' ESCAPE '\\';");

			while (rs.next()) {
				String tableName = rs.getString(1).substring(2);

				if (!filtered || !Config.tableFiltered(tableName)) {
					tableNames.add(rs.getString(1).substring(2));
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return tableNames;
	}

	public static Collection<Integer> identifyEmptyColumns(String sourceTableName, Connection conn)
			throws SQLException {
		int columnCount = getColumnNames(sourceTableName, conn).size();
		Set<Integer> emptyColumns = IntStream.range(0, columnCount).boxed().collect(Collectors.toSet());

		String sql = "SELECT * FROM " + sourceTableName;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		Set<Integer> toRemove = new HashSet<Integer>();
		while (rs.next()) {
			for (int columnIndex : emptyColumns) {
				if (!rs.getString(columnIndex + 1).isEmpty()) {
					toRemove.add(columnIndex);
				}
			}
			emptyColumns.removeAll(toRemove);
			toRemove.clear();
		}

		return emptyColumns;
	}

	public static int countLines(String sourceTableName, Connection conn) throws SQLException {
		String sql = "SELECT COUNT(*) FROM " + sourceTableName;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		rs.next();
		return rs.getInt(1);
	}
}
