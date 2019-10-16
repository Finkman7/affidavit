package affidavit.data;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import affidavit.Affidavit;
import affidavit.config.BinomDistUtility;
import affidavit.config.Config;
import affidavit.data.util.MutableInteger;
import affidavit.data.util.ValueCounts;
import affidavit.transformations.OperationalTransformation;
import affidavit.util.L;
import affidavit.util.Random;

public class BlockingResult implements Serializable {
	private Map<BlockIndex, Block>							blockMap					= new HashMap<>();
	private Map<String[], Block>							reverseSourceRecordIndex	= new HashMap<>(
			Affidavit.ENVIRONMENT.targetLineCount());
	private Map<String[], Block>							reverseTargetRecordIndex	= new HashMap<>(
			Affidavit.ENVIRONMENT.targetLineCount());
	private Map<Integer, Map<String, Collection<Block>>>	reverseValueIndices			= new HashMap<>(
			Affidavit.ENVIRONMENT.columnCount());
	private Map<Integer, ValueCounts>						valueDistributions			= new HashMap<>();
	private List<Block>										blocksWithMatches;

	public BlockingResult() {
	}

	public BlockingResult(Map<BlockIndex, Block> blockMap) {
		this.blockMap = blockMap;

		for (Block block : blockMap.values()) {
			for (String[] targetRecord : block.targetRows) {
				reverseTargetRecordIndex.put(targetRecord, block);
			}

			for (String[] sourceRecord : block.sourceRows) {
				reverseSourceRecordIndex.put(sourceRecord, block);
			}
		}
	}

	public void buildReverseBlockIndexFor(int attribute) {
		if (reverseValueIndices.containsKey(attribute)) {
			return;
		}

		reverseValueIndices.put(attribute, new HashMap<String, Collection<Block>>());

		for (Block b : blockMap.values()) {
			addToReverseBlockIndex(b, attribute);
		}
	}

	public Set<Entry<BlockIndex, Block>> getBlockEntries() {
		return blockMap.entrySet();
	}

	private void createNewBlock(BlockIndex index) {
		Block newBlock = new Block(index);
		blockMap.put(index, newBlock);
	}

	public void addSourceRow(BlockIndex index, String[] row) {
		if (!blockMap.containsKey(index)) {
			createNewBlock(index);
		}

		Block block = blockMap.get(index);
		block.addSourceRow(row);
		reverseSourceRecordIndex.put(row, block);
	}

	public void addTargetRecord(BlockIndex index, String[] row) {
		if (!blockMap.containsKey(index)) {
			createNewBlock(index);
		}

		Block block = blockMap.get(index);
		block.addTargetRow(row);
		reverseTargetRecordIndex.put(row, block);
	}

	public void addSourceRows(BlockIndex index, Collection<String[]> sourceRows) {
		if (!blockMap.containsKey(index)) {
			createNewBlock(index);
		}

		Block block = blockMap.get(index);
		block.addSources(sourceRows);
		for (String[] sourceRecord : sourceRows) {
			reverseSourceRecordIndex.put(sourceRecord, block);
		}
	}

	public void addTargetRows(BlockIndex index, Collection<String[]> targetRecords) {
		if (!blockMap.containsKey(index)) {
			createNewBlock(index);
		}

		Block block = blockMap.get(index);
		block.addTargets(targetRecords);
		for (String[] targetRecord : targetRecords) {
			reverseTargetRecordIndex.put(targetRecord, block);
		}
	}

	public List<String[]> getMatchableSourceRows() {
		List<String[]> matchableSources = new ArrayList<>();

		for (Block block : blockMap.values()) {
			if (!block.hasNoTargetRows()) {
				matchableSources.addAll(block.sourceRows);
			}
		}

		return matchableSources;
	}

	public List<String[]> getMatchableTargetRows() {
		List<String[]> matchableTargets = new ArrayList<>();

		for (Block block : blockMap.values()) {
			if (!block.hasNoSourceRows()) {
				matchableTargets.addAll(block.targetRows);
			}
		}

		return matchableTargets;
	}

