package affidavit.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import affidavit.Affidavit;
import affidavit.config.Config;
import affidavit.data.Block;
import affidavit.data.RowPair;
import affidavit.search.state.State;
import affidavit.search.state.StateFactory;
import affidavit.transformations.Transformation;
import affidavit.util.L;
import affidavit.util.Random;
import affidavit.util.Timer;

public class Matcher {
	private final Affidavit					ENVIRONMENT;
	private final StateFactory				STATE_FACTORY;
	private Map<Integer, Transformation>	criteria;
	private Collection<String[]>			sourceRows;
	private Collection<String[]>			targetRows;

	private Matcher(Map<Integer, Transformation> criteria, Collection<String[]> sourceRows,
			Collection<String[]> targetRows) {
		ENVIRONMENT = Affidavit.ENVIRONMENT;
		STATE_FACTORY = ENVIRONMENT.STATE_FACTORY;
		this.criteria = criteria;
		this.sourceRows = sourceRows;
		this.targetRows = targetRows;
	}

	public Matcher(Map<Integer, Transformation> criteria) {
		ENVIRONMENT = Affidavit.ENVIRONMENT;
		STATE_FACTORY = ENVIRONMENT.STATE_FACTORY;
		this.criteria = criteria;
		sourceRows = ENVIRONMENT.SOURCE_TABLE.rows;
		targetRows = ENVIRONMENT.TARGET_TABLE.rows;
	}

	public static Map<RowPair, Integer> matchAllRows(Map<Integer, Transformation> criteria) {
		Matcher instance = new Matcher(criteria);
		return instance.match();
	}

	public static Collection<RowPair> sampleRandom(Collection<Block> blocks) {
		Collection<RowPair> rowSamples = new LinkedList<RowPair>();

		blocks.stream().filter(block -> block.hasMatches()).forEach(block -> {
			rowSamples.addAll(block.getRowSample());
		});

		return rowSamples;
	}

	public static Map<RowPair, Collection<Integer>> produceRefinementPairs(Map<Integer, Transformation> map, Collection<String[]> sourceRows, Collection<String[]> targetRows) {
		Matcher instance = new Matcher(map, sourceRows, targetRows);
		return instance.produceRefinementPairs();
	}

	private Map<RowPair, Collection<Integer>> produceRefinementPairs() {
		Map<RowPair, Collection<Integer>> pairScores = new LinkedHashMap<>();
		Map<Integer, State> singleCriterionStates = Affidavit.ENVIRONMENT.STATE_FACTORY
				.createSingleCriterionStates(criteria, false);
		Collection<RowPair> ignored = new HashSet<>();

		for (State state : singleCriterionStates.values()) {
			Blocker.buildBlocksFor(state, sourceRows, targetRows);

			for (Block b : state.getBlockingResult().getBlocksWithMatches()) {
				if (b.sourceRows.size() * b.targetRows.size() < Config.MAX_MATCHING_BLOCK_SIZE) {
					for (String[] sL : b.sourceRows) {
						for (String[] tL : b.targetRows) {
							RowPair rp = new RowPair(sL, tL);

							if (!ignored.contains(rp) && !pairScores.containsKey(rp)) {
								try {
									pairScores.put(rp, rp.getColumnsToAdjust(criteria));
								} catch (Exception e) {
									ignored.add(rp);
								}
							}
						}
					}
				} else {
					int smaller = Math.min(b.sourceRows.size(), b.targetRows.size());
					int count = Math.floorDiv(Config.MAX_MATCHING_BLOCK_SIZE, smaller);

					for (int i = 0; i < count; i++) {
						List<String[]> sampledSourceRows = Random.sampleDistinctFromList(b.sourceRows, smaller);
						List<String[]> sampledTargetRows = Random.sampleDistinctFromList(b.targetRows, smaller);

						for (int j = 0; j < smaller; j++) {
							RowPair rp = new RowPair(sampledSourceRows.get(j), sampledTargetRows.get(j));

							if (!ignored.contains(rp) && !pairScores.containsKey(rp)) {
								try {
									pairScores.put(rp, rp.getColumnsToAdjust(criteria));
								} catch (Exception e) {
									ignored.add(rp);
								}
							}
						}
					}

				}
			}
		}

		return pairScores;
	}

	private Map<RowPair, Integer> match() {
		Map<RowPair, Integer> pairScores = new HashMap<>();
		Map<Integer, State> singleCriterionStates = Affidavit.ENVIRONMENT.STATE_FACTORY
				.createSingleCriterionStates(criteria, false);

		L.logln("Mining Row Overlaps... ");
		Timer.start("calcoverlap");
		for (State state : singleCriterionStates.values()) {
			Blocker.buildBlocksFor(state, sourceRows, targetRows);
			List<Block> blocks = state.getBlockingResult().getBlocksWithMatches();
			System.out.println("Blocking with " + state + " (" + blocks.size() + " blocks )");

			for (Block b : blocks) {
				if (b.getSquaredSize() < Config.MAX_MATCHING_BLOCK_SIZE) {
					for (String[] sL : b.sourceRows) {
						for (String[] tL : b.targetRows) {
							RowPair rp = new RowPair(sL, tL);

							if (!pairScores.containsKey(rp)) {
								pairScores.put(rp, rp.getOverlap(criteria));
							}
						}
					}
				}
			}
		}

		L.logln("Matching took " + Timer.getMilliSecondsWithUnit("calcoverlap") + " resulting in " + pairScores.size()
				+ " candidates.");

		return pairScores;
	}

	public static Map<RowPair, Integer> filter(Map<RowPair, Integer> pairScores) {
		L.log("Keeping highest candidates... ");
		Timer.start("filtercandidates");

		Iterator<Entry<RowPair, Integer>> iterator = pairScores.entrySet().iterator();
		Map<String[], Integer> matchedSourceRows = new HashMap<>(Affidavit.ENVIRONMENT.sourceLineCount());
		while (iterator.hasNext()) {
			Entry<RowPair, Integer> pairScore = iterator.next();
			String[] sourceRow = pairScore.getKey().sourceRow;
			if (!matchedSourceRows.containsKey(sourceRow)) {
				matchedSourceRows.put(sourceRow, pairScore.getValue());
			} else if (matchedSourceRows.get(sourceRow) > pairScore.getValue()) {
				iterator.remove();
			}
		}

		L.logln("took " + Timer.getMilliSecondsWithUnit("filtercandidates") + " resulting in " + pairScores.size()
				+ " candidates.");

		return pairScores;
	}

	public static Map<RowPair, Integer> sort(Map<RowPair, Integer> pairScores) {
		return pairScores.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> {
					throw new AssertionError();
				}, LinkedHashMap::new));
	}

}
