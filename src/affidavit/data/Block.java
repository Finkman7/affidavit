package affidavit.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import affidavit.Affidavit;
import affidavit.config.Config;
import affidavit.data.util.MutableInteger;
import affidavit.data.util.ValueCounts;
import affidavit.transformations.OperationalTransformation;
import affidavit.util.Random;
import affidavit.util.Timer;

/**
 * Stores source and target tuples that could potentially be matched with each other given this join result.
 *
 * @author mfink
 *
 */
public class Block implements Serializable {
	public BlockIndex													index;
	public List<String[]>												sourceRows;
	public List<String[]>												targetRows;
	public Collection<RowPair>											rowSample;
	private Map<Integer, ValueCounts>									valueDistributions	= new HashMap<>();
	private Map<Integer, Map<OperationalTransformation, ValueCounts>>	transformedCounts	= new HashMap<>();

	public Block(BlockIndex index) {
		sourceRows = new ArrayList<>();
		targetRows = new ArrayList<>();
		this.index = index;
	}

	public void addSourceRow(String[] row) {
		sourceRows.add(row);
	}

	public void addSources(Collection<String[]> sourceLines) {
		sourceRows.addAll(sourceLines);
	}

	public void addTargetRow(String[] row) {
		targetRows.add(row);
	}

	public void addTargets(Collection<String[]> targetLines) {
		targetRows.addAll(targetLines);
	}

	public boolean hasNoSourceRows() {
		return sourceRows.isEmpty();
	}

	public boolean hasNoTargetRows() {
		return targetRows.isEmpty();
	}

	public boolean hasMoreTargetsThanSources() {
		return targetRows.size() > sourceRows.size();
	}

	public boolean hasMoreSourcesThanTarget() {
		return sourceRows.size() > targetRows.size();
	}

	/**
	 *
	 * @return true if it has both source and target rows.
	 */
	public boolean hasMatches() {
		return !hasNoSourceRows() && !hasNoTargetRows();
	}

	/**
	 *
	 * @return true if it has exactly one source and target row.
	 */
	public boolean isOneToOneBlock() {
		return sourceRows.size() == 1 && targetRows.size() == 1;
	};

	public Map<Integer, ValueCounts> getValueDistribution(Collection<Integer> attributes) {
		List<Integer> attributesWithoutDistribution = attributes.stream()
				.filter(attribute -> !valueDistributions.containsKey(attribute)).collect(Collectors.toList());
		if (!attributesWithoutDistribution.isEmpty()) {
			buildValueDistributions(attributesWithoutDistribution);
		}

		return valueDistributions.entrySet().stream().filter(e -> attributes.contains(e.getKey()))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}

	private void buildValueDistributions(List<Integer> attributes) {
		for (int attribute : attributes) {
			valueDistributions.put(attribute, new ValueCounts());
		}
		Stream<String[]> sourceRowStream;
		if (sourceRows.size() > Math.sqrt(Affidavit.ENVIRONMENT.smallerLineCount())) {
			sourceRowStream = sourceRows.parallelStream();
		} else {
			sourceRowStream = sourceRows.stream();
		}
		sourceRowStream.forEach(sourceLine -> {
			for (int attribute : attributes) {
				valueDistributions.get(attribute).increaseSourceValueCount(sourceLine[attribute]);
			}
		});

		Stream<String[]> targetRowStream;
		if (targetRows.size() > Math.sqrt(Affidavit.ENVIRONMENT.smallerLineCount())) {
			targetRowStream = targetRows.parallelStream();
		} else {
			targetRowStream = targetRows.stream();
		}
		targetRowStream.forEach(targetLine -> {
			for (int attribute : attributes) {
				valueDistributions.get(attribute).increaseTargetValueCount(targetLine[attribute]);
			}
		});
	}

	public ValueCounts getValueCounts(int columnIndex, int maxTargets) {
		ValueCounts valueCounts = new ValueCounts();

		for (String[] sourceLine : sourceRows) {
			valueCounts.increaseSourceValueCount(sourceLine[columnIndex]);
		}

		for (String[] targetLine : targetRows) {
			valueCounts.increaseTargetValueCount(targetLine[columnIndex]);
			if (valueCounts.getTargetCounts().size() > maxTargets) {
				break;
			}
		}

		return valueCounts;
	}

	public ValueCounts getValueCounts(int columnIndex, int maxSources, int maxTargets) throws Exception {
		ValueCounts valueCounts = new ValueCounts();

		for (String[] sourceLine : sourceRows) {
			valueCounts.increaseSourceValueCount(sourceLine[columnIndex]);
			if (valueCounts.getSourceCounts().size() > maxSources) {
				throw new Exception("Maximum source value count within join block exceeded.");
			}
		}

		for (String[] targetLine : targetRows) {
			valueCounts.increaseTargetValueCount(targetLine[columnIndex]);
			if (valueCounts.getSourceCounts().size() > maxTargets) {
				throw new Exception("Maximum target value count within join block exceeded.");
			}
		}

		return valueCounts;
	}

	public long getSquaredSize() {
		return sourceRows.size() * (long) targetRows.size();
	}

	public boolean columnIsFunctional(int index) {
		String seenTargetValue = null;

		for (String[] row : targetRows) {
			String currentTargetValue = row[index];

			if (seenTargetValue != null && !currentTargetValue.equals(seenTargetValue)) {
				return false;
			} else {
				seenTargetValue = currentTargetValue;
			}
		}

		return true;
	}

