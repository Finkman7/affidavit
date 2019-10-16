package affidavit.util;

import java.util.HashMap;
import java.util.Map;

public class Timer {
	private static Map<String, Long> startTimes = new HashMap<>();

	public static void start(String identifier) {
		Timer.startTimes.put(identifier, System.nanoTime());
	}

	public static long getNanoSeconds(String identifier) {
		long startTime = startTimes.get(identifier);
		long now = System.nanoTime();

		return now - startTime;
	}

	public static long getMilliSeconds(String identifier) {
		return (long) ((1.0 / 1E6) * getNanoSeconds(identifier));
	}

	public static String getMilliSecondsWithUnit(String identifier) {
		return milliSecondsToString(getMilliSeconds(identifier));
	}

	public static String getNanoSecondsWithUnit(String identifier) {
		return nanoSecondsToString(getNanoSeconds(identifier));
	}

	public static String milliSecondsToString(long milliSeconds) {
		return String.format("%6d", milliSeconds) + "ms";
	}

	public static String nanoSecondsToString(long nanoSeconds) {
		return String.format("%12d", nanoSeconds) + "ns";
	}
}
