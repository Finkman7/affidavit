package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class FrontTrimTransformation extends OperationalTransformation {
	protected int trimLength;

	public FrontTrimTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (ValuePair.sourceValue.endsWith(ValuePair.targetValue)) {
			this.trimLength = ValuePair.sourceValue.length() - ValuePair.targetValue.length();
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		return source.substring(Math.min(source.length(), this.trimLength));
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			FrontTrimTransformation that = (FrontTrimTransformation) obj;
			return Objects.equals(this.trimLength, that.trimLength);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.trimLength;
		return result;
	}

	@Override
	protected String formulaToString() {
		return "[.]{" + this.trimLength + "}xxx -> xxx";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
