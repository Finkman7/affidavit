package affidavit.transformations;

import java.util.Objects;

import affidavit.data.ValuePair;

public class IDTransformation extends OperationalTransformation {
	public static final OperationalTransformation INSTANCE = new IDTransformation();

	@Override
	protected boolean hasParameters() {
		return false;
	}

	@Override
	public boolean canBeConstructedFrom(ValuePair example) {
		return example.isUnchanged();
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {

	}

	@Override
	public String transform(String string) {
		return string;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getClass().getSimpleName());
	}

	@Override
	protected String formulaToString() {
		return "x -> x";
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}

	@Override
	public long getCosts() {
		return 0;
	}
}
