package affidavit.transformations;

import java.util.Collection;

import affidavit.data.ValuePair;

public abstract class NumberTransformation extends OperationalTransformation {

	public NumberTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	public NumberTransformation() {

	}

	@Override
	protected boolean canBeConstructedFrom(ValuePair example) {
		return super.canBeConstructedFrom(example)
				&& getScopes().contains(TransformationScope.getScopeOf(example.sourceValue))
				&& getScopes().contains(TransformationScope.getScopeOf(example.targetValue));
	}

	protected abstract Collection<TransformationScope> getScopes();
}
