package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class FrontCharTrimTransformation extends OperationalTransformation {
	protected char toTrim;

	public FrontCharTrimTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (ValuePair.sourceValue.endsWith(ValuePair.targetValue)) {
			String front = ValuePair.sourceValue.substring(0,
					ValuePair.sourceValue.length() - ValuePair.targetValue.length());
			char firstChar = front.charAt(0);
			toTrim = firstChar;
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		int i = 0;

		while (i < source.length() && source.charAt(i) == toTrim) {
			i++;
		}

		return source.substring(i);
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			FrontCharTrimTransformation that = (FrontCharTrimTransformation) obj;
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
		return "[" + toTrim + "]*xxx -> xxx";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