	public Collection<String[]> getUnmatchedSourceRows() {
		Collection<String[]> unMatchedSources = new ArrayList<>();

		for (Block block : blockMap.values()) {
			if (block.hasNoTargetRows()) {
				unMatchedSources.addAll(block.sourceRows);
			}
		}

		return unMatchedSources;
	}

	public Collection<String[]> getUnmatchedTargetRows() {
		Collection<String[]> unMatchedTargets = new ArrayList<>();

		for (Block block : blockMap.values()) {
			if (block.hasNoSourceRows()) {
				unMatchedTargets.addAll(block.targetRows);
			}
		}

		return unMatchedTargets;
	}

	public int getMultiMatchedTargetRowCount() {
		int multiMatchedTargetCount = 0;

		for (Block block : blockMap.values()) {
			int sourceLinesCount = block.sourceRows.size();
			int targetLinesCount = block.targetRows.size();

			if (sourceLinesCount > targetLinesCount) {
				multiMatchedTargetCount += sourceLinesCount - targetLinesCount;
			}
		}

		return multiMatchedTargetCount;
	}

	public Collection<Block> getAllBlocks() {
		return blockMap.values();
	}

	public List<Block> getBlocksWithMatches() {
		if (blocksWithMatches == null) {
			blocksWithMatches = new ArrayList<>(blockMap.size());

			for (Block block : blockMap.values()) {
				if (block.hasMatches()) {
					blocksWithMatches.add(block);
				}
			}
		}

		return new ArrayList<>(blocksWithMatches);
	}

	public Collection<Block> getOneToOneBlocks() {
		Collection<Block> blocks = new ArrayList<>(blockMap.size());

		for (Block block : blockMap.values()) {
			if (block.isOneToOneBlock()) {
				blocks.add(block);
			}
		}

		return blocks;
	}

	public double getAverageBlockSize() {
		double sum = 0;
		int count = 0;
		for (Block b : blockMap.values()) {
			if (b.hasMatches()) {
				count++;
				sum += b.sourceRows.size() + b.targetRows.size();
			}

		}

		return sum / count;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		blockMap.keySet().stream().filter(string -> blockMap.get(string).hasMatches()).forEach(string -> {
			sb.append("\"").append(string).append("\" {\n").append(blockMap.get(string));
			sb.append("}\n\n");
		});

		sb.append("DROPPED SOURCE LINES:\n");
		blockMap.keySet().stream().filter(string -> blockMap.get(string).hasNoTargetRows()).forEach(string -> {
			sb.append(blockMap.get(string));
			sb.append("}\n");
		});

		sb.append("\n\nINSERTED TARGET LINES:\n");
		blockMap.keySet().stream().filter(string -> blockMap.get(string).hasNoSourceRows()).forEach(string -> {
			sb.append(blockMap.get(string));
			sb.append("}\n\n");
		});

		return sb.toString();
	}

	public String toShortString() {
		StringBuilder sb = new StringBuilder();

		sb.append(blockMap.keySet().stream().filter(string -> blockMap.get(string).hasMatches()).count());
		sb.append(" MATCHES | ");

		sb.append(blockMap.keySet().stream().filter(string -> blockMap.get(string).hasNoTargetRows()).count());
		sb.append(" DELETIONS | ");

		sb.append(blockMap.keySet().stream().filter(string -> blockMap.get(string).hasNoSourceRows()).count());
		sb.append(" INSERTIONS | ");

		return sb.toString();
	}

	public void update(int attribute, String sourceValue, String oldTargetValue, String newTargetValue) {
		Collection<Block> affectedBlocks = reverseValueIndices.get(attribute).get(oldTargetValue);
		Collection<Block> toRemove = new HashSet<>();
		Map<BlockIndex, Block> toInsert = new HashMap<>();

		for (Block block : affectedBlocks) {
			BlockIndex index = block.index;

			if (!block.hasNoSourceRows()) {
				List<String[]> affectedSourceRows = block.sourceRows.stream()
						.filter(r -> r[attribute].equals(sourceValue)).collect(Collectors.toList());

				if (!affectedSourceRows.isEmpty()) {
					BlockIndex newIndex = index.clone();

					newIndex.setAttributeValue(attribute, newTargetValue);

					Block newBlock;
					if (blockMap.containsKey(newIndex)) {
						newBlock = blockMap.get(newIndex);
					} else if (toInsert.containsKey(newIndex)) {
						newBlock = toInsert.get(newIndex);
					} else {
						newBlock = new Block(newIndex);
						toInsert.put(newIndex, newBlock);
					}

					newBlock.addSources(affectedSourceRows);

					block.sourceRows.removeAll(affectedSourceRows);
					if (block.isEmpty()) {
						toRemove.add(block);
					}
				}
			}
		}

		for (Block b : toRemove) {
			blockMap.remove(b.index);
			removeFromReverseBlockIndices(b);
		}

		for (Block b : toInsert.values()) {
			blockMap.put(b.index, b);
			addToReverseBlockIndices(b);
		}
	}

