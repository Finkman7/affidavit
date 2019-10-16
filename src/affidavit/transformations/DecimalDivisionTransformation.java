package affidavit.transformations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import affidavit.data.ValuePair;

public class DecimalDivisionTransformation extends NumberTransformation {
	protected BigDecimal denominator;

	public DecimalDivisionTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		try {
			BigDecimal sourceNumber = new BigDecimal(ValuePair.sourceValue).setScale(5);
			BigDecimal targetNumber = new BigDecimal(ValuePair.targetValue).setScale(5);

			if (sourceNumber.compareTo(targetNumber) > 0) {
				this.denominator = sourceNumber.divide(targetNumber);
				return;
			}
		} catch (ArithmeticException e) {

		}

		throw new UnsuitableTransformationException(this, ValuePair);
	}

	@Override
	public String transform(String source) {
		try {
			return new BigDecimal(source).divide(denominator).stripTrailingZeros().toPlainString();
		} catch (NumberFormatException | ArithmeticException e) {
			return source;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			DecimalDivisionTransformation that = (DecimalDivisionTransformation) obj;
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
		scopes.add(TransformationScope.DECIMAL);

		return scopes;
	}

}
