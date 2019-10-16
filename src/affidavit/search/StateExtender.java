package affidavit.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import affidavit.config.BinomDistUtility;
import affidavit.config.Config;
import affidavit.data.Block;
import affidavit.data.BlockingResult;
import affidavit.data.RowPair;
import affidavit.data.ValuePair;
import affidavit.data.util.MutableInteger;
import affidavit.data.util.ValueCounts;
import affidavit.search.state.State;
import affidavit.transformations.MapTransformation;
import affidavit.transformations.OperationalTransformation;
import affidavit.transformations.TransformationFactory;
import affidavit.transformations.UnsuitableTransformationException;
import affidavit.util.L;
import affidavit.util.Random;
import affidavit.util.Timer;

public class StateExtender {
	public static void extend(State state) {
		Timer.start("total");
		List<Integer> sortedAttributes = state.getBlockingResult()
				.sortAttributesByAmbiguity(state.getUnassignedAttributes());
		L.logln("Sorting took " + Timer.getMilliSecondsWithUnit("total") + " "
				+ state.getBlockingResult().getBlocksWithMatches().size() + " blocks.");
		List<Integer> queue = new ArrayList<Integer>(Config.QUEUE_WIDTH);

		int i = 0;
		while (i < Config.BRANCHING_FACTOR && i < sortedAttributes.size()) {
			queue.add(sortedAttributes.get(i));
			i++;
		}

		Set<State> children = new HashSet<>();
		Set<Integer> diamondAttributes = new HashSet<>();
		Collection<RowPair> rowSample = null;

		while (children.isEmpty() && i <= sortedAttributes.size()) {
			Timer.start("best");
			Map<Integer, Map<OperationalTransformation, MutableInteger>> transformationCandidates = induceFunctions(
					state.getBlockingResult(), queue);
			L.logln("induce: " + Timer.getMilliSecondsWithUnit("best"));
			transformationCandidates = transformationCandidates.entrySet().stream().filter(e -> !e.getValue().isEmpty())
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
			Map<Integer, List<OperationalTransformation>> bestCandidates = state.getBlockingResult()
					.rankTransformations(transformationCandidates);
			L.logln("best: " + Timer.getMilliSecondsWithUnit("best"));

			for (Integer attribute : queue) {
				if (!bestCandidates.containsKey(attribute)) {
					diamondAttributes.add(attribute);
					continue;
				}

				boolean mapNeeded = true;
				List<State> extensions = new ArrayList<State>(Config.QUEUE_WIDTH + 1);
				for (OperationalTransformation t : bestCandidates.get(attribute)) {
					State extension = state.extend(attribute, t, true);
					extensions.add(extension);
					StateEvaluator.evaluateState(extension, true);
					if (extension.getMetrics().alignedCount() >= state.getMetrics().alignedCount()) {
						mapNeeded = false;
					}
				}

				if (mapNeeded) {
					if (rowSample == null) {
						rowSample = Matcher.sampleRandom(state.getBlockingResult().getBlocksWithMatches());
					}

					MapTransformation greedyMapTransformation = TransformationFactory
							.createGreedyMapTransformationFrom(rowSample, attribute);
					State mapExtension = state.extend(attribute, greedyMapTransformation, true);
					StateEvaluator.evaluateState(mapExtension, true);
					extensions.add(mapExtension);

					Collections.sort(extensions, StateComparator.INSTANCE);

					int mapRank = extensions.indexOf(mapExtension);
					if (mapRank == 0) {
						L.logln(mapExtension + "is better than all " + extensions.size());
						diamondAttributes.add(attribute);
					} else {
						children.addAll(extensions.subList(0, mapRank));
					}
				} else {
					children.addAll(extensions);
				}
			}

			if (i < sortedAttributes.size()) {
				queue.clear();
				queue.add(sortedAttributes.get(i));
			}
			i++;
		}

		if (children.isEmpty()) {
			State child = state;
			for (Integer diamondAttribute : diamondAttributes) {
				child = child.markNonOperational(diamondAttribute);
			}
			children.add(child);
		}
		List<State> realChildren = new ArrayList<State>();
		for (State child : children) {
			if (child.isEndState() && child.containsDiamonds()) {
				child = finalize(child);
				StateEvaluator.evaluateState(child, false);
			}

			realChildren.add(child);
		}

		state.setChildren(realChildren);
		L.logln("total " + Timer.getMilliSecondsWithUnit("total"));
	}

