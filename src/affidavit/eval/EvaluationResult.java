package affidavit.eval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import affidavit.util.Timer;

public class EvaluationResult {
	private static final String		CSV_SEPARATOR	= "\t";
	private static final String[]	headers			= { "run", "runtime", "costs", "aligned", "prec", "rec", "f1",
			"acc" };

	public String					testCaseName;
	public String					configDescription;
	public long						runtime;
	public double					precision;
	public double					recall;
	public double					f1;
	public double					scoreDelta;
	public double					scoreDeltaPercent;
	public double					alignedDelta;
	public double					alignedDeltaPercent;
	private double					accuracy;

	public EvaluationResult(String testCaseName, String configDescription, double accuracy, long runtime,
			double precision, double recall, double f1, double scoreDelta, double scoreDeltaPercent,
			double alignedDelta, double alignedDeltaPercent) {
		this.testCaseName = testCaseName;
		this.configDescription = configDescription;
		this.accuracy = accuracy;
		this.runtime = runtime;
		this.precision = precision;
		this.recall = recall;
		this.f1 = f1;
		this.scoreDelta = scoreDelta;
		this.scoreDeltaPercent = scoreDeltaPercent;
		this.alignedDelta = alignedDelta;
		this.alignedDeltaPercent = alignedDeltaPercent;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		// Header
		sb.append(headersToString()).append("\n");

		// Run Name
		sb.append(String.format("%-10s", "-")).append(CSV_SEPARATOR);

		// Values
		sb.append(valuesToString());

		return sb.toString();

		// return "run" + CSV_SEPARATOR + "runtime" + CSV_SEPARATOR + "precision" + CSV_SEPARATOR + "recall"
		// + CSV_SEPARATOR + "f1" + CSV_SEPARATOR + "scoreDelta" + CSV_SEPARATOR + "scoreDeltaPercent"
		// + CSV_SEPARATOR + "alignedDelta" + CSV_SEPARATOR + "alignedDeltaPercent" + "\n" + "-" + CSV_SEPARATOR
		// + runtime + CSV_SEPARATOR + precision + CSV_SEPARATOR + recall + CSV_SEPARATOR + f1 + CSV_SEPARATOR
		// + scoreDelta + CSV_SEPARATOR + scoreDeltaPercent + CSV_SEPARATOR + alignedDelta + CSV_SEPARATOR
		// + alignedDeltaPercent;
	}

	public void writeTo(File resultFile, String runName) throws IOException {
		File resultPath = resultFile.getParentFile();
		if (!resultPath.exists()) {
			resultPath.mkdir();
		}

		boolean writeHeader = !resultFile.exists();

		try (FileWriter fw = new FileWriter(resultFile, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			if (writeHeader) {
				out.println(headersToString());
			}

			out.println(String.format("%-10s", runName) + CSV_SEPARATOR + valuesToString());
		} catch (

		IOException e) {
			e.printStackTrace();
		}

	}

	private String valuesToString() {
		return String.format("%-10s", Timer.milliSecondsToString(runtime)) + CSV_SEPARATOR
				+ String.format("%-10s", String.format("%2.1f%%", scoreDeltaPercent)) + CSV_SEPARATOR
				+ String.format("%-10s", String.format("%2.1f%%", alignedDeltaPercent)) + CSV_SEPARATOR
				+ String.format("%-10s", String.format("%1.2f", precision)) + CSV_SEPARATOR
				+ String.format("%-10s", String.format("%1.2f", recall)) + CSV_SEPARATOR
				+ String.format("%-10s", String.format("%1.2f", f1)) + CSV_SEPARATOR
				+ String.format("%-10s", String.format("%1.2f", accuracy));
	}

	private String headersToString() {
		return Arrays.stream(headers).map(header -> String.format("%-10s", header))
				.collect(Collectors.joining(CSV_SEPARATOR));
	}

	public static EvaluationResult merged(Collection<EvaluationResult> runResults) {
		String configDescription = null;
		long runtime = 0;
		double accuracy = 0;
		double precision = 0;
		double recall = 0;
		double f1 = 0;
		double scoreDelta = 0;
		double scoreDeltaPercent = 0;
		double alignedDelta = 0;
		double alignedDeltaPercent = 0;

		for (EvaluationResult result : runResults) {
			if (configDescription == null) {
				configDescription = result.configDescription;
			}

			runtime += result.runtime;
			accuracy += result.accuracy;
			precision += result.precision;
			recall += result.recall;
			f1 += result.f1;
			scoreDelta += result.scoreDelta;
			scoreDeltaPercent += result.scoreDeltaPercent;
			alignedDelta += result.alignedDelta;
			alignedDeltaPercent += result.alignedDeltaPercent;
		}

		runtime /= runResults.size();
		accuracy /= runResults.size();
		precision /= runResults.size();
		recall /= runResults.size();
		f1 /= runResults.size();
		scoreDelta /= runResults.size();
		scoreDeltaPercent /= runResults.size();
		alignedDelta /= runResults.size();
		alignedDeltaPercent /= runResults.size();

		return new EvaluationResult("avg", configDescription, accuracy, runtime, precision, recall, f1, scoreDelta,
				scoreDeltaPercent, alignedDelta, alignedDeltaPercent);
	}

}