	private void addToReverseBlockIndex(Block b, int attribute) {
		String attributeValue = b.index.getValueAt(attribute);

		Map<String, Collection<Block>> attributeIndex = reverseValueIndices.get(attribute);
		if (!attributeIndex.containsKey(attributeValue)) {
			attributeIndex.put(attributeValue, new HashSet<>());
		}

		attributeIndex.get(attributeValue).add(b);
	}

	private void addToReverseBlockIndices(Block b) {
		for (int attribute : reverseValueIndices.keySet()) {
			addToReverseBlockIndex(b, attribute);
		}
	}

	private void removeFromReverseBlockIndex(Block b, int attribute) {
		String attributeValue = b.index.getValueAt(attribute);
		Map<String, Collection<Block>> reverseAttributeIndex = reverseValueIndices.get(attribute);

		reverseValueIndices.get(attribute).get(attributeValue).remove(b);

		if (reverseAttributeIndex.get(attributeValue).isEmpty()) {
			reverseAttributeIndex.remove(attributeValue);
		}
	}

	private void removeFromReverseBlockIndices(Block b) {
		for (int attribute : reverseValueIndices.keySet()) {
			removeFromReverseBlockIndex(b, attribute);
		}
	}

	@Override
	public BlockingResult clone() {
		BlockingResult clone = new BlockingResult();

		for (BlockIndex index : blockMap.keySet()) {
			BlockIndex clonedIndex = index.clone();
			Block oldBlock = blockMap.get(index);

			clone.addSourceRows(clonedIndex, oldBlock.sourceRows);
			clone.addTargetRows(clonedIndex, oldBlock.targetRows);
		}

		return clone;
	}

	public Map<String[], String[]> getSourceToTargetAlignment() {
		return getBlocksWithMatches().stream().flatMap(block -> {
			Collection<Pair<String[], String[]>> alignments = new HashSet<>();

			for (String[] sourceRecord : block.sourceRows) {
				if (block.targetRows.size() > 1) {
					System.err.println("oh: block");
					System.exit(-1);
				}

				for (String[] targetRecord : block.targetRows) {
					alignments.add(Pair.of(sourceRecord, targetRecord));
				}
			}

			return alignments.stream();
		}).collect(Collectors.toMap(pair -> pair.getLeft(), pair -> pair.getRight()));
	}

	public Map<String[], Collection<String[]>> getTargetFromSourceAlignment() {
		Map<String[], Collection<String[]>> targetToSourcesMap = new HashMap<>();

		for (Block block : getBlocksWithMatches()) {
			for (String[] targetRow : block.targetRows) {
				targetToSourcesMap.put(targetRow, block.sourceRows);
			}
		}

		return targetToSourcesMap;
	}

	public Block getBlockOfTargetRecord(String[] targetRecord) {
		return reverseTargetRecordIndex.get(targetRecord);
	}

	public Block getBlockOfSourceRecord(String[] sourceRecord) {
		return reverseSourceRecordIndex.get(sourceRecord);
	}

