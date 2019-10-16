package affidavit.search.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import affidavit.Affidavit;
import affidavit.data.Block;
import affidavit.data.BlockIndex;
import affidavit.data.BlockingResult;
import affidavit.search.AlignmentMetrics;
import affidavit.transformations.IDTransformation;
import affidavit.transformations.MapTransformation;
import affidavit.transformations.OperationalTransformation;
import affidavit.transformations.RefinedTransformation;
import affidavit.transformations.Transformation;

public class State implements Serializable {
	private Assignment[]		stateAssignments;
	private BlockingResult		blockingResult;
	private AlignmentMetrics	metrics	= new AlignmentMetrics();;
	private Collection<State>	children;
	private long				transformationHeuristicLowerbound;

	public State() {
		stateAssignments = new Assignment[Affidavit.ENVIRONMENT.columnCount()];

		for (int attribute = 0; attribute < stateAssignments.length; attribute++) {
			if (Affidavit.ENVIRONMENT.IGNORED_COLUMNS.contains(attribute)) {
				stateAssignments[attribute] = Assignment.IGNORED;
			} else {
				stateAssignments[attribute] = Assignment.EMTPY;
			}
		}
	}

	private State(State other) {
		this.stateAssignments = Arrays.stream(other.stateAssignments).map(a -> a.clone()).toArray(Assignment[]::new);
	}

	public static State getFullIdState() {
		State state = new State();

		for (int attribute = 0; attribute < state.stateAssignments.length; attribute++) {
			if (Affidavit.ENVIRONMENT.IGNORED_COLUMNS.contains(attribute)) {
				state.stateAssignments[attribute] = Assignment.IGNORED;
			} else {
				state.stateAssignments[attribute] = new TransformationAssignment(IDTransformation.INSTANCE);
			}
		}

		return state;
	}

	public boolean isEndState() {
		return getUnassignedAttributes().isEmpty();
	}

	public BlockingResult getBlockingResult() {
		return blockingResult;
	}

	public Collection<State> getChildren() {
		return children;
	}

	public long getCosts() {
		return getTransformationCosts() + metrics.getCosts();
	}

	public Collection<Integer> getUnassignedAttributes() {
		return IntStream.range(0, stateAssignments.length).filter(i -> stateAssignments[i] instanceof EmptyAssignment)
				.boxed().collect(Collectors.toList());
	}

	public Collection<Integer> getDiamondAttributes() {
		return IntStream.range(0, stateAssignments.length).filter(i -> stateAssignments[i] == Assignment.DIAMOND)
				.boxed().collect(Collectors.toList());
	}

	public Map<Integer, Transformation> getBlockingCriteria() {
		return IntStream.range(0, stateAssignments.length)
				.filter(i -> stateAssignments[i] instanceof TransformationAssignment).boxed().collect(
						Collectors.toMap(i -> i, i -> ((TransformationAssignment) stateAssignments[i]).transformation));
	}

	public AlignmentMetrics getMetrics() {
		return metrics;
	}

	public void setChildren(Collection<State> children) {
		this.children = children;
	}

