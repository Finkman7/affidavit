package affidavit.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.BinomialDistribution;

public class BinomDistUtility {
	private static final double								zValue		= 1.96;
	public static BinomDistUtility							INSTANCE	= new BinomDistUtility(Config.NOISE,
			Config.CONFIDENCE);
	public final int										lowerBound;
	private static final Map<Integer, Map<Integer, Double>>	cache		= new HashMap<>();

	public BinomDistUtility(double noise, double confidence) {
		for (int N = 0; true; N++) {
			BinomialDistribution dist = new BinomialDistribution(N, 1 - noise);

			for (int k = 0; k <= N; k++) {
				double p = dist.cumulativeProbability(k);

				if (1 - p < confidence) {
					if (k > 5) {
						lowerBound = N;
						return;
					} else {
						break;
					}
				}
			}
		}
	}

	public double cummulativeProbability(int N, int k) {
		if (!cache.containsKey(N) || !cache.get(N).containsKey(k)) {
			BinomialDistribution dist = new BinomialDistribution(N, 1 - Config.NOISE);
			if (!cache.containsKey(N)) {
				cache.put(N, new HashMap<>());
			}

			cache.get(N).put(k, dist.cumulativeProbability(k));
		}

		return cache.get(N).get(k);
	}

	public int cochranSampleSize(double p, double e) {
		return (int) (zValue * zValue * p * (1 - p) / (e * e));
	}
}
