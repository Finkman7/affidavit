package affidavit.data;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import affidavit.data.util.MutableInteger;

/**
 * Stores the a specific value with its count and all values and their counts that occured together with this value in
 * the same join block on the opposite side.
 * 
 * @author mfink
 *
 */
public class ValueBucket {
	public String						value;
	public int							valueCount;
	public Map<String, MutableInteger>	correspondingValueCounts	= new HashMap<>();

	public ValueBucket(String value, int valueCount) {
		this.value = value;
		this.valueCount = valueCount;
	}

	public void incrementCorrespondingValueCount(String correspondingValue, MutableInteger count) {
		if (!correspondingValueCounts.containsKey(correspondingValue)) {
			correspondingValueCounts.put(correspondingValue, count.clone());
		} else {
			correspondingValueCounts.get(correspondingValue).add(count);
		}
	}

	public boolean isFunctional() {
		return correspondingValueCounts.size() == 1;
	}

	public boolean hasCorrespondences() {
		return !correspondingValueCounts.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(value + " (" + valueCount + ") -> " + correspondingValueCounts.entrySet().stream()
				.map(entry -> entry.getKey() + " (" + entry.getValue().get() + ")").collect(Collectors.joining(", ")));

		return sb.toString();
	}

	public ValuePair getFunctionValuePair() {
		return new ValuePair(value, correspondingValueCounts.keySet().iterator().next());
	}
}