package affidavit.search;

import java.util.Comparator;

import affidavit.search.state.State;

public class StateComparator implements Comparator<State> {
	public static final StateComparator INSTANCE = new StateComparator();

	@Override
	public int compare(State o1, State o2) {
		int result = Long.compare(o1.getCosts(), o2.getCosts());

		if (result == 0) {
			result = Integer.compare(o1.getUnassignedAttributes().size(), o2.getUnassignedAttributes().size());
		}

		if (result == 0) {
			result = Integer.compare(o1.getDiamondAttributes().size(), o2.getDiamondAttributes().size());
		}

		if (result == 0) {
			result = Integer.compare(o1.hashCode(), o2.hashCode());
		}

		return result;
	}

}
