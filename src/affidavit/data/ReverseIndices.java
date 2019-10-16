package affidavit.data;

import java.util.HashMap;
import java.util.Map;

public class ReverseIndices {
	public static ReverseIndices		source;
	public static ReverseIndices		target;

	private Map<Integer, ReverseIndex>	indices;

	public ReverseIndices(int columnCount) {
		indices = new HashMap<>();

		for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
			indices.put(columnIndex, new ReverseIndex());
		}
	}

	public ReverseIndex column(int columnIndex) {
		return indices.get(columnIndex);
	}

}
