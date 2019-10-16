package affidavit.data;

import java.io.*;
import java.util.*;

public class Table implements Serializable {
	public String			name;
	public String[]			headers;
	public List<String[]>	rows;

	public Table(String name, String[] headers, List<String[]> rows) {
		this.name = name;
		this.headers = headers;
		this.rows = rows;
	}

	public int lineCount() {
		return this.rows.size();
	}

	public int columnCount() {
		return headers.length;
	}

	public String dataToString() {
		return TablePrinter.print(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(name).append(" (").append(lineCount()).append(") ")
				.append(Arrays.toString(headers));

		return sb.toString();
	}

	private static class TablePrinter {
		private static final int TABLE_HEADER_REPETITION = 10;

		public static String print(Table table) {
			StringBuilder sb = new StringBuilder();
			int count = -1;
			sb.append("$$ ------- " + table.name + ":");

			for (String[] line : table.rows) {
				if ((++count % TABLE_HEADER_REPETITION) == 0) {
					for (String header : table.headers) {
						sb.append(String.format("%10s\t", header));
					}
					sb.append("\n");
				}

				for (String date : line) {
					sb.append(String.format("%10s\t", date));
				}
				sb.append("\n");
			}

			sb.append("" + "------- " + table.name + " Data End $$\n");

			return sb.toString();
		}
	}
}
