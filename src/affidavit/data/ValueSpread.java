package affidavit.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import affidavit.transformations.OperationalTransformation;
import affidavit.util.Random;

public class ValueSpread implements Comparable<ValueSpread> {
	Map<String, Spread>			possibleTargetValues	= new HashMap<>();
	Map<String, Set<String[]>>	affectedSourceRecords	= new HashMap<>();
	Set<String[]>				allSourceRecords		= new HashSet<>();
	private Double				entropy;

	public void addTargetOccurence(String sourceValue, String targetValue, RowPair rowPair) {
		if (!this.possibleTargetValues.containsKey(sourceValue)) {
			possibleTargetValues.put(sourceValue, new Spread());
		}

		possibleTargetValues.get(sourceValue).addTarget(targetValue, rowPair);

		if (!this.affectedSourceRecords.containsKey(sourceValue)) {
			affectedSourceRecords.put(sourceValue, new HashSet<>());
		}

		affectedSourceRecords.get(sourceValue).add(rowPair.sourceRow);
		allSourceRecords.add(rowPair.sourceRow);
	}

	public long evaluateTransformation(OperationalTransformation t) {
		AffectedRecords covered = new AffectedRecords();

		for (String sourceValue : possibleTargetValues.keySet()) {
			Spread targetValues = possibleTargetValues.get(sourceValue);
			String transformed = t.applyTo(sourceValue);

			if (targetValues.contains(transformed)) {
				covered.addAll(targetValues.getAffectedRecords(transformed));
			}
		}

		return covered.getMinimumAffected();
	}

	public Set<ValuePair> produceGreedyValuePairs() {
		Set<ValuePair> valuePairs = new HashSet<>();

		for (String sourceValue : possibleTargetValues.keySet()) {
			valuePairs.add(
					new ValuePair(sourceValue, possibleTargetValues.get(sourceValue).getMostPromisingTargetValue()));
		}

		return valuePairs;
	}

	public Set<Pair<ValuePair, AffectedRecords>> produceGreedyValuePairSample(int count) {
		Set<Pair<ValuePair, AffectedRecords>> valuePairs = new HashSet<>();

		possibleTargetValues.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(count / 2).forEach(e -> {
			String sourceValue = e.getKey();
			String targetValue = possibleTargetValues.get(sourceValue).getMostPromisingTargetValue();
			ValuePair vp = new ValuePair(sourceValue, targetValue);
			AffectedRecords affected = new AffectedRecords();
			affected.sourceRecords = affectedSourceRecords.get(sourceValue);
			affected.targetRecords = possibleTargetValues.get(sourceValue)
					.getAffectedRecords(targetValue).targetRecords;

			valuePairs.add(Pair.of(vp, affected));
		});

		affectedSourceRecords.entrySet().stream()
				.sorted((e1, e2) -> -Integer.compare(e1.getValue().size(), e2.getValue().size())).limit(count / 2)
				.forEach(e -> {
					String sourceValue = e.getKey();
					String targetValue = possibleTargetValues.get(sourceValue).getMostPromisingTargetValue();
					ValuePair vp = new ValuePair(sourceValue, targetValue);
					AffectedRecords affected = new AffectedRecords();
					affected.sourceRecords = affectedSourceRecords.get(sourceValue);
					affected.targetRecords = possibleTargetValues.get(sourceValue)
							.getAffectedRecords(targetValue).targetRecords;

					valuePairs.add(Pair.of(vp, affected));
				});

		return valuePairs;
	}

	@Override
	public int compareTo(ValueSpread o) {
		return Double.compare(this.getEntropy(), o.getEntropy());
	}

	public double getEntropy() {
		if (entropy == null) {
			entropy = possibleTargetValues.keySet().parallelStream().mapToDouble(sourceValue -> {
				double factor = (double) affectedSourceRecords.get(sourceValue).size()
						/ affectedSourceRecords.get(sourceValue).size();
				return factor * possibleTargetValues.get(sourceValue).getEntropy();
			}).sum();
		}

		return entropy;
	}

	private class Spread implements Comparable<Spread> {
		private Map<String, AffectedRecords>	targetValues	= new HashMap<>();
		private Double							entropy;

		public void addTarget(String targetValue, RowPair rowPair) {
			if (!targetValues.containsKey(targetValue)) {
				targetValues.put(targetValue, new AffectedRecords());
			}

			targetValues.get(targetValue).add(rowPair);
		}

		public boolean contains(String targetValue) {
			return targetValues.containsKey(targetValue);
		}

		public AffectedRecords getAffectedRecords(String targetValue) {
			return targetValues.get(targetValue);
		}

		public String getMostPromisingTargetValue() {
			if (targetValues.size() == 1) {
				return targetValues.keySet().iterator().next();
			}

			List<Entry<String, AffectedRecords>> sortedTargets = targetValues.entrySet().stream()
					.sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());

			if (sortedTargets.get(0).getValue().compareTo(sortedTargets.get(1).getValue()) == 0) {
				List<Entry<String, AffectedRecords>> bestTargets = sortedTargets.stream()
						.filter(t -> t.getValue().compareTo(sortedTargets.get(0).getValue()) == 0)
						.collect(Collectors.toList());

				return bestTargets.get(Random.instance.nextInt(bestTargets.size())).getKey();
			} else {
				return sortedTargets.get(0).getKey();
			}
		}

		public double getEntropy() {
			if (entropy == null) {
				entropy = 0.0;
				int totalAffected = 0;

				for (String targetValue : targetValues.keySet()) {
					totalAffected += targetValues.get(targetValue).getMinimumAffected();
				}

				for (String targetValue : targetValues.keySet()) {
					double p = 1.0 * targetValues.get(targetValue).getMinimumAffected() / totalAffected;
					entropy -= p * Math.log(p) / Math.log(2);
				}
			}

			return entropy;
		}

		@Override
		public int compareTo(Spread o) {
			return Double.compare(this.getEntropy(), o.getEntropy());
		}
	}

	public AffectedRecords getAffectedRecords(ValuePair vp) {
		return possibleTargetValues.get(vp.sourceValue).getAffectedRecords(vp.targetValue);
	}
}