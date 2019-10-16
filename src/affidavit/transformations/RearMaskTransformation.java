package affidavit.transformations;

import affidavit.data.ValuePair;

public class RearMaskTransformation extends OperationalTransformation {
	private String mask;

	public RearMaskTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	protected boolean canBeConstructedFrom(ValuePair example) {
		return super.canBeConstructedFrom(example) && example.sourceValue.length() <= example.targetValue.length();
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (ValuePair.sourceValue.equals(ValuePair.targetValue)) {
			throw new UnsuitableTransformationException(this, ValuePair);
		} else {
			buildMask(ValuePair);
		}
	}

	private void buildMask(ValuePair delta) {
		String source = delta.sourceValue;
		String target = delta.targetValue;

		if (target.length() > source.length()) {
			mask = target;
		} else {
			// equal length
			int cut = target.length() - 1;
			for (int i = source.length() - 1; i >= 0; i--) {
				if (source.charAt(i) != target.charAt(i)) {
					cut = i;
				}
			}
			mask = target.substring(cut);
		}
	}

	@Override
	public String transform(String source) {
		if (source.isEmpty()) {
			return source;
		} else if (source.length() < mask.length()) {
			return mask;
		}

		return source.substring(0, source.length() - mask.length()) + mask;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			RearMaskTransformation that = (RearMaskTransformation) obj;
			return this.mask.equals(that.mask);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + mask.hashCode();
		return result;
	}

	@Override
	protected String formulaToString() {
		StringBuilder sb = new StringBuilder();

		sb.append("y.[").append(mask.length() + "] -> y\"");
		sb.append(mask).append("\"");

		return sb.toString();
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
