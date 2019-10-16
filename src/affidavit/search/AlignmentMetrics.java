package affidavit.search;

import java.io.Serializable;

import affidavit.Affidavit;

public class AlignmentMetrics implements Serializable {
	private final long	DELTA	= Affidavit.ENVIRONMENT.sourceLineCount() - Affidavit.ENVIRONMENT.targetLineCount();
	public long			droppedSourceCount;
	public long			insertedTargetCount;

	public long getCosts() {
		return Affidavit.ENVIRONMENT.columnCount() * (Math.max(droppedSourceCount - DELTA, insertedTargetCount));
	}

	public long alignedCount() {
		long alignedSources = Affidavit.ENVIRONMENT.sourceLineCount() - droppedSourceCount;
		long alignedTargets = Affidavit.ENVIRONMENT.targetLineCount() - insertedTargetCount;

		return alignedSources;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(droppedSourceCount).append("|").append(insertedTargetCount);

		return sb.toString();
	}
}
