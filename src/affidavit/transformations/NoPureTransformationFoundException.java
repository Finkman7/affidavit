package affidavit.transformations;

import java.util.Set;

import affidavit.data.ValuePair;

public class NoPureTransformationFoundException extends Exception {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3252883152638240571L;
	private Set<ValuePair>		columnDataPairs;

	public NoPureTransformationFoundException(Set<ValuePair> columnDataPairs) {
		this.columnDataPairs = columnDataPairs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(columnDataPairs.size())
				.append(" data pairs unexplainable with current pure transformation expresiveness.");

		return sb.toString();
	}
}
