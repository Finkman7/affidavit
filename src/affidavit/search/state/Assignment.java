package affidavit.search.state;

import java.io.Serializable;

public abstract class Assignment implements Serializable {
	public static final IgnoredAssignment	IGNORED	= new IgnoredAssignment();
	public static final EmptyAssignment		EMTPY	= new EmptyAssignment();
	public static final DiamondAssignment	DIAMOND	= new DiamondAssignment();

	@Override
	public abstract Assignment clone();

	@Override
	public abstract String toString();

	public abstract String toVerboseString();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();
}