	protected static Map<Integer, Map<OperationalTransformation, MutableInteger>> induceFunctions(BlockingResult blockingResult, List<Integer> queue) {
		List<String[]> randomTargetRecords = Random.sampleDistinctFromList(blockingResult.getMatchableTargetRows(),
				BinomDistUtility.INSTANCE.lowerBound);

		Map<Integer, Map<OperationalTransformation, MutableInteger>> functionCandidates = new HashMap<>();

		queue.stream().forEach(attribute -> {
			functionCandidates.put(attribute, new HashMap<>());
			for (String[] targetRecord : randomTargetRecords) {
				String targetValue = targetRecord[attribute];
				Block block = blockingResult.getBlockOfTargetRecord(targetRecord);
				Map<Integer, ValueCounts> counts = block.getValueDistribution(queue);

				ValueCounts attributeCounts = counts.get(attribute);
				Map<OperationalTransformation, MutableInteger> possible = functionCandidates.get(attribute);
				Set<OperationalTransformation> blockCandidates = new HashSet<OperationalTransformation>();
				for (String sourceValue : attributeCounts.getSourceCounts().keySet()) {
					blockCandidates.addAll(TransformationFactory
							.createPossibleTransformationsFor(new ValuePair(sourceValue, targetValue)));
				}

				for (OperationalTransformation t : blockCandidates) {
					if (!possible.containsKey(t)) {
						possible.put(t, new MutableInteger(1));
					} else {
						possible.get(t).increment();
					}
				}
			}

			L.logln("Found " + functionCandidates.get(attribute).size() + " possible functions on " + attribute
					+ " from " + randomTargetRecords.size() + " random target records.");

			filter(functionCandidates.get(attribute), randomTargetRecords.size());
			L.logln("Filtered to " + functionCandidates.get(attribute).size() + ".");
		});

		return functionCandidates;
	}

	private static void filter(Map<OperationalTransformation, MutableInteger> hits, int sampleSize) {
		Iterator<Entry<OperationalTransformation, MutableInteger>> iter = hits.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<OperationalTransformation, MutableInteger> e = iter.next();

			if (BinomDistUtility.INSTANCE.cummulativeProbability(sampleSize,
					e.getValue().get()) < (1 - Config.CONFIDENCE)) {
				// L.logln("Removing " + e.getKey() + "(" + e.getValue().get() +
				// "/" + sampleSize + ")");
				iter.remove();
			}
		}

		if (hits.size() > Config.MAX_EVALUATIONS) {
			List<Entry<OperationalTransformation, MutableInteger>> best = hits.entrySet().stream()
					.sorted(Map.Entry.<OperationalTransformation, MutableInteger>comparingByValue().reversed())
					.limit(Config.MAX_EVALUATIONS).collect(Collectors.toList());

			iter = hits.entrySet().iterator();

			while (iter.hasNext()) {
				if (!best.contains(iter.next())) {
					iter.remove();
				}
			}
		}
	}

	protected State produceGreedyMapExtension(State state, int column, Map<ValuePair, MutableInteger> sampledValuePairCounts)
			throws UnsuitableTransformationException {
		Map<String, Map<String, MutableInteger>> valueDistribution = new HashMap<>();

		for (Entry<ValuePair, MutableInteger> valuePairCount : sampledValuePairCounts.entrySet()) {
			String sourceValue = valuePairCount.getKey().sourceValue;
			String targetValue = valuePairCount.getKey().targetValue;

			if (!valueDistribution.containsKey(sourceValue)) {
				valueDistribution.put(sourceValue, new HashMap<String, MutableInteger>());
			}

			if (!valueDistribution.get(sourceValue).containsKey(targetValue)) {
				valueDistribution.get(sourceValue).put(targetValue, new MutableInteger(valuePairCount.getValue()));
			} else {
				valueDistribution.get(sourceValue).get(targetValue).add(valuePairCount.getValue());
			}
		}

		Set<ValuePair> valuePairs = valueDistribution.entrySet().stream().map(distribution -> {
			String mostFrequentTargetValue = distribution.getValue().entrySet().stream()
					.sorted((targetValueCount1, targetValueCount2) -> -targetValueCount1.getValue()
							.compareTo(targetValueCount2.getValue()))
					.findFirst().get().getKey();

			return new ValuePair(distribution.getKey(), mostFrequentTargetValue);
		}).collect(Collectors.toSet());

		OperationalTransformation mapTransformation = TransformationFactory
				.createPartialMapTransformationFrom(valuePairs);

		return state.extend(column, mapTransformation, true);
	}

	public static State finalize(State state) {
		L.logln("Finalizing " + state);
		List<Integer> diamondAttributes = state.getBlockingResult()
				.sortAttributesByDomainSize(state.getDiamondAttributes());

		for (int attribute : diamondAttributes) {
			Collection<RowPair> rowSample = Matcher.sampleRandom(state.getBlockingResult().getBlocksWithMatches());

			MapTransformation greedyMapTransformation = TransformationFactory
					.createGreedyMapTransformationFrom(rowSample, attribute);
			state = state.extend(attribute, greedyMapTransformation, true);
		}

		state.trimMapTransformations();

		return state;
	}
}
