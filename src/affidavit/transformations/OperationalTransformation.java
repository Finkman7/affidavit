package affidavit.transformations;

import affidavit.data.Table;
import affidavit.data.ValuePair;

public abstract class OperationalTransformation extends Transformation {
	public OperationalTransformation(ValuePair example) throws UnsuitableTransformationException {
		if (canBeConstructedFrom(example)) {
			if (hasParameters()) {
				learnParameters(example);
			}
		} else {
			throw new UnsuitableTransformationException(this, example);
		}
	}

	protected boolean hasParameters() {
		return true;
	}

	public OperationalTransformation() {

	}

	protected boolean canBeConstructedFrom(ValuePair example) {
		return !example.sourceValue.isEmpty();
	}

	@Override
	public String applyTo(String source) {
		return transform(source);
	}

	@Override
	public void applyTo(Table sourceTable, Integer columnIndex) {
		for (String[] row : sourceTable.rows) {
			row[columnIndex] = this.applyTo(row[columnIndex]);
		}
	}

	@Override
	public boolean covers(ValuePair dp) {
		return dp.targetValue.equals(applyTo(dp.sourceValue));
	}

	@Override
	public long getCosts() {
		return 1;
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
	public int hashCode() {
		return 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(getClass().getSimpleName());
		sb.append(":\t\t");
		sb.append(formulaToString());

		return sb.toString();
	}

	@Override
	public String toShortString() {
		return this.getClass().getSimpleName().substring(0, this.getClass().getSimpleName().length() - 14);
	}

	public abstract boolean formulaUsesArgument();

	protected abstract void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException;

	@Override
	protected abstract String transform(String source);

	protected abstract String formulaToString();
}
