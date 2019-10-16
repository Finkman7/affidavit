package affidavit.search.state;

public class IgnoredAssignment extends Assignment {
	@Override
	public Assignment clone() {
		return this;
	}

	@Override
	public String toString() {
		return "-";
	}

	@Override
	public String toVerboseString() {
		return toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof IgnoredAssignment;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}