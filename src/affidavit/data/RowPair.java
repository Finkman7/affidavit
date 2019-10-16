package affidavit.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import affidavit.config.Config;
import affidavit.transformations.Transformation;

public class RowPair {
	public String[]	sourceRow;
	public String[]	targetRow;

	public RowPair(String[] sourceRow, String[] targetRow) {
		this.sourceRow = sourceRow;
		this.targetRow = targetRow;
	}

	public Collection<Integer> getColumnsToAdjust(Map<Integer, Transformation> criteria) throws Exception {
		Collection<Integer> toAdjust = new ArrayList<>(sourceRow.length);

		for (int attribute : criteria.keySet()) {
			if (!criteria.get(attribute).applyTo(sourceRow[attribute]).equals(targetRow[attribute])) {
				if (toAdjust.size() >= Config.MAX_ATTRIBUTES_PER_REFINEMENT) {
					throw new Exception("Too many attributes are different.");
				}

				toAdjust.add(attribute);
			}
		}

		return toAdjust;
	}

	public Integer getOverlap(Map<Integer, Transformation> criteria) {
		int overlap = 0;

		for (int i : criteria.keySet()) {
			if (criteria.get(i).applyTo(sourceRow[i]).equals(targetRow[i])) {
				overlap++;
			}
		}

		return overlap;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(sourceRow);
		result = prime * result + Objects.hashCode(targetRow);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RowPair other = (RowPair) obj;

		return sourceRow == other.sourceRow && targetRow == other.targetRow;
	}

	@Override
	public String toString() {
		return "{\n\t" + Arrays.toString(sourceRow) + "\n\t" + Arrays.toString(targetRow) + "\n}";
	}
}
