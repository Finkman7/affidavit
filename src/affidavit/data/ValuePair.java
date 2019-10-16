package affidavit.data;

/**
 * Stores a source and a corresponding target value.
 * 
 * @author mfink
 *
 */
public class ValuePair {
	public String	sourceValue;
	public String	targetValue;

	public ValuePair(String sourceValue, String targetValue) {
		this.sourceValue = sourceValue;
		this.targetValue = targetValue;
	}

	/**
	 * 
	 * @return true if sourceValue and targetValue are equal, false if not.
	 */
	public boolean isUnchanged() {
		return sourceValue.equals(targetValue);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(sourceValue).append(" -> ").append(targetValue);

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.sourceValue == null) ? 0 : this.sourceValue.hashCode());
		result = prime * result + ((this.targetValue == null) ? 0 : this.targetValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValuePair other = (ValuePair) obj;
		if (this.sourceValue == null) {
			if (other.sourceValue != null)
				return false;
		} else if (!this.sourceValue.equals(other.sourceValue))
			return false;
		if (this.targetValue == null) {
			if (other.targetValue != null)
				return false;
		} else if (!this.targetValue.equals(other.targetValue))
			return false;
		return true;
	}
}
