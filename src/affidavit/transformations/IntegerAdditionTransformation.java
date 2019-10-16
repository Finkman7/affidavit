package affidavit.transformations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import affidavit.data.ValuePair;

public class IntegerAdditionTransformation extends NumberTransformation {
	protected BigInteger offset;

	public IntegerAdditionTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	public IntegerAdditionTransformation(BigInteger offset) {
		this.offset = offset;
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (StringUtils.isNumeric(ValuePair.sourceValue) && StringUtils.isNumeric(ValuePair.targetValue)) {
			BigInteger sourceNumber = new BigInteger(ValuePair.sourceValue);
			BigInteger targetNumber = new BigInteger(ValuePair.targetValue);
			this.offset = targetNumber.subtract(sourceNumber);

			if (this.offset.signum() == 0) {
				throw new UnsuitableTransformationException(this, ValuePair);
			}
		} else {
			throw new UnsuitableTransformationException(this, ValuePair);
		}
	}

	@Override
	public String transform(String source) {
		try {
			return (new BigInteger(source).add(this.offset).toString());
		} catch (NumberFormatException e) {
			return source;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			IntegerAdditionTransformation that = (IntegerAdditionTransformation) obj;
			return Objects.equals(this.offset, that.offset);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.offset == null) ? 0 : this.offset.hashCode());
		return result;
	}

	@Override
	protected String formulaToString() {
		if (this.offset.signum() > 0) {
			return "x -> [x + " + this.offset + "]";
		} else {
			return "x -> [x - " + (this.offset.abs()) + "]";
		}
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}

	@Override
	protected Collection<TransformationScope> getScopes() {
		List<TransformationScope> scopes = new ArrayList<>(2);

		scopes.add(TransformationScope.INTEGER);

		return scopes;
	}
}
