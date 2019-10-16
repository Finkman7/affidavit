package affidavit.search.state;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import affidavit.Affidavit;
import affidavit.data.RowPair;
import affidavit.data.Table;
import affidavit.data.ValuePair;
import affidavit.data.util.MutableInteger;
import affidavit.search.Blocker;
import affidavit.search.Matcher;
import affidavit.transformations.IDTransformation;
import affidavit.transformations.OperationalTransformation;
import affidavit.transformations.Transformation;
import affidavit.transformations.TransformationFactory;
import affidavit.util.L;
import affidavit.util.Timer;

public class StateFactory {
	private Affidavit ENVIRONMENT;

	public StateFactory(Table sourceTable, Table targetTable) {
		this.ENVIRONMENT = Affidavit.ENVIRONMENT;
	}

	public State create(boolean withBlocking) {
		State state = new State();

		if (withBlocking) {
			Blocker.buildBlocksFor(state, ENVIRONMENT.SOURCE_TABLE.rows, ENVIRONMENT.TARGET_TABLE.rows);
		}

		return state;
	}

	public State createFullIdState(boolean withBlocking) {
		State state = State.getFullIdState();

		if (withBlocking) {
			Blocker.buildBlocksFor(state, ENVIRONMENT.SOURCE_TABLE.rows, ENVIRONMENT.TARGET_TABLE.rows);
		}

		return state;
	}

	public State createBestIDState() {
		Map<Integer, Transformation> idTransformations = ENVIRONMENT.COLUMNS_TO_ASSIGN.stream()
				.collect(Collectors.toMap(i -> i, i -> IDTransformation.INSTANCE));
		L.logln("Creating Single ID States for Overlap Matching");
		Map<RowPair, Integer> pairScores = Matcher.matchAllRows(idTransformations);
		pairScores = Matcher.sort(pairScores);
		pairScores = Matcher.filter(pairScores);
		int idColumnCount = Math.max(0, getMostFrequentScore(pairScores));
		Map<Integer, MutableInteger> idCount = countIDs(pairScores);

		State result;
		do {
			result = create(false);
			for (int idAttribute : idCount.entrySet().stream()
					.sorted(Map.Entry.<Integer, MutableInteger>comparingByValue().reversed()).limit(idColumnCount)
					.map(e -> e.getKey()).collect(Collectors.toList())) {
				result.assign(idAttribute, IDTransformation.INSTANCE);
			}

			Blocker.buildBlocksFor(result, ENVIRONMENT.SOURCE_TABLE.rows, ENVIRONMENT.TARGET_TABLE.rows);
			idColumnCount--;
		} while (result.getBlockingResult().getBlocksWithMatches().isEmpty() && idColumnCount >= 0);

		return result;
	}

	public Map<Integer, State> createSingleIDStates(boolean withBlocking) {
		return createSingleCriterionStates(Affidavit.ENVIRONMENT.COLUMNS_TO_ASSIGN.stream()
				.collect(Collectors.toMap(i -> i, i -> IDTransformation.INSTANCE)), withBlocking);
	}

	public Map<Integer, State> createSingleCriterionStates(Map<Integer, Transformation> criteria, boolean withBlocking) {
		State emptyState = create(false);
		Map<Integer, State> singleStates = new HashMap<>();

		for (int attribute : criteria.keySet()) {
			State extension = emptyState.extend(attribute, criteria.get(attribute), false);
			if (withBlocking) {
				Blocker.buildBlocksFor(extension);
			}
			singleStates.put(attribute, extension);
		}

		return singleStates;
	}

	private Map<Integer, MutableInteger> countIDs(Map<RowPair, Integer> pairScores) {
		L.log("Counting IDs... ");
		Timer.start("countIDs");

		Map<Integer, MutableInteger> idCount = new HashMap<>();
		for (int i : ENVIRONMENT.COLUMNS_TO_ASSIGN) {
			idCount.put(i, new MutableInteger(0));
		}
		pairScores.entrySet().stream().forEach(e -> {
			String[] sourceLine = e.getKey().sourceRow;
			String[] targetLine = e.getKey().targetRow;

			for (int i : ENVIRONMENT.COLUMNS_TO_ASSIGN) {
				if (sourceLine[i].equals(targetLine[i])) {
					idCount.get(i).increment();
				}
			}
		});

		L.logln("took " + Timer.getMilliSecondsWithUnit("countIDs") + ".\n");

		return idCount;
	}

	private Map<Integer, Map<OperationalTransformation, MutableInteger>> mineTransformations(Map<RowPair, Integer> pairScores) {
		L.log("Mining Transformations... ");
		Timer.start("miningtransformations");

		Map<Integer, Map<OperationalTransformation, MutableInteger>> transformationCount = new HashMap<>();
		for (int i : ENVIRONMENT.COLUMNS_TO_ASSIGN) {
			transformationCount.put(i, new HashMap<>());
		}
		pairScores.entrySet().stream().forEach(e -> {
			String[] sourceLine = e.getKey().sourceRow;
			String[] targetLine = e.getKey().targetRow;

			for (int i : ENVIRONMENT.COLUMNS_TO_ASSIGN) {
				ValuePair vp = new ValuePair(sourceLine[i], targetLine[i]);
				Set<OperationalTransformation> transformations = TransformationFactory
						.createPossibleTransformationsFor(vp);
				for (OperationalTransformation t : transformations) {
					Map<OperationalTransformation, MutableInteger> count = transformationCount.get(i);

					if (!count.containsKey(t)) {
						count.put(t, new MutableInteger(1));
					} else {
						count.get(t).increment();
					}
				}
			}

		});

		L.logln("took " + Timer.getMilliSecondsWithUnit("miningtransformations") + ".\n");

		return transformationCount;
	}

	private int getMostFrequentScore(Map<RowPair, Integer> pairScores) {
		Optional<Pair<Integer, Integer>> mostFrequent = pairScores.entrySet().stream()
				.collect(Collectors.groupingBy(Map.Entry::getValue)).entrySet().stream()
				.map(x -> Pair.of(x.getKey(), x.getValue().size())).max(Comparator.comparing(x -> x.getValue()));

		if (mostFrequent.isPresent()) {
			return mostFrequent.get().getLeft();
		} else {
			return 0;
		}
	}
}
