package affidavit.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockIndex implements Serializable {
	private Map<Integer, String> attributeValues = new HashMap<>();

	private BlockIndex(Map<Integer, String> attributeValues) {
		this.attributeValues = new HashMap<>(attributeValues);
	}

	public BlockIndex() {

	}

	public void setAttributeValue(int attributeIndex, String value) {
		this.attributeValues.put(attributeIndex, value);
	}

	public BlockIndex extend(int attributeIndex, String value) {
		BlockIndex extension = this.clone();
		extension.setAttributeValue(attributeIndex, value);
		return extension;
	}

	@Override
	public BlockIndex clone() {
		return new BlockIndex(attributeValues);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		for (Entry<Integer, String> e : attributeValues.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toList())) {
			result = prime * result + e.getKey().hashCode();
			result = prime * result + e.getValue().hashCode();
		}

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BlockIndex other = (BlockIndex) obj;
		if (this.attributeValues == null) {
			if (other.attributeValues != null) {
				return false;
			}
		} else {
			if (!(this.attributeValues.equals(other.attributeValues)
					&& (this.attributeValues.size() == other.attributeValues.size()))) {
				return false;
			}

			for (Integer key : this.attributeValues.keySet()) {
				if (!attributeValues.get(key).equals(other.attributeValues.get(key))) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return attributeValues.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.map(e -> e.getValue() + "[" + e.getKey() + "]").collect(Collectors.joining("|"));
	}

	public boolean spans(int columnIndex) {
		return attributeValues.containsKey(columnIndex);
	}

	public boolean contains(int columnIndex, String sourceValue) {
		return spans(columnIndex) && attributeValues.get(columnIndex).equals(sourceValue);
	}

	public Set<Integer> indices() {
		return attributeValues.keySet();
	}

	public String getValueAt(int attributeIndex) {
		return attributeValues.get(attributeIndex);
	}

}
