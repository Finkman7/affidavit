package affidavit.transformations;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import affidavit.config.Config;
import affidavit.data.ValuePair;

public class RefinedTransformation extends Transformation {
	private OperationalTransformation	baseTransformation;
	private Map<String, String>			refinements	= new HashMap<>();

	public RefinedTransformation(OperationalTransformation baseTransformation) {
		this.baseTransformation = baseTransformation;
	}

	public void addRefinement(String sourceValue, String targetValue) {
		if (this.baseTransformation.applyTo(sourceValue).equals(targetValue)) {
			this.refinements.remove(sourceValue);
		} else {
			this.refinements.put(sourceValue, targetValue);
		}
	}

	@Override
	protected String transform(String source) {
		if (refinements.containsKey(source)) {
			return refinements.get(source);
		} else {
			return baseTransformation.applyTo(source);
		}
	}

	@Override
	public long getCosts() {
		return baseTransformation.getCosts() + refinementCosts();
	}

	private long refinementCosts() {
		return 2 * refinements.entrySet().size();
	}

	@Override
	public String toString() {
		String result = baseTransformation.toString() + " Exception(s): [";

		if (Config.PRINT_MAPS) {
			result += refinements.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue())
					.collect(Collectors.joining(", "));
		} else {
			result += refinements.entrySet().stream().count();
		}
		result += "]";

		return result;
	}

	public Set<ValuePair> getExceptions() {
		return refinements.entrySet().stream().map(e -> new ValuePair(e.getKey(), e.getValue()))
				.collect(Collectors.toSet());
	}

	public Transformation getBaseTransformation() {
		return baseTransformation;
	}

	public void addRefinements(Map<String, String> valueMap) {
		for (Entry<String, String> refinement : valueMap.entrySet()) {
			addRefinement(refinement.getKey(), refinement.getValue());
		}
	}
}
