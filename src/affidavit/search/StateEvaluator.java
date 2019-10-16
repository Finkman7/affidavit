package affidavit.search;

import java.util.Map;

import affidavit.config.Config;
import affidavit.config.InitializationStrategy;
import affidavit.data.Block;
import affidavit.search.state.State;

public class StateEvaluator {
	private State						state;
	private Long						minimumInsertedTargetCount	= 0L;
	private Long						minimumDroppedSourceCount	= 0L;
	private static Map<Integer, State>	singleIDStates;

	public static void setSingleIDStates(Map<Integer, State> singleIDStates) {
		StateEvaluator.singleIDStates = singleIDStates;
	}

	private StateEvaluator(State state) {
		this.state = state;
	}

	public static void evaluateState(State state, boolean withHeuristic) {
		StateEvaluator instance = new StateEvaluator(state);

		if (state.isEndState()) {
			instance.evaluateFinalCosts();
		} else {
			instance.evaluateIntermediateCosts(withHeuristic);
		}
	}

	private void evaluateFinalCosts() {
		long insertedTargetCount = 0L;
		long dropppedSourceCount = 0L;

		for (Block joinBlock : state.getBlockingResult().getAllBlocks()) {
			if (joinBlock.hasNoSourceRows()) {
				insertedTargetCount += joinBlock.targetRows.size();
			} else if (joinBlock.hasNoTargetRows()) {
				dropppedSourceCount += joinBlock.sourceRows.size();
			} else {
				if (joinBlock.targetRows.size() > 1 || joinBlock.sourceRows.size() > 1) {
					dropppedSourceCount += joinBlock.sourceRows.size() - 1;
					insertedTargetCount += joinBlock.targetRows.size() - 1;
				}
			}
		}

		state.setMetrics(dropppedSourceCount, insertedTargetCount);
	}

	private void evaluateIntermediateCosts(boolean withHeuristic) {
		for (Block joinBlock : state.getBlockingResult().getAllBlocks()) {
			if (joinBlock.hasMoreTargetsThanSources()) {
				minimumInsertedTargetCount += joinBlock.targetRows.size() - joinBlock.sourceRows.size();
			}

			if (joinBlock.hasMoreSourcesThanTarget()) {
				minimumDroppedSourceCount += joinBlock.sourceRows.size() - joinBlock.targetRows.size();
			}
		}

		state.setMetrics(minimumDroppedSourceCount, minimumInsertedTargetCount);

		if (withHeuristic) {
			state.setTransformationHeuristicLowerbound(calculateAttributeFunctionLowerbound());
		}
	}

	private long calculateAttributeFunctionLowerbound() {
		if (Config.INITIALIZATION_STRATEGY.equals(InitializationStrategy.SINGLE_IDs)) {
			return singleIDStates.entrySet().stream().filter(e -> {
				boolean attributeIsUnassigned = state.getUnassignedAttributes().contains(e.getKey());
				AlignmentMetrics idStateMetrics = e.getValue().getMetrics();
				AlignmentMetrics stateMetrics = state.getMetrics();
				boolean metricsAreWorse = idStateMetrics.droppedSourceCount > stateMetrics.droppedSourceCount
						|| idStateMetrics.insertedTargetCount > stateMetrics.insertedTargetCount;

				return attributeIsUnassigned && metricsAreWorse;
			}).count();
		} else if (Config.INITIALIZATION_STRATEGY.equals(InitializationStrategy.BEST_ID)) {
			return state.getUnassignedAttributes().size();
		}

		return 0;
	}
}
