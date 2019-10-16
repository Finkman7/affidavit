package affidavit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class Random {
	public static java.util.Random instance = new java.util.Random();

	public static <T> ArrayList<T> sampleDistinctFromList(List<T> candidates, int sampleSize) {
		ArrayList<T> result = new ArrayList<>(sampleSize);
		int[] range = IntStream.rangeClosed(0, candidates.size() - 1).toArray();

		for (int i = 0; i < sampleSize && i < candidates.size(); i++) {
			int randomIndex = instance.nextInt(range.length - i);
			result.add(candidates.get(range[randomIndex]));
			range[randomIndex] = range[range.length - i - 1];
		}

		return result;
	}

	public static ArrayList<String> sampleDistinctFromCollection(Collection<String> collection, int sampleSize) {
		return sampleDistinctFromList(new ArrayList<>(collection), sampleSize);
	}
}
