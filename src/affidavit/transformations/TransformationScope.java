package affidavit.transformations;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

public enum TransformationScope {
	EMPTY, WHITESPACE, INTEGER, TEXT, DECIMAL;
	private static TransformationScope[] fullScope = values();

	boolean includes(String source) {
		return this.equals(getScopeOf(source));
	}

	public static TransformationScope getScopeOf(String source) {
		if (source.isEmpty()) {
			return TransformationScope.EMPTY;
		} else if (source.matches("\\s+")) {
			return TransformationScope.WHITESPACE;
		} else if (source.matches("\\S+") && isBigInteger(source)) {
			return TransformationScope.INTEGER;
		} else if (source.matches("\\S+") && isBigDecimal(source)) {
			return TransformationScope.DECIMAL;
		} else {
			return TransformationScope.TEXT;
		}
	}

	public static boolean isBigInteger(String string) {
		try {
			new BigInteger(string);

			return true;

		} catch (NumberFormatException e) {

		}

		return false;
	}

	public static boolean isBigDecimal(String string) {
		try {
			new BigDecimal(string);

			return true;

		} catch (NumberFormatException e) {

		}

		return false;
	}

	public static TransformationScope[] getFullscopes() {
		return fullScope;
	}

	public static Collection<TransformationScope> getFullscopeCollection() {
		return Arrays.asList(fullScope);
	}
};