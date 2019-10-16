package affidavit.transformations;

import affidavit.data.ValuePair;

public class UnsuitableTransformationException extends Exception {
	/**
	 * 
	 */
	private static final long			serialVersionUID	= 6897661375573912609L;
	private OperationalTransformation	transformation;
	private ValuePair[]					dataPairs;

	public UnsuitableTransformationException(OperationalTransformation transformation, ValuePair... dataPairs) {
		this.transformation = transformation;
		this.dataPairs = dataPairs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(transformation.toShortString());
		sb.append(" can't explain ");
		sb.append(dataPairs.length);
		sb.append(" data pairs. For example");

		ValuePair dp = dataPairs[0];

		sb.append(dp.sourceValue);
		sb.append(" -> ");
		sb.append(dp.targetValue);

		return sb.toString();
	}

}