	public void updateBlockingResult(BlockingResult oldBlockingResult, int attributeIndex, Transformation transformation) {
		blockingResult = new BlockingResult();

		Stream<Block> blockStream = (oldBlockingResult.getAllBlocks().size() > 1000)
				? oldBlockingResult.getAllBlocks().parallelStream()
				: oldBlockingResult.getAllBlocks().stream();

		Map<BlockIndex, Block> resultingBlocks = blockStream.map(block -> {
			final BlockIndex index = block.index;

			Stream<String[]> sourceRowStream = block.sourceRows.size() > 1000 ? block.sourceRows.parallelStream()
					: block.sourceRows.stream();
			Map<String, List<String[]>> sourceRowsByValue = sourceRowStream
					.collect(Collectors.groupingBy(row -> transformation.applyTo(row[attributeIndex])));
			Stream<Map.Entry<String, List<String[]>>> sourceRowsByValueStream = sourceRowsByValue.size() > 10000
					? sourceRowsByValue.entrySet().parallelStream()
					: sourceRowsByValue.entrySet().stream();
			final Map<BlockIndex, List<String[]>> sourceRowsByBlockIndex = sourceRowsByValueStream.collect(
					Collectors.toMap(e -> index.clone().extend(attributeIndex, e.getKey()), e -> e.getValue()));

			Stream<String[]> targetRowStream = block.targetRows.size() > 10000 ? block.targetRows.parallelStream()
					: block.targetRows.stream();
			Map<String, List<String[]>> targetRowsByValue = targetRowStream
					.collect(Collectors.groupingBy(row -> row[attributeIndex]));
			Stream<Map.Entry<String, List<String[]>>> targetRowsByValueStream = targetRowsByValue.size() > 10000
					? targetRowsByValue.entrySet().parallelStream()
					: targetRowsByValue.entrySet().stream();
			final Map<BlockIndex, List<String[]>> targetRowsByBlockIndex = targetRowsByValueStream.collect(
					Collectors.toMap(e -> index.clone().extend(attributeIndex, e.getKey()), e -> e.getValue()));

			Stream<Map.Entry<BlockIndex, List<String[]>>> sourceRowsByBlockIndexStream = sourceRowsByBlockIndex
					.size() > 1000 ? sourceRowsByBlockIndex.entrySet().parallelStream()
							: sourceRowsByBlockIndex.entrySet().stream();
			Map<BlockIndex, Block> blocks = sourceRowsByBlockIndexStream
					.collect(Collectors.toMap(e -> e.getKey(), e -> {
						Block newBlock = new Block(e.getKey());
						newBlock.addSources(e.getValue());
						if (targetRowsByBlockIndex.containsKey(e.getKey())) {
							newBlock.addTargets(targetRowsByBlockIndex.get(e.getKey()));

							if (newBlock.sourceRows.size() == block.sourceRows.size()
									&& newBlock.targetRows.size() == block.targetRows.size()) {
								// block stayed the same after extension, keep row pair
								// sample
								newBlock.rowSample = block.rowSample;
							}
						}

						return newBlock;
					}));

			targetRowsByBlockIndex.entrySet().stream().filter(e -> !sourceRowsByBlockIndex.containsKey(e.getKey()))
					.forEach(e -> {
						Block newBlock = new Block(e.getKey());
						newBlock.addTargets(e.getValue());
						blocks.put(e.getKey(), newBlock);
					});

			return blocks;
		}).flatMap(e -> e.entrySet().parallelStream())
				.collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue, (x1, x2) ->

		{
					x1.addSources(x2.sourceRows);
					x1.addTargets(x2.targetRows);

					return x1;
				}));

		blockingResult = new BlockingResult(resultingBlocks);
	}

	public void setBlockingResult(BlockingResult blockingResult) {
		this.blockingResult = blockingResult;
	}

	public void setMetrics(long dropppedSourceCount, long insertedTargetCount) {
		metrics.droppedSourceCount = dropppedSourceCount;
		metrics.insertedTargetCount = insertedTargetCount;
	}

	public void setMetrics(AlignmentMetrics metrics) {
		this.metrics.droppedSourceCount = metrics.droppedSourceCount;
		this.metrics.insertedTargetCount = metrics.insertedTargetCount;
	}

	public void setTransformationHeuristicLowerbound(long transformationHeuristicLowerbound) {
		this.transformationHeuristicLowerbound = transformationHeuristicLowerbound;
	}

	public boolean containsDiamonds() {
		return Arrays.stream(stateAssignments).filter(a -> a == Assignment.DIAMOND).findAny().isPresent();
	}

	public void assign(int columnIndex, Transformation transformation) {
		stateAssignments[columnIndex] = new TransformationAssignment(transformation);
	}

	public State extend(int attributeIndex, Transformation transformation, boolean withBlocking) {
		State clone = clone();
		clone.stateAssignments[attributeIndex] = new TransformationAssignment(transformation);

		if (withBlocking) {
			clone.updateBlockingResult(blockingResult, attributeIndex, transformation);
		}

		return clone;
	}

	public State removeAssignment(int hole) {
		State clone = clone();

		clone.stateAssignments[hole] = Assignment.EMTPY;

		return clone;
	}

	public State markNonOperational(int attribute) {
		State clone = clone();

		clone.stateAssignments[attribute] = Assignment.DIAMOND;
		clone.setBlockingResult(blockingResult);
		clone.setMetrics(metrics);

		return clone;
	}

	public void addRefinement(int columnIndex, String sourceValue, String targetValue) {
		Map<String, String> valueMap = new HashMap<>();
		valueMap.put(sourceValue, targetValue);
		addRefinements(columnIndex, valueMap);
	}

	public void addRefinements(int attributeIndex, Map<String, String> map) {
		if (!(stateAssignments[attributeIndex] instanceof TransformationAssignment)) {
			return;
		}

		try {
			prepareAttributeForRefinements(attributeIndex, map);
		} catch (Exception e) {
			return;
		}

		Transformation currentTransformation = ((TransformationAssignment) stateAssignments[attributeIndex]).transformation;

		if (currentTransformation instanceof RefinedTransformation) {
			RefinedTransformation refinedTransformation = (RefinedTransformation) currentTransformation;

			for (String sourceValue : map.keySet()) {
				String newTargetValue = map.get(sourceValue);
				String oldTargetValue = currentTransformation.applyTo(sourceValue);
				refinedTransformation.addRefinement(sourceValue, newTargetValue);
				blockingResult.update(attributeIndex, sourceValue, oldTargetValue, newTargetValue);
			}
		} else {
			MapTransformation mapTransformation = (MapTransformation) currentTransformation;

			for (String sourceValue : map.keySet()) {
				String newTargetValue = map.get(sourceValue);
				String oldTargetValue = mapTransformation.applyTo(sourceValue);
				mapTransformation.setValueMapping(sourceValue, newTargetValue);
				blockingResult.update(attributeIndex, sourceValue, oldTargetValue, newTargetValue);
			}
		}
	}

	@Override
	public State clone() {
		return new State(this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(stateAssignments);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}

		State that = (State) obj;

		return Arrays.equals(stateAssignments, that.stateAssignments);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		Stream<String> assignmentStrings = Arrays.stream(stateAssignments).map(a -> {
			String returnValue;
			if (a == null) {
				returnValue = "*";
			} else if (a instanceof IgnoredAssignment) {
				returnValue = "-";
			} else {
				returnValue = a.toString();
			}

			return returnValue;
		});
		sb.append(assignmentStrings.collect(Collectors.joining("|")));
		sb.append(") ");
		sb.append(this.getCosts()).append(" ");
		sb.append("[").append(metrics).append("]");

		return sb.toString();
	}

	public String toVerboseString() {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		Stream<String> assignmentStrings = Arrays.stream(stateAssignments).map(a -> {
			String returnValue;
			if (a == null) {
				returnValue = "*";
			} else if (a instanceof IgnoredAssignment) {
				returnValue = "-";
			} else {
				returnValue = a.toString();
			}

			return returnValue;
		});
		sb.append(assignmentStrings.collect(Collectors.joining("\n")));
		sb.append(")\nScore: ");
		sb.append(this.getCosts());
		sb.append(" [").append(metrics).append("]");

		return sb.toString();
	}

	private void prepareAttributeForRefinements(int columnIndex, Map<String, String> map) throws Exception {
		if (stateAssignments[columnIndex] instanceof TransformationAssignment) {
			Transformation currentTransformation = ((TransformationAssignment) stateAssignments[columnIndex]).transformation;

			Iterator<Entry<String, String>> iter = map.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, String> e = iter.next();
				if (currentTransformation.applyTo(e.getKey()).equals(e.getValue())) {
					iter.remove();
				}
			}

			if (map.isEmpty()) {
				throw new Exception("All refinements covered by existing function.");
			} else {
				if ((currentTransformation instanceof OperationalTransformation)
						&& !(currentTransformation instanceof MapTransformation)) {
					stateAssignments[columnIndex] = new TransformationAssignment(
							new RefinedTransformation((OperationalTransformation) currentTransformation));
				}

				blockingResult.buildReverseBlockIndexFor(columnIndex);
			}
		}
	}

	private long getTransformationCosts() {
		transformationHeuristicLowerbound = 0;
		return getBlockingCriteria().values().stream().mapToLong(Transformation::getCosts).sum()
				+ transformationHeuristicLowerbound;
	}

	public void trimMapTransformations() {
		getBlockingCriteria().entrySet().stream().filter(e -> e.getValue() instanceof MapTransformation).forEach(e -> {
			Set<String> sourceDomain = blockingResult.getBlocksWithMatches().stream()
					.flatMap(b -> b.sourceRows.stream()).map(r -> r[e.getKey()]).distinct().collect(Collectors.toSet());

			Set<String> targetDomain = blockingResult.getBlocksWithMatches().stream()
					.flatMap(b -> b.targetRows.stream()).map(r -> r[e.getKey()]).distinct().collect(Collectors.toSet());

			MapTransformation t = (MapTransformation) e.getValue();
			t.trim(sourceDomain, targetDomain);
		});
	}

	public void writeTo(File file) {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		try (PrintWriter out = new PrintWriter(file)) {
			out.write(this.toVerboseString());
			out.println("\n\nAlignment:");
			out.write(this.blockingResult.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
