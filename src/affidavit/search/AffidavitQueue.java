package affidavit.search;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import affidavit.config.Config;
import affidavit.search.state.State;

public class AffidavitQueue extends PriorityQueue<State> {
	private Map<Integer, PriorityQueue<State>> queues;

	public AffidavitQueue(int attCount) {
		super(StateComparator.INSTANCE);
		this.queues = IntStream.rangeClosed(0, attCount).boxed()
				.collect(Collectors.toMap(i -> i, i -> new PriorityQueue<>(StateComparator.INSTANCE.reversed())));
	}

	@Override
	public boolean add(State e) {
		int assigned = e.getBlockingCriteria().keySet().size();
		PriorityQueue<State> queueAtLevel = queues.get(assigned);

		if (queueAtLevel.size() >= Math.max(1, Config.QUEUE_WIDTH - assigned + 1)) {
			State worstInQueue = queueAtLevel.peek();
			if (StateComparator.INSTANCE.compare(worstInQueue, e) > 0) {
				super.remove(queueAtLevel.remove());
				queueAtLevel.add(e);
				return super.add(e);
			} else {
				return true;
			}
		} else {
			queueAtLevel.add(e);
			return super.add(e);
		}
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof State) {
			State toRemove = (State) o;

			int assigned = toRemove.getBlockingCriteria().keySet().size();
			PriorityQueue<State> queueAtLevel = queues.get(assigned);
			queueAtLevel.remove(toRemove);

			return super.remove(o);
		} else {
			return false;
		}

	}

	@Override
	public State poll() {
		State first = super.poll();
		int assigned = first.getBlockingCriteria().keySet().size();
		PriorityQueue<State> queueAtLevel = queues.get(assigned);
		queueAtLevel.remove(first);

		return first;
	}

}
