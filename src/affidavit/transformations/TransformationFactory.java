package affidavit.transformations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import affidavit.data.RowPair;
import affidavit.data.ValuePair;
import affidavit.util.Random;

public class TransformationFactory {
	private static Map<ValuePair, Set<OperationalTransformation>> cache = new HashMap<>();

	public static OperationalTransformation getBestOperation(Set<ValuePair> valuePairs) {
		List<OperationalTransformation> possible = new ArrayList<>();

		for (ValuePair vp : valuePairs) {
			possible.addAll(TransformationFactory.createPossibleTransformationsFor(vp));
		}

		return possible.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet()
				.stream().sorted(Map.Entry.<OperationalTransformation, Long>comparingByValue().reversed()).findFirst()
				.get().getKey();
	}

	public static Set<OperationalTransformation> createPossibleTransformationsFor(ValuePair dp) {
		Set<OperationalTransformation> result = new HashSet<>();

		try {
			result.add(new FixedValueTransformation(dp));
		} catch (UnsuitableTransformationException e3) {
		}

		if (dp.isUnchanged()) {
			result.add(IDTransformation.INSTANCE);

			return result;
		}

		try {
			result.add(new CapsLockTransformation(dp));
		} catch (UnsuitableTransformationException e3) {
		}

		// Number Transformations
		try {
			result.add(new IntegerAdditionTransformation(dp));
		} catch (UnsuitableTransformationException e) {
		}

		try {
			result.add(new DecimalDivisionTransformation(dp));
		} catch (UnsuitableTransformationException e) {
		}

		// Front Transformations
		try {
			result.add(new PrefixTransformation(dp));
		} catch (UnsuitableTransformationException e1) {
		}

		try {
			result.add(new FrontTrimTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		try {
			result.add(new FrontCharTrimTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		try {
			result.add(new FrontMaskTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		try {
			result.add(new FrontReplaceTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		// Rear Transformations

		try {
			result.add(new SuffixTransformation(dp));
		} catch (UnsuitableTransformationException e1) {
		}

		try {
			result.add(new RearTrimTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		try {
			result.add(new RearCharTrimTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		try {
			result.add(new DateTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		try {
			result.add(new RearMaskTransformation(dp));
		} catch (UnsuitableTransformationException e2) {
		}

		return result;
	}

	public static MapTransformation createPartialMapTransformationFrom(Set<ValuePair> functionalValuePairs)
			throws UnsuitableTransformationException {
		return new MapTransformation(functionalValuePairs);
	}

	public static MapTransformation createGreedyMapTransformationFrom(Set<ValuePair> produceGreedyValuePairs) {
		try {
			return createPartialMapTransformationFrom(produceGreedyValuePairs);
		} catch (UnsuitableTransformationException e) {
			return null;
		}
	}

	public static MapTransformation createGreedyMapTransformationFrom(Map<ValuePair, Pair<Set<String[]>, Set<String[]>>> map) {
		Map<String, Map<String, Pair<Set<String[]>, Set<String[]>>>> valueDistribution = new HashMap<>();

		for (Entry<ValuePair, Pair<Set<String[]>, Set<String[]>>> valuePairCount : map.entrySet()) {
			String sourceValue = valuePairCount.getKey().sourceValue;
			String targetValue = valuePairCount.getKey().targetValue;

			if (!valueDistribution.containsKey(sourceValue)) {
				valueDistribution.put(sourceValue, new HashMap<String, Pair<Set<String[]>, Set<String[]>>>());
			}

			if (!valueDistribution.get(sourceValue).containsKey(targetValue)) {
				valueDistribution.get(sourceValue).put(targetValue,
						Pair.of(new HashSet<>(valuePairCount.getValue().getLeft()),
								new HashSet<>(valuePairCount.getValue().getRight())));
			} else {
				valueDistribution.get(sourceValue).get(targetValue).getLeft()
						.addAll(valuePairCount.getValue().getLeft());
				valueDistribution.get(sourceValue).get(targetValue).getRight()
						.addAll(valuePairCount.getValue().getRight());
			}
		}

		Set<ValuePair> valuePairs = valueDistribution.entrySet().stream().map(distribution -> {
			String mostFrequentTargetValue = distribution.getValue().entrySet().stream().sorted((x, y) -> {
				long xCovered = Math.min(x.getValue().getLeft().size(), x.getValue().getRight().size());
				long yCovered = Math.min(y.getValue().getLeft().size(), y.getValue().getRight().size());
				return -Long.compare(xCovered, yCovered);
			}).findFirst().get().getKey();

			return new ValuePair(distribution.getKey(), mostFrequentTargetValue);
		}).collect(Collectors.toSet());

		try {
			return createPartialMapTransformationFrom(valuePairs);
		} catch (UnsuitableTransformationException e) {
			return null;
		}
	}

	public static OperationalTransformation createRandomTransformation(List<String> columnValues) {
		TransformationScope columnType = determineColumnType(columnValues);
		List<TransformationType> possibleTransformations = TransformationType
				.getPossibleTransformationTypes(columnType);

		while (true) {
			TransformationType randomType = possibleTransformations
					.get(Random.instance.nextInt(possibleTransformations.size()));

			try {
				OperationalTransformation t = randomize(randomType, columnValues);
				if (t instanceof CapsLockTransformation) {
					System.out.println("");
				}
				return t;
			} catch (Exception e) {
				possibleTransformations.remove(randomType);
			}
		}
	}

	private static OperationalTransformation randomize(TransformationType randomType, List<String> columnValues)
			throws Exception {
		Optional<String> sourceValueCandidate;
		String sourceValue;
		String targetValue;
		int cut;
		String sign;
		int max;

		List<String> columnValuesRandomized;
		switch (randomType) {
		case CAPSLOCK:
			return new CapsLockTransformation(new ValuePair("string", "STRING"));
		case DECIMAL_DIVISION:
			sign = Random.instance.nextBoolean() ? "-" : "";
			targetValue = sign + String.valueOf(Random.instance.nextDouble());
			return new DecimalDivisionTransformation(new ValuePair("1.0", targetValue));
		case FIXED_VALUE:
			return new FixedValueTransformation(new ValuePair("x", "FIXED"));
		case FRONT_CHAR_TRIM:
			sourceValueCandidate = columnValues.stream().filter(v -> v.length() >= 2).findAny();
			if (sourceValueCandidate.isPresent()) {
				sourceValue = sourceValueCandidate.get();
				cut = 0;
				do {
					cut++;
				} while (sourceValue.charAt(cut) == sourceValue.charAt(0));
				return new FrontCharTrimTransformation(new ValuePair(sourceValue, sourceValue.substring(cut)));
			} else {
				throw new Exception("Not possible");
			}
		case FRONT_MASK:
			if (columnValues.stream().filter(v -> v.length() == 1).findAny().isPresent()) {
				throw new Exception("Not possible");
			}

			Collections.shuffle(columnValues);
			sourceValue = columnValues.get(0);
			cut = sourceValue.length() / 2;
			targetValue = IntStream.range(0, cut).mapToObj(i -> "X").collect(Collectors.joining())
					+ sourceValue.substring(cut);
			return new FrontMaskTransformation(new ValuePair(sourceValue, targetValue));
		case FRONT_REPLACE:
			if (columnValues.stream().filter(v -> v.length() < 2).count() < 0.2 * columnValues.size()) {
				throw new Exception("Not possible");
			}

			sourceValueCandidate = columnValues.stream().filter(v -> v.length() >= 2).findAny();
			if (sourceValueCandidate.isPresent()) {
				sourceValue = sourceValueCandidate.get();
				cut = Math.min("REPLACED".length(), sourceValue.length() / 2);
				targetValue = IntStream.range(0, cut).mapToObj(i -> "REPLACED".substring(i, i))
						.collect(Collectors.joining()) + sourceValue.substring(cut);
				return new FrontMaskTransformation(new ValuePair(sourceValue, targetValue));
			} else {
				throw new Exception("Not possible");
			}
		case FRONT_TRIM:
			if (columnValues.stream().filter(v -> v.length() < 2).count() < 0.2 * columnValues.size()) {
				throw new Exception("Not possible");
			}

			return new FrontTrimTransformation(new ValuePair("xy", "y"));
		case INTEGER_ADDITION:
			max = columnValues.stream().mapToInt(s -> Integer.valueOf(s)).max().getAsInt();
			int randomOffset = Random.instance.nextInt(Integer.MAX_VALUE - max - 1);
			return new IntegerAdditionTransformation(BigInteger.valueOf(randomOffset));
		case INTEGER_DIVISION:
			max = columnValues.stream().mapToInt(s -> Integer.valueOf(s)).max().getAsInt();
			return new IntegerDivisionTransformation(Random.instance.nextInt(1000 * max));
		default:
		case MAP:
			columnValuesRandomized = new ArrayList<>(columnValues);
			Collections.shuffle(columnValuesRandomized);
			columnValues = columnValues.stream().distinct().collect(Collectors.toList());
			columnValuesRandomized = columnValuesRandomized.stream().distinct().collect(Collectors.toList());

			Set<ValuePair> valuePairs = new HashSet<>();
			for (int i = 0; i < columnValues.size(); i++) {
				valuePairs.add(new ValuePair(columnValues.get(i), columnValuesRandomized.get(i) + "_random" + i));
			}
			return new MapTransformation(valuePairs);
		case PREFIX:
			return new PrefixTransformation(new ValuePair("x", "PREFIXx"));
		case REAR_TRIM:
			if (columnValues.stream().filter(v -> v.length() < 2).count() < 0.2 * columnValues.size()) {
				throw new Exception("Not possible");
			}

			return new RearTrimTransformation(new ValuePair("xy", "x"));
		case REAR_CHAR_TRIM:
			sourceValueCandidate = columnValues.stream().filter(v -> v.length() >= 2).findAny();
			if (sourceValueCandidate.isPresent()) {
				sourceValue = sourceValueCandidate.get();
				cut = sourceValue.length();
				do {
					cut--;
				} while (sourceValue.charAt(cut) == sourceValue.charAt(sourceValue.length() - 1));
				return new RearCharTrimTransformation(new ValuePair(sourceValue, sourceValue.substring(0, cut + 1)));
			} else {
				throw new Exception("Not possible");
			}
		case REAR_MASK:
			if (columnValues.stream().filter(v -> v.length() == 1).findAny().isPresent()) {
				throw new Exception("Not possible");
			}

			Collections.shuffle(columnValues);
			sourceValue = columnValues.get(0);
			cut = sourceValue.length() / 2;
			targetValue = sourceValue.substring(0, sourceValue.length() - cut)
					+ IntStream.range(0, cut).mapToObj(i -> "X").collect(Collectors.joining());
			return new RearMaskTransformation(new ValuePair(sourceValue, targetValue));
		case REAR_REPLACE:
			if (columnValues.stream().filter(v -> v.length() < 2).count() < 0.2 * columnValues.size()) {
				throw new Exception("Not possible");
			}

			sourceValueCandidate = columnValues.stream().filter(v -> v.length() >= 2).findAny();
			if (sourceValueCandidate.isPresent()) {
				sourceValue = sourceValueCandidate.get();
				cut = Math.min("REPLACED".length(), sourceValue.length() / 2);
				targetValue = sourceValue.substring(0, sourceValue.length() - cut) + IntStream.range(0, cut - 1)
						.mapToObj(i -> "REPLACED".substring(i, i)).collect(Collectors.joining());
				return new FrontMaskTransformation(new ValuePair(sourceValue, targetValue));
			} else {
				throw new Exception("Not possible");
			}
		case SUFFIX:
			return new SuffixTransformation(new ValuePair("x", "xSUFFIX"));
		}

		// throw new Exception("Invalid Transformation Type");
	}

	private static TransformationScope determineColumnType(List<String> columnValues) {
		TransformationScope columnType = TransformationScope.getScopeOf(columnValues.iterator().next());

		for (String value : columnValues.stream().distinct().collect(Collectors.toList())) {
			if (columnType.equals(TransformationScope.TEXT)) {
				break;
			}

			TransformationScope curType = TransformationScope.getScopeOf(value);

			if (curType.equals(columnType)) {
				continue;
			}

			switch (curType) {
			case EMPTY:
				break;

			case INTEGER:
				if (columnType == TransformationScope.INTEGER) {

				}
				if (columnType.equals(TransformationScope.EMPTY)) {
					columnType = TransformationScope.INTEGER;
				} else if (!columnType.equals(TransformationScope.DECIMAL)) {
					columnType = TransformationScope.TEXT;
				}
				break;

			case DECIMAL:
				if (columnType.equals(TransformationScope.INTEGER) || columnType.equals(TransformationScope.EMPTY)) {
					columnType = TransformationScope.DECIMAL;
				} else {
					columnType = TransformationScope.TEXT;
				}
				break;

			case WHITESPACE:
				columnType = TransformationScope.TEXT;
				break;

			default:
			case TEXT:
				columnType = TransformationScope.TEXT;
				break;
			}
		}

		return columnType;
	}

	public static MapTransformation createGreedyMapTransformationFrom(Collection<RowPair> rowSample, Integer attribute) {
		Set<ValuePair> valuePairs = rowSample.stream().collect(Collectors.groupingBy(r -> r.sourceRow[attribute]))
				.entrySet().parallelStream().map(e -> {
					String sourceValue = e.getKey();
					List<RowPair> recordPairs = e.getValue();

					String targetValue = recordPairs.stream()
							.collect(Collectors.groupingBy(r -> r.targetRow[attribute], Collectors.counting()))
							.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
							.findFirst().map(r -> r.getKey()).get();

					return new ValuePair(sourceValue, targetValue);
				}).collect(Collectors.toSet());

		return createGreedyMapTransformationFrom(valuePairs);
	}
}
