package affidavit.util;

import org.apache.commons.lang3.ArrayUtils;

import affidavit.config.Config;

public class L {
	private static final LoggingClass[] activeLoggingClasses = { LoggingClass.STRUCTURE_CONTENT };

	public static void log(Object o) {
		log(o.toString());
	}

	public static void logln(Object o) {
		logln(o.toString());
	}

	public static void logln(String s) {
		if (Config.VERBOSE_MODE) {
			System.out.println(s);
		}
	}

	public static void log(String s) {
		if (Config.VERBOSE_MODE) {
			System.out.print(s);
		}
	}

	public static void logln(String s, LoggingClass loggingClass) {
		if (ArrayUtils.contains(activeLoggingClasses, loggingClass)) {
			System.out.println(s);
		}
	}

	public static void log(String s, LoggingClass loggingClass) {
		if (ArrayUtils.contains(activeLoggingClasses, loggingClass)) {
			System.out.print(s);
		}
	}

	public static void logln() {
		if (Config.VERBOSE_MODE) {
			System.out.println();
		}
	}
}
