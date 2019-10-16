package affidavit.transformations;

import java.util.Collection;

import affidavit.data.ValuePair;

public class FrontReplaceTransformation extends OperationalTransformation {
	private String	toReplace;
	private String	replacement;

	public FrontReplaceTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	protected FrontReplaceTransformation(Collection<TransformationScope> scopes, String toReplace, String replacement) {
		this.toReplace = toReplace;
		this.replacement = replacement;
	}

	@Override
	protected boolean canBeConstructedFrom(ValuePair example) {
		return super.canBeConstructedFrom(example) && !example.targetValue.endsWith(example.sourceValue)
				&& example.sourceValue.regionMatches(example.sourceValue.length() - 1, example.targetValue,
						example.targetValue.length() - 1, 1);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {
		if (ValuePair.sourceValue.equals(ValuePair.targetValue)) {
			throw new UnsuitableTransformationException(this, ValuePair);
		} else {
			buildReplacement(ValuePair);
		}
	}

	private void buildReplacement(ValuePair delta) {
		String source = delta.sourceValue;
		String target = delta.targetValue;

		int offset = 1;

		while (offset < source.length() && offset < target.length()
				&& source.charAt(source.length() - offset - 1) == target.charAt(target.length() - offset - 1)) {
			offset++;
		}

		toReplace = source.substring(0, source.length() - offset);
		replacement = target.substring(0, target.length() - offset);
	}

	@Override
	public String transform(String source) {
		if (source.isEmpty()) {
			return source;
		} else if (source.startsWith(toReplace)) {
			return replacement + source.substring(toReplace.length());
		}

		return source;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			FrontReplaceTransformation that = (FrontReplaceTransformation) obj;
			return this.toReplace.equals(that.toReplace) && this.replacement.equals(that.replacement);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.replacement.hashCode();
		return result;
	}

	@Override
	protected String formulaToString() {
		StringBuilder sb = new StringBuilder();

		sb.append("\"");
		sb.append(toReplace);
		sb.append("\"y");
		sb.append(" -> \"");
		sb.append(replacement);
		sb.append("\"y");

		return sb.toString();
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}

	@Override
	public long getCosts() {
		return 2;
	}

}
