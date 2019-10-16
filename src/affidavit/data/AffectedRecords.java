package affidavit.data;

import java.util.HashSet;
import java.util.Set;

public class AffectedRecords implements Comparable<AffectedRecords> {
	public Set<String[]>	sourceRecords	= new HashSet<>();
	public Set<String[]>	targetRecords	= new HashSet<>();
	private Integer			mininmumAffected;

	public void add(RowPair rowPair) {
		sourceRecords.add(rowPair.sourceRow);
		targetRecords.add(rowPair.targetRow);
	}

	public void addAll(AffectedRecords affectedRecords) {
		sourceRecords.addAll(affectedRecords.sourceRecords);
		targetRecords.addAll(affectedRecords.targetRecords);
	}

	public int getMinimumAffected() {
		if (mininmumAffected == null) {
			mininmumAffected = Math.min(sourceRecords.size(), targetRecords.size());
		}

		return mininmumAffected;
	}

	@Override
	public int compareTo(AffectedRecords o) {
		return -Integer.compare(this.getMinimumAffected(), o.getMinimumAffected());
	}

	@Override
	public String toString() {
		return sourceRecords.size() + "/" + targetRecords.size();
	}

	public int difference() {
		return Math.abs(sourceRecords.size() - targetRecords.size());
	}
}