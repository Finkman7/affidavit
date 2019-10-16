package affidavit.transformations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import affidavit.config.Config;
import affidavit.data.ValuePair;

public class MapTransformation extends OperationalTransformation {
	protected Map<String, String> valueMap;

	public MapTransformation(Collection<TransformationScope> scopes, Map<String, String> map) {
		this.valueMap = map;
	}

	public MapTransformation(Set<ValuePair> valuePairs) throws UnsuitableTransformationException {
		buildMap(valuePairs);
	}

	@Override
	protected void learnParameters(ValuePair ValuePair) throws UnsuitableTransformationException {

	}

	protected void buildMap(Set<ValuePair> examples) throws UnsuitableTransformationException {
		this.valueMap = new HashMap<>();

		for (ValuePair dp : examples) {
			if (this.valueMap.containsKey(dp.sourceValue)) {
				if (!this.valueMap.get(dp.sourceValue).equals(dp.targetValue)) {
					throw new UnsuitableTransformationException(this, dp);
				}
			} else {
				this.valueMap.put(dp.sourceValue, dp.targetValue);
			}
		}
	}

	@Override
	public String transform(String source) {
		if (this.valueMap.containsKey(source)) {
			return this.valueMap.get(source);
		} else {
			return source;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			MapTransformation that = (MapTransformation) obj;
			return this.valueMap.equals(that.valueMap);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.valueMap == null) ? 0 : this.valueMap.hashCode());
		return result;
	}

	@Override
	protected String formulaToString() {
		StringBuilder sb = new StringBuilder();

		sb.append("[");

		if (Config.PRINT_MAPS) {
			sb.append(this.valueMap.entrySet().stream().filter(entry -> !entry.getKey().equals(entry.getValue()))
					.map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining(", ")));
		} else {
			sb.append(this.valueMap.entrySet().stream().filter(entry -> !entry.getKey().equals(entry.getValue()))
					.count());
		}

		List<Entry<String, String>> unchangedMappings = this.valueMap.entrySet().stream()
				.filter(entry -> entry.getKey().equals(entry.getValue())).collect(Collectors.toList());
		if (!unchangedMappings.isEmpty()) {
			sb.append("]. Unchanged: [");
			if (Config.PRINT_MAPS) {
				sb.append(unchangedMappings.stream().map(entry -> entry.getKey()).collect(Collectors.joining(", ")));
			} else {
				sb.append(unchangedMappings.size());
			}
		}

		sb.append("]");

		return sb.toString();
	}

	@Override
	public boolean formulaUsesArgument() {
		return false;
	}

	@Override
	public long getCosts() {
		return 2 * valueMap.entrySet().size();
	}

	public boolean mapsAll(Set<String> values) {
		return valueMap.keySet().containsAll(values);
	}

	public Map<String, String> getValueMap() {
		return valueMap;
	}

	public void setValueMapping(String sourceValue, String targetValue) {
		valueMap.put(sourceValue, targetValue);
	}

	public Map<String, String> getMap() {
		return valueMap;
	}

	public void trim(Set<String> sourceDomain, Set<String> targetDomain) {
		Iterator<Entry<String, String>> iter = valueMap.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<String, String> e = iter.next();
			if (!sourceDomain.contains(e.getKey()) || !targetDomain.contains(e.getValue())) {
				iter.remove();
			}
		}
	}
}
