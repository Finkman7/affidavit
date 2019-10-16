package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class SuffixTransformation extends OperationalTransformation {
	private String suffix;

	public SuffixTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (!ValuePair.sourceValue.equals(ValuePair.targetValue)
				&& ValuePair.targetValue.startsWith(ValuePair.sourceValue)) {
			this.suffix = ValuePair.targetValue.substring(ValuePair.sourceValue.length());
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		return source + suffix;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			SuffixTransformation that = (SuffixTransformation) obj;
			return Objects.equals(suffix, that.suffix);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.suffix == null) ? 0 : this.suffix.hashCode());
		return result;
	}

	@Override
	protected String formulaToString() {
		return "\"xxxx\" -> \"xxxx" + suffix + "\"";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
