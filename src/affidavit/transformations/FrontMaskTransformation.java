package affidavit.transformations;

import affidavit.data.ValuePair;

public class FrontMaskTransformation extends OperationalTransformation {
	private String mask;

	public FrontMaskTransformation(ValuePair dp) throws UnsuitableTransformationException {
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
			int cut = 0;
			for (int i = 0; i < source.length(); i++) {
				if (source.charAt(i) != target.charAt(i)) {
					cut = i;
				}
			}
			mask = target.substring(0, cut + 1);
		}
	}

	@Override
	public String transform(String source) {
		if (source.isEmpty()) {
			return source;
		} else if (source.length() < mask.length()) {
			return mask;
		}

		return mask + source.substring(mask.length());
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			FrontMaskTransformation that = (FrontMaskTransformation) obj;
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

		sb.append(".[").append(mask.length());
		sb.append("]y -> \"");
		sb.append(mask);
		sb.append("\"y");

		return sb.toString();
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}
}
