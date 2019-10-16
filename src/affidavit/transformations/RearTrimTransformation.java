package affidavit.transformations;

import java.util.Collection;
import java.util.Objects;

import affidavit.data.ValuePair;

public class RearTrimTransformation extends OperationalTransformation {
	protected int trimLength;

	public RearTrimTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	public RearTrimTransformation(Collection<TransformationScope> scopes, int trimLength) {
		this.trimLength = trimLength;
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (ValuePair.sourceValue.startsWith(ValuePair.targetValue)) {
			this.trimLength = ValuePair.sourceValue.length() - ValuePair.targetValue.length();
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		return source.substring(0, Math.max(0, source.length() - this.trimLength));
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			RearTrimTransformation that = (RearTrimTransformation) obj;
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
		return "xxx[.]{" + this.trimLength + "} -> xxx";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
