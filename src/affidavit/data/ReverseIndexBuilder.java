package affidavit.data;

public class ReverseIndexBuilder {

	public static ReverseIndices build(Table sourceTable) {
		ReverseIndices indices = new ReverseIndices(sourceTable.headers.length);

		for (String[] row : sourceTable.rows) {
			for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
				indices.column(columnIndex).addRow(row[columnIndex], row);
			}
		}

		return indices;
	}

}
