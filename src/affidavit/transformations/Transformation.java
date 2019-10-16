package affidavit.transformations;

import java.io.*;
import java.util.*;

import affidavit.data.*;

public abstract class Transformation implements Serializable {
	protected abstract String transform(String source);

	public abstract long getCosts();

	public String applyTo(String source) {
		return transform(source);
	}

	public void applyTo(Table sourceTable, Integer columnIndex) {
		for (String[] row : sourceTable.rows) {
			row[columnIndex] = this.applyTo(row[columnIndex]);
		}
	}

	public boolean covers(ValuePair dp) {
		return dp.targetValue.equals(applyTo(dp.sourceValue));
	}

	public boolean covers(Collection<ValuePair> evidence) {
		for (ValuePair dp : evidence) {
			if (!covers(dp)) {
				return false;
			}
		}

		return true;
	}

	public double getCoverage(Collection<ValuePair> examples) {
		if (examples.size() == 0) {
			return 1.0;
		} else {
			int covered = 0;

			for (ValuePair dp : examples) {
				if (covers(dp)) {
					covered++;
				}
			}

			return (1.0 * covered) / examples.size();
		}
	}

	public Collection<ValuePair> getCoveredExamples(Collection<ValuePair> examples) {
		Collection<ValuePair> coveredExamples = new ArrayList<>();

		for (ValuePair dp : examples) {
			if (this.covers(dp)) {
				coveredExamples.add(dp);
			}
		}

		return coveredExamples;
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

		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(getClass().getSimpleName());

		return sb.toString();
	}

	public String toShortString() {
		return this.getClass().getSimpleName().substring(0,
				this.getClass().getSimpleName().length() - 14);
	}
}
