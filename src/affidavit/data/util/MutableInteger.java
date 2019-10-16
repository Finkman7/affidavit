package affidavit.data.util;

public class MutableInteger implements Comparable<MutableInteger> {
	private int value;

	public MutableInteger(int value) {
		this.value = value;
	}

	public MutableInteger(MutableInteger that) {
		this.value = that.value;
	}

	public void increment() {
		++value;
	}

	public void add(int x) {
		this.value += x;
	}

	public void add(MutableInteger other) {
		this.add(other.get());
	}

	public int get() {
		return value;
	}

	@Override
	public MutableInteger clone() {
		return new MutableInteger(this);
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public int compareTo(MutableInteger that) {
		return Integer.compare(this.value, that.value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.value;
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
		MutableInteger other = (MutableInteger) obj;
		if (this.value != other.value)
			return false;
		return true;
	}

}