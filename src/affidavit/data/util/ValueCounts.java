package affidavit.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import affidavit.data.ValuePair;
import affidavit.transformations.OperationalTransformation;
import affidavit.util.Random;

/**
 * Stores source and target values with corresponding counts.
 *
 * @author mfink
 *
 */
public class ValueCounts {
	private int							totalSourcesCount	= 0;
	private int							totalTargetsCount	= 0;
	private Map<String, MutableInteger>	sourceCounts		= new HashMap<>();
	private Map<String, MutableInteger>	targetCounts		= new HashMap<>();
	private int							maxSourceCount		= 0;

	public int getTotalSourcesCount() {
		return totalSourcesCount;
	}

	public int getTotalTargetsCount() {
		return totalTargetsCount;
	}

	public Map<String, MutableInteger> getSourceCounts() {
		return sourceCounts;
	}

	public Map<String, MutableInteger> getTargetCounts() {
		return targetCounts;
	}

	public synchronized void increaseSourceValueCount(String sourceValue) {
		if (!sourceCounts.containsKey(sourceValue)) {
			sourceCounts.put(sourceValue, new MutableInteger(1));
		} else {
			sourceCounts.get(sourceValue).increment();
		}

		totalSourcesCount++;
	}

	public synchronized void increaseTargetValueCount(String targetValue) {
		if (!targetCounts.containsKey(targetValue)) {
			targetCounts.put(targetValue, new MutableInteger(1));
		} else {
			targetCounts.get(targetValue).increment();
		}

		totalTargetsCount++;
	}

	public long getSquaredSize() {
		return sourceCounts.size() * (long) targetCounts.size();
	}

	/**
	 *
	 * @return true if there are less than 2 target values.
	 */
	public boolean isFunctional() {
		return targetCounts.size() < 2;
	}

	public long getMinimumDroppedSourceCount() {
		int highestTargetFrequency = targetCounts.isEmpty() ? 0
				: targetCounts.values().stream().mapToInt(mutableInt -> mutableInt.get()).max().getAsInt();

		return sourceCounts.values().stream().mapToInt(v -> v.get()).filter(count -> count > highestTargetFrequency)
				.map(count -> count - highestTargetFrequency).sum();
	}

	public Collection<ValuePair> getFunctionalValuePairs() throws Exception {
		if (sourceCounts.size() > 1 || !this.isFunctional()) {
			throw new Exception("ValueCounts is not functional. Can not return functional value pairs.");
		} else {
			String sourceValue = sourceCounts.keySet().iterator().next();
			String targetValue = targetCounts.keySet().iterator().next();
			int count = Math.min(totalSourcesCount, totalTargetsCount);
			Collection<ValuePair> valuePairs = new ArrayList<>(count);

			for (int i = 0; i < count; i++) {
				valuePairs.add(new ValuePair(sourceValue, targetValue));
			}

			return valuePairs;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SOURCES:\n");

		sb.append(sourceCounts.entrySet().stream().map(entry -> entry.getKey() + "(" + entry.getValue().get() + ")")
				.collect(Collectors.joining(", ")));

		sb.append("\nTARGETS:\n");
		sb.append(targetCounts.entrySet().stream().map(entry -> entry.getKey() + "(" + entry.getValue().get() + ")")
				.collect(Collectors.joining(", ")));

		return sb.toString();
	}

	public void addFuntionalBounded(ValueCounts other) {
		int maxTargets = other.totalSourcesCount == 0 ? 0
				: other.getSourceCounts().values().stream().mapToInt(mutI -> mutI.get()).min().getAsInt();
		int maxSources = other.totalTargetsCount == 0 ? 0
				: other.getTargetCounts().values().stream().mapToInt(mutI -> mutI.get()).min().getAsInt();

		this.totalSourcesCount += other.totalSourcesCount;
		this.totalTargetsCount += other.totalTargetsCount;

		for (String sourceValue : other.sourceCounts.keySet()) {
			MutableInteger otherSourceValueCount = other.sourceCounts.get(sourceValue);
			int adjusted = Math.min(otherSourceValueCount.get(), maxSources);
			this.totalSourcesCount += adjusted;

			if (!this.sourceCounts.containsKey(sourceValue)) {
				this.sourceCounts.put(sourceValue, new MutableInteger(adjusted));
			} else {
				this.sourceCounts.get(sourceValue).add(adjusted);
			}
		}

		for (String targetValue : other.targetCounts.keySet()) {
			MutableInteger otherTargetValueCount = other.targetCounts.get(targetValue);
			int adjusted = Math.min(otherTargetValueCount.get(), maxTargets);
			this.totalTargetsCount += adjusted;

			if (!this.targetCounts.containsKey(targetValue)) {
				this.targetCounts.put(targetValue, new MutableInteger(otherTargetValueCount));
			} else {
				this.targetCounts.get(targetValue).add(otherTargetValueCount);
			}
		}
	}

	public synchronized void add(ValueCounts other) {
		this.totalSourcesCount += other.totalSourcesCount;
		this.totalTargetsCount += other.totalTargetsCount;

		if (other.sourceCounts.keySet().size() > maxSourceCount) {
			this.maxSourceCount = other.sourceCounts.keySet().size();
		}

		for (String sourceValue : other.sourceCounts.keySet()) {
			MutableInteger otherSourceValueCount = other.sourceCounts.get(sourceValue);

			if (!this.sourceCounts.containsKey(sourceValue)) {
				this.sourceCounts.put(sourceValue, new MutableInteger(otherSourceValueCount));
			} else {
				this.sourceCounts.get(sourceValue).add(otherSourceValueCount);
			}
		}

		for (String targetValue : other.targetCounts.keySet()) {
			MutableInteger otherTargetValueCount = other.targetCounts.get(targetValue);

			if (!this.targetCounts.containsKey(targetValue)) {
				this.targetCounts.put(targetValue, new MutableInteger(otherTargetValueCount));
			} else {
				this.targetCounts.get(targetValue).add(otherTargetValueCount);
			}
		}
	}

	public List<String> getRandomTargetValues(int samplingSize) {
		return Random.sampleDistinctFromList(new ArrayList<>(this.targetCounts.keySet()), samplingSize);
	}

	public int getCoOccurence(String sourceValue, String targetValue) {
		if (sourceCounts.containsKey(sourceValue) && targetCounts.containsKey(targetValue)) {
			return Math.min(sourceCounts.get(sourceValue).get(), targetCounts.get(targetValue).get());
		}

		return 0;
	}

	public List<OperationalTransformation> getBest(Set<OperationalTransformation> transformationCandidates, int limit) {
		Map<OperationalTransformation, Integer> scores = new ConcurrentHashMap<>(transformationCandidates.size());
		Map<OperationalTransformation, Map<String, MutableInteger>> producedCounts = transformationCandidates.stream()
				.collect(Collectors.toConcurrentMap(t -> t, t -> new ConcurrentHashMap<>()));

		this.sourceCounts.keySet().parallelStream().forEach(sourceValue -> {
			transformationCandidates.stream().forEach(t -> {
				Map<String, MutableInteger> producedCountsAttribute = producedCounts.get(t);
				String transformed = t.applyTo(sourceValue);

				if (targetCounts.containsKey(transformed)) {
					if (!producedCountsAttribute.containsKey(transformed)) {
						producedCountsAttribute.put(transformed, new MutableInteger(sourceCounts.get(sourceValue)));
					} else {
						producedCountsAttribute.get(transformed).add(sourceCounts.get(sourceValue));
					}
				}
			});
		});

		for (OperationalTransformation t : transformationCandidates) {
			int score = getOverlap(producedCounts.get(t), targetCounts);
			scores.put(t, score);
		}

		return scores.entrySet().stream().sorted((e1, e2) -> {
			return -Long.compare(e1.getValue() - e1.getKey().getCosts(), e2.getValue() - e2.getKey().getCosts());
		}).limit(limit).map(e -> e.getKey()).collect(Collectors.toList());
	}

	public static int getOverlap(Map<String, MutableInteger> targetCounts, Map<String, MutableInteger> producedCounts) {
		int overlap = 0;

		Map<String, MutableInteger> smaller = producedCounts.size() < targetCounts.size() ? producedCounts
				: targetCounts;
		Map<String, MutableInteger> larger = (smaller == targetCounts) ? producedCounts : targetCounts;

		for (String value : smaller.keySet()) {
			if (larger.containsKey(value)) {
				overlap += Math.min(smaller.get(value).get(), larger.get(value).get());
			}
		}

		return overlap;
	}

	public int getMaximumSourceCount() {
		return maxSourceCount;
	}

}
