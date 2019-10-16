package affidavit.transformations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import affidavit.data.ValuePair;

public class IntegerDivisionTransformation extends NumberTransformation {
	protected BigInteger denominator;

	public IntegerDivisionTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	public IntegerDivisionTransformation(int denominator) {
		this.denominator = BigInteger.valueOf(denominator);
	}

	@Override
	protected void learnParameters(ValuePair valuePair) throws UnsuitableTransformationException {
		if (StringUtils.isNumeric(valuePair.sourceValue) && StringUtils.isNumeric(valuePair.targetValue)) {
			BigInteger sourceNumber = new BigInteger(valuePair.sourceValue);
			BigInteger targetNumber = new BigInteger(valuePair.targetValue);

			if (sourceNumber.abs().compareTo(targetNumber.abs()) > 0) {
				if (targetNumber.signum() == 0 && sourceNumber.mod(targetNumber).signum() == 0) {
					this.denominator = sourceNumber.divide(targetNumber);
				} else if (targetNumber.signum() != 0) {

				}
			} else {
				throw new UnsuitableTransformationException(this, valuePair);
			}
		} else {
			throw new UnsuitableTransformationException(this, valuePair);
		}
	}

	@Override
	public String transform(String source) {
		try {
			return String.valueOf(new BigInteger(source).divide(denominator));
		} catch (NumberFormatException e) {
			return source;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			IntegerDivisionTransformation that = (IntegerDivisionTransformation) obj;
			return Objects.equals(denominator, that.denominator);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.denominator == null) ? 0 : this.denominator.hashCode());
		return result;
	}

	@Override
	protected String formulaToString() {
		return "x -> [x / " + denominator + "]";
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
