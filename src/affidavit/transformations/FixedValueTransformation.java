package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class FixedValueTransformation extends OperationalTransformation {
	protected String value;

	public FixedValueTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	public boolean canBeConstructedFrom(ValuePair example) {
		return true;
	}

	@Override
	public String transform(String string) {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			FixedValueTransformation that = (FixedValueTransformation) obj;
			return Objects.equals(this.value, that.value);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	@Override
	protected String formulaToString() {
		return "x -> \"" + this.value + "\"";
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		this.value = ValuePair.targetValue;
	}

	@Override
	public boolean formulaUsesArgument() {
		return false;
	}
}