	public void writeTo(String path) {
		try (PrintWriter out = new PrintWriter(path)) {
			out.write(this.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<Integer> sortAttributesByAmbiguity(Collection<Integer> attributes) {
		List<Integer> attributesWithoutDistribution = attributes.stream()
				.filter(attribute -> !valueDistributions.containsKey(attribute)).collect(Collectors.toList());
		if (!attributesWithoutDistribution.isEmpty()) {
			buildValueDistributions(attributesWithoutDistribution);
		}

		return attributes.stream()
				.sorted((a1, a2) -> Integer.compare(valueDistributions.get(a1).getMaximumSourceCount(),
						valueDistributions.get(a2).getMaximumSourceCount()))
				.collect(Collectors.toList());
	}

	public List<Integer> sortAttributesByDomainSize(Collection<Integer> attributes) {
		List<Integer> attributesWithoutDistribution = attributes.stream()
				.filter(attribute -> !valueDistributions.containsKey(attribute)).collect(Collectors.toList());
		if (!attributesWithoutDistribution.isEmpty()) {
			buildValueDistributions(attributesWithoutDistribution);
		}

		return attributes.stream()
				.sorted((a1, a2) -> Integer.compare(valueDistributions.get(a1).getSourceCounts().keySet().size(),
						valueDistributions.get(a2).getSourceCounts().keySet().size()))
				.collect(Collectors.toList());
	}

	private void buildValueDistributions(List<Integer> attributes) {
		for (int attribute : attributes) {
			valueDistributions.put(attribute, new ValueCounts());
		}

		Stream<Block> blockStream;

		if (getBlocksWithMatches().size() > Math.sqrt(Affidavit.ENVIRONMENT.smallerLineCount())) {
			blockStream = getBlocksWithMatches().parallelStream();
			L.logln("Using parallel stream over blocks");
		} else {
			blockStream = getBlocksWithMatches().stream();
		}

		blockStream.forEach(block -> {
			Map<Integer, ValueCounts> blockDistributions = block.getValueDistribution(attributes);

			for (int attribute : attributes) {
				valueDistributions.get(attribute).add(blockDistributions.get(attribute));
			}
		});
	}

	public Map<Integer, ValueCounts> map() {
		return valueDistributions;
	}

	public Map<Integer, List<OperationalTransformation>> rankTransformations(Map<Integer, Map<OperationalTransformation, MutableInteger>> transformationCandidates) {
		L.logln("Finding best on " + Arrays.toString(transformationCandidates.keySet().toArray()));
		List<String[]> randomSourceRecords = Random.sampleDistinctFromList(getMatchableSourceRows(),
				BinomDistUtility.INSTANCE.cochranSampleSize(Config.NOISE, 1 - Config.CONFIDENCE));
		L.logln(randomSourceRecords.size() + " random source records");

		Set<Block> blockSample = randomSourceRecords.stream().map(r -> getBlockOfSourceRecord(r))
				.collect(Collectors.toSet());

		L.logln("resulting in " + blockSample.size() + " blocks.");

		Map<Integer, Map<OperationalTransformation, MutableInteger>> scores = transformationCandidates.keySet().stream()
				.filter(att -> !transformationCandidates.get(att).isEmpty())
				.collect(Collectors.toMap(att -> att, att -> transformationCandidates.get(att).entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey(), e -> new MutableInteger(0)))));

		for (Block randomBlock : blockSample) {
			Map<Integer, ValueCounts> targetCounts = randomBlock
					.getValueDistribution(transformationCandidates.keySet());

			Map<Integer, Map<OperationalTransformation, ValueCounts>> transformationCounts = randomBlock
					.getTransformedCounts(transformationCandidates);

			for (int attribute : transformationCandidates.keySet()) {
				if (attribute == 6) {
					System.out.print("");
				}

				for (OperationalTransformation t : transformationCandidates.get(attribute).keySet()) {
					int score = ValueCounts.getOverlap(targetCounts.get(attribute).getTargetCounts(),
							transformationCounts.get(attribute).get(t).getTargetCounts());
					scores.get(attribute).get(t).add(score);
				}
			}
		}

		Map<Integer, List<OperationalTransformation>> bestTransformations = scores.entrySet().stream().collect(
				Collectors.toMap(att -> att.getKey(), att -> att.getValue().entrySet().stream().sorted((e1, e2) -> {
					return -Long.compare(e1.getValue().get() - e1.getKey().getCosts(),
							e2.getValue().get() - e2.getKey().getCosts());
				}).limit(Config.BRANCHING_FACTOR).map(e -> e.getKey()).collect(Collectors.toList())));

		L.logln("Evaluation done by sampling " + (blockSample.size() + " different blocks."));

		return bestTransformations;
	}
}
