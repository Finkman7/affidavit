package affidavit.search.state;

import affidavit.transformations.Transformation;

public class TransformationAssignment extends Assignment {
	public Transformation transformation;

	public TransformationAssignment(Transformation transformation) {
		this.transformation = transformation;
	}

	@Override
	public Assignment clone() {
		return new TransformationAssignment(transformation);
	}

	@Override
	public String toString() {
		return transformation.toShortString();
	}

	@Override
	public String toVerboseString() {
		return transformation.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.transformation == null) ? 0 : this.transformation.hashCode());
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
		TransformationAssignment other = (TransformationAssignment) obj;
		if (this.transformation == null) {
			if (other.transformation != null)
				return false;
		} else if (!this.transformation.equals(other.transformation))
			return false;
		return true;
	}

}
