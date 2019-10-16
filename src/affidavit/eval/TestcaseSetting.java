package affidavit.eval;

public class TestcaseSetting {
	public String	name;
	public double	transformationFraction;
	public double	noiseFraction;

	public TestcaseSetting(double noiseFraction, double transformationFraction) {
		this.noiseFraction = noiseFraction / (1 + noiseFraction);
		this.transformationFraction = transformationFraction;
		this.name = "N" + String.format("%2.0f", 100 * noiseFraction) + "_" + "T"
				+ String.format("%2.0f", 100 * transformationFraction);
	}

	@Override
	public String toString() {
		return name;
	}
}
