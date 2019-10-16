package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class PrefixTransformation extends OperationalTransformation {
	private String prefix;

	public PrefixTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (!ValuePair.sourceValue.equals(ValuePair.targetValue)
				&& ValuePair.targetValue.endsWith(ValuePair.sourceValue)) {
			int prefixEnd = ValuePair.targetValue.length() - ValuePair.sourceValue.length();
			this.prefix = ValuePair.targetValue.substring(0, prefixEnd);
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		if (source.isEmpty()) {
			return source;
		}

		return prefix + source;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			PrefixTransformation that = (PrefixTransformation) obj;
			return Objects.equals(prefix, that.prefix);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.prefix == null) ? 0 : this.prefix.hashCode());
		return result;
	}

	@Override
	protected String formulaToString() {
		return "\"xxxx\" -> \"" + prefix + "xxxx\"";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
