package affidavit.search.state;

public class EmptyAssignment extends Assignment {
	@Override
	public Assignment clone() {
		return this;
	}

	@Override
	public String toString() {
		return "*";
	}

	@Override
	public String toVerboseString() {
		return toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EmptyAssignment;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
