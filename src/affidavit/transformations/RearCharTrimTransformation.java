package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class RearCharTrimTransformation extends OperationalTransformation {
	protected char toTrim;

	public RearCharTrimTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (ValuePair.sourceValue.startsWith(ValuePair.targetValue)) {
			String tail = ValuePair.sourceValue.substring(ValuePair.targetValue.length());

			char firstChar = tail.charAt(0);
			toTrim = firstChar;
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		int i = source.length() - 1;

		while (i >= 0 && source.charAt(i) == toTrim) {
			i--;
		}

		return source.substring(0, i + 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			RearCharTrimTransformation that = (RearCharTrimTransformation) obj;
			return Objects.equals(toTrim, that.toTrim);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.toTrim;
		return result;
	}

	@Override
	protected String formulaToString() {
		return "xxx[" + toTrim + "]* -> xxx";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
