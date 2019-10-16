package affidavit.data.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Stores source and target values with rows containing these values in a specific column.
 * 
 * @author mfink
 *
 */
public class Partioning {
	private int									totalSourcesCount	= 0;
	private int									totalTargetsCount	= 0;
	private Map<String, Collection<String[]>>	sourceMap			= new HashMap<>();
	private Map<String, Collection<String[]>>	targetMap			= new HashMap<>();

	public Map<String, Collection<String[]>> getSourceMap() {
		return sourceMap;
	}

	public Map<String, Collection<String[]>> getTargetMap() {
		return targetMap;
	}

	public int getTotalSourcesCount() {
		return this.totalSourcesCount;
	}

	public int getTotalTargetsCount() {
		return this.totalTargetsCount;
	}

	public int getDifferentSourcesCount() {
		return sourceMap.keySet().size();
	}

	public int getDifferentTargetsCount() {
		return targetMap.keySet().size();
	}

	public void addSourceRow(String sourceValue, String[] row) {
		insert(sourceMap, sourceValue, row);
		totalSourcesCount++;
	}

	public void addTargetRow(String targetValue, String[] row) {
		insert(targetMap, targetValue, row);
		totalTargetsCount++;
	}

	private void insert(Map<String, Collection<String[]>> map, String value, String[] row) {
		if (!map.containsKey(value)) {
			map.put(value, new LinkedList<>());
		}

		map.get(value).add(row);
	}

	public boolean isFunctional() {
		return sourceMap.size() == 1 && targetMap.size() == 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SOURCE ROWS:\n");

		sourceMap.keySet().stream().forEach(string -> {
			sb.append("\"").append(string).append("\" {\n");
			sourceMap.get(string).stream().forEach(row -> sb.append(Arrays.toString(row)).append("\n"));
			sb.append("}\n\n");
		});
		sb.append("TARGET ROWS:\n");
		targetMap.keySet().stream().forEach(string -> {
			sb.append("\"").append(string).append("\" {\n");
			targetMap.get(string).stream().forEach(row -> sb.append(Arrays.toString(row)).append("\n"));
			sb.append("}\n\n");
		});

		return sb.toString();
	}
}
