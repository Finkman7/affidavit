package affidavit.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import affidavit.util.L;

public class DBConnectionManager {
	private static Connection conn = null;

	public static Connection connect(String pathToDB) throws SQLException {
		if (conn == null) {
			String url = "jdbc:sqlite:" + pathToDB;
			conn = DriverManager.getConnection(url);
			conn.setAutoCommit(false);

			L.logln("Connection to SQLite DB at " + pathToDB + " has been established.");
		}

		return conn;
	}

	public static Connection connectInMemory(String pathToDB) throws SQLException {
		if (conn == null) {
			File dbFile = new File(pathToDB);
			if (!dbFile.exists()) {
				throw new SQLException("Database file does not exist: " + dbFile.getAbsolutePath());
			}

			conn = DriverManager.getConnection("jdbc:sqlite:");
			Statement stat = conn.createStatement();
			stat.executeUpdate("restore from '" + dbFile.getAbsolutePath() + "'");
			stat.close();
			conn.setAutoCommit(false);

			L.logln("In-Memory copy of " + pathToDB + " has been loaded.");
		}

		return conn;
	}

	public static void disconnect(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
				DBConnectionManager.conn = null;
			}
		} catch (SQLException ex) {
			L.logln(ex.getMessage());
		}
	}
}