	public Collection<? extends RowPair> getRowSample() {
		if (rowSample == null) {
			int pairings = Math.min(sourceRows.size(), targetRows.size());
			rowSample = new ArrayList<>(pairings);
			ArrayList<String[]> shuffledSourceRecords = Random.sampleDistinctFromList(sourceRows, pairings);
			ArrayList<String[]> shuffledTargetRecords = Random.sampleDistinctFromList(targetRows, pairings);

			for (int j = 0; j < pairings; j++) {
				rowSample.add(new RowPair(shuffledSourceRecords.get(j), shuffledTargetRecords.get(j)));
			}
		}

		return rowSample;
	}

	public Collection<? extends RowPair> getSafeSamples() {
		if (rowSample == null) {
			rowSample = new LinkedList<>();

			if (targetRows.size() < 50 && sourceRows.size() < 50) {
				for (String[] sourceRow : sourceRows) {
					for (String[] targetRow : targetRows) {
						rowSample.add(new RowPair(sourceRow, targetRow));
					}

				}
			} else {
				int smaller = Math.min(sourceRows.size(), targetRows.size());
				int count = Math.floorDiv(Config.MAX_MATCHING_BLOCK_SIZE, smaller);

				for (int i = 0; i < count; i++) {
					List<String[]> sampledSourceRows = Random.sampleDistinctFromList(sourceRows, smaller);
					List<String[]> sampledTargetRows = Random.sampleDistinctFromList(targetRows, smaller);

					for (int j = 0; j < smaller; j++) {
						RowPair rp = new RowPair(sampledSourceRows.get(j), sampledTargetRows.get(j));

						rowSample.add(rp);
					}
				}
			}
		}

		return rowSample;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (!sourceRows.isEmpty()) {
			sb.append("Sources:\n");
			for (String[] sourceLine : sourceRows) {
				sb.append("\t").append(Arrays.toString(sourceLine)).append("\n");
			}
		}
		if (!targetRows.isEmpty()) {
			sb.append("Targets:\n");
			for (String[] targetLine : targetRows) {
				sb.append("\t").append(Arrays.toString(targetLine)).append("\n");
			}
		}

		return sb.toString();
	}

	public boolean isEmpty() {
		return hasNoSourceRows() && hasNoTargetRows();
	}

	public ValueCounts getTransformedCount(int attribute, OperationalTransformation t) {
		if (!transformedCounts.containsKey(attribute)) {
			transformedCounts.put(attribute, new HashMap<>());
		}

		if (!transformedCounts.get(attribute).containsKey(t)) {
			ValueCounts transformedCount = new ValueCounts();

			for (String[] sourceRow : sourceRows) {
				String transformed = t.applyTo(sourceRow[attribute]);
				transformedCount.increaseTargetValueCount(transformed);
			}

			transformedCounts.get(attribute).put(t, transformedCount);
		}

		return transformedCounts.get(attribute).get(t);
	}

	public Map<OperationalTransformation, ValueCounts> getTransformedCounts(int attribute, Collection<OperationalTransformation> transformations) {
		Timer.start("toll");
		if (!transformedCounts.containsKey(attribute)) {
			transformedCounts.put(attribute, new HashMap<>());
		}

		for (OperationalTransformation t : transformations) {
			if (!transformedCounts.get(attribute).containsKey(t)) {
				transformedCounts.get(attribute).put(t, new ValueCounts());
			}
		}

		Stream<String[]> sourceRowStream;
		if (sourceRows.size() >= Math.sqrt(Affidavit.ENVIRONMENT.sourceLineCount())) {
			sourceRowStream = sourceRows.parallelStream();
		} else {
			sourceRowStream = sourceRows.stream();
		}

		System.out.println("toll1: " + Timer.getMilliSecondsWithUnit("toll"));
		sourceRowStream.forEach(sourceRow -> {
			for (OperationalTransformation t : transformations) {
				String transformed = t.applyTo(sourceRow[attribute]);
				transformedCounts.get(attribute).get(t).increaseTargetValueCount(transformed);
			}
		});
		System.out.println("toll2: " + Timer.getMilliSecondsWithUnit("toll"));

		return transformedCounts.get(attribute).entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}

	public Map<Integer, Map<OperationalTransformation, ValueCounts>> getTransformedCounts(Map<Integer, Map<OperationalTransformation, MutableInteger>> transformationCandidates) {
		for (int attribute : transformationCandidates.keySet()) {
			if (!transformedCounts.containsKey(attribute)) {
				transformedCounts.put(attribute, new HashMap<>());
			}

			for (OperationalTransformation t : transformationCandidates.get(attribute).keySet()) {
				if (!transformedCounts.get(attribute).containsKey(t)) {
					transformedCounts.get(attribute).put(t, new ValueCounts());
				}
			}
		}

		Stream<String[]> sourceRowStream;
		if (sourceRows.size() >= Math.sqrt(Affidavit.ENVIRONMENT.sourceLineCount())) {
			sourceRowStream = sourceRows.parallelStream();
		} else {
			sourceRowStream = sourceRows.stream();
		}

		sourceRowStream.forEach(sourceRow -> {
			for (int attribute : transformationCandidates.keySet()) {
				for (OperationalTransformation t : transformationCandidates.get(attribute).keySet()) {
					String transformed = t.applyTo(sourceRow[attribute]);
					transformedCounts.get(attribute).get(t).increaseTargetValueCount(transformed);
				}
			}
		});

		return transformedCounts.entrySet().stream().filter(e -> transformationCandidates.containsKey(e.getKey()))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}
}