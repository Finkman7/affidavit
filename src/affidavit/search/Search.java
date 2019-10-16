package affidavit.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import affidavit.Affidavit;
import affidavit.config.Config;
import affidavit.search.state.State;
import affidavit.util.L;

public class Search {
	private static final int		QUEUE_PRINT_LENGTH	= 10;
	private final Affidavit			ENVIRONMENT;

	private PriorityQueue<State>	queue;
	private Set<State>				producedStates;

	public Search() {
		this.ENVIRONMENT = Affidavit.ENVIRONMENT;
	}

	public State findEndState() {
		List<State> startingStates = initStartStates();

		return search(startingStates);
	}

	private List<State> initStartStates() {
		List<State> startStates = new ArrayList<>();

		switch (Config.INITIALIZATION_STRATEGY) {
		default:
		case EMPTY:
			startStates.add(ENVIRONMENT.STATE_FACTORY.create(true));
			break;
		case SINGLE_IDs:
			Map<Integer, State> singleIDStates = ENVIRONMENT.STATE_FACTORY.createSingleIDStates(true);
			for (State state : singleIDStates.values()) {
				StateEvaluator.evaluateState(state, false);
			}
			StateEvaluator.setSingleIDStates(singleIDStates);
			startStates.addAll(singleIDStates.values().stream().collect(Collectors.toList()));
			break;
		case BEST_ID:
			startStates.add(ENVIRONMENT.STATE_FACTORY.createBestIDState());
			break;

		case FULL_IDs:
			State fullIDState = ENVIRONMENT.STATE_FACTORY.createFullIdState(true);
			startStates.add(fullIDState);
			break;
		}

		return startStates;
	}

	private State search(List<State> s0) {
		this.queue = new AffidavitQueue(ENVIRONMENT.columnCount());
		this.producedStates = new HashSet<>();
		for (State s : s0) {
			StateEvaluator.evaluateState(s, true);
			enqueue(s);
		}

		int iteration = 0;
		while (!this.queue.isEmpty()) {
			printQueue();
			iteration++;
			State curState = this.queue.poll();
			L.logln("--- " + iteration + ": Visiting " + curState);

			if (curState.isEndState()) {
				L.logln("End State found.");
				return curState;
			} else {
				StateExtender.extend(curState);
				enqueueChildrenOf(curState);
			}
		}

		return s0.get(0);
	}

	private void enqueue(State state) {
		if (!this.producedStates.contains(state)) {
			if (state.isEndState() && state.containsDiamonds()) {
				state = StateExtender.finalize(state);
				StateEvaluator.evaluateState(state, false);
			}

			if (!pruned(state)) {
				// filterQueue(state);
				this.queue.add(state);
			}
		}

		this.producedStates.add(state);
	}

	private boolean pruned(State state) {
		return state.getBlockingResult().getBlocksWithMatches().isEmpty();
	}

	private void enqueueChildrenOf(State curState) {
		Collection<State> children = curState.getChildren();
		L.logln("Trying to enqueue up to " + children.size() + " children.");

		for (State child : children) {
			System.err.println(child);
			enqueue(child);
		}
	}

	private void printQueue() {
		L.logln("Queue (" + this.queue.size() + "):");
		int count = 0;

		List<State> statesInQueue = new ArrayList<>(this.queue);
		Collections.sort(statesInQueue, StateComparator.INSTANCE);

		for (State state : statesInQueue) {
			if (++count >= QUEUE_PRINT_LENGTH) {
				break;
			}
			L.logln(state);
		}

		L.logln();
	}
}
