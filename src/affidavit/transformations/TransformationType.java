package affidavit.transformations;

import java.util.ArrayList;
import java.util.List;

public enum TransformationType {
	CAPSLOCK, DECIMAL_DIVISION, FIXED_VALUE, FRONT_CHAR_TRIM, FRONT_MASK, FRONT_REPLACE, FRONT_TRIM, ID, INTEGER_ADDITION, INTEGER_DIVISION, MAP, PREFIX, REAR_TRIM, REAR_MASK, REAR_CHAR_TRIM, SUFFIX, REAR_REPLACE;

	public static List<TransformationType> getPossibleTransformationTypes(TransformationScope type) {
		List<TransformationType> possibleTypes = new ArrayList<TransformationType>(TransformationType.values().length);

		possibleTypes.add(FIXED_VALUE);

		if (type.equals(TransformationScope.EMPTY)) {
			return possibleTypes;
		}

		possibleTypes.add(MAP);
		possibleTypes.add(MAP);
		possibleTypes.add(PREFIX);
		possibleTypes.add(SUFFIX);
		possibleTypes.add(FRONT_MASK);
		possibleTypes.add(REAR_MASK);
		possibleTypes.add(FRONT_CHAR_TRIM);
		possibleTypes.add(REAR_CHAR_TRIM);
		possibleTypes.add(FRONT_REPLACE);
		possibleTypes.add(REAR_REPLACE);

		switch (type) {
		case DECIMAL:
			possibleTypes.add(DECIMAL_DIVISION);
			break;
		case EMPTY:
			break;
		case INTEGER:
			possibleTypes.add(INTEGER_ADDITION);
			break;
		case TEXT:
			possibleTypes.add(CAPSLOCK);
			break;
		case WHITESPACE:
			break;
		default:
			break;
		}

		return possibleTypes;
	}
}
