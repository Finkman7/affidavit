package affidavit.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReverseIndex {
	private Map<String, Set<String[]>> index = new HashMap<>();

	public Set<String[]> getRows(String value) {
		if (index.containsKey(value)) {
			return index.get(value);
		} else {
			return new HashSet<>();
		}
	}

	public void addRow(String string, String[] row) {
		if (!index.containsKey(string)) {
			index.put(string, new HashSet<>());
		}

		index.get(string).add(row);
	}

	public Set<String> getAllValues() {
		return index.keySet();
	}

}
