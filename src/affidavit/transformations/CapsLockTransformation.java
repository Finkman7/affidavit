package affidavit.transformations;

import affidavit.data.ValuePair;

public class CapsLockTransformation extends OperationalTransformation {

	public CapsLockTransformation(ValuePair example) throws UnsuitableTransformationException {
		super(example);
	}

	@Override
	protected boolean canBeConstructedFrom(ValuePair example) {
		return super.canBeConstructedFrom(example) && example.targetValue.equals(example.sourceValue.toUpperCase());
	}

	@Override
	public boolean formulaUsesArgument() {
		return false;
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {

	}

	@Override
	protected String transform(String source) {
		return source.toUpperCase();
	}

	@Override
	protected String formulaToString() {
		return "string ->  STRING";
	}

}
