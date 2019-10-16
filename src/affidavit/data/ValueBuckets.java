package affidavit.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import affidavit.data.util.MutableInteger;

/**
 * Resembles a map that stores the corresponding ValueBucket for different values.
 *
 * @author mfink
 *
 */
public class ValueBuckets {
	private Map<String, ValueBucket> valueBuckets = new HashMap<>();

	public void adjustCount(String value, MutableInteger count) {
		if (!valueBuckets.containsKey(value)) {
			valueBuckets.put(value, new ValueBucket(value, count.get()));
		} else {
			valueBuckets.get(value).valueCount += count.get();
		}
	}

	public List<ValueBucket> getFunctionalBuckets() {
		return valueBuckets.values().stream().filter(bucket -> bucket.isFunctional()).collect(Collectors.toList());
	}

	/**
	 * @return a Set<ValuePair> that includes all pairs such that the target value was the only one appearing together
	 *         with the source value in the same join blocks (potentially with multiple occurences though)
	 */
	public Set<ValuePair> getFunctionalValuePairs() {
		Set<ValuePair> valuePairs = new HashSet<>();

		for (ValueBucket bucket : valueBuckets.values()) {
			if (bucket.isFunctional()) {
				ValuePair pair = bucket.getFunctionValuePair();
				valuePairs.add(pair);
			}
		}

		return valuePairs;
	}

	/**
	 * @return a Set<ValuePair> that includes all pairs such that the target value was the most frequent value appearing
	 *         together with the source value in the same join blocks
	 */
	public Set<ValuePair> getGreedyValuePairs() {
		Set<ValuePair> valuePairs = new HashSet<>();

		valueBuckets.values().stream().filter(bucket -> bucket.hasCorrespondences()).forEach(bucket -> {
			if (bucket.isFunctional()) {
				valuePairs.add(bucket.getFunctionValuePair());
			} else {
				int highestCount = 0;
				String mostFrequentString = bucket.correspondingValueCounts.keySet().iterator().next();

				for (Entry<String, MutableInteger> entry : bucket.correspondingValueCounts.entrySet()) {
					// find most frequent corresponding value and if equal string is among the most frequent, choose
					// that one
					if (entry.getValue().get() > highestCount) {
						highestCount = entry.getValue().get();
						mostFrequentString = entry.getKey();
					}
				}

				valuePairs.add(new ValuePair(bucket.value, mostFrequentString));
			}
		});

		return valuePairs;
	}

	public boolean isFunctional(String targetValue) {
		return valueBuckets.containsKey(targetValue) && valueBuckets.get(targetValue).isFunctional();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(valueBuckets.entrySet().stream().map(entry -> entry.getValue().toString())
				.collect(Collectors.joining("\n")));

		return sb.toString();
	}

	public Collection<ValueBucket> getBuckets() {
		return valueBuckets.values();
	}
}