package affidavit.transformations;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;

import affidavit.data.ValuePair;
import affidavit.util.DateUtil;

public class DateTransformation extends OperationalTransformation {
	private Entry<String, String>	format;
	private Entry<String, String>	sourceFormat;
	private Entry<String, String>	targetFormat;

	public DateTransformation(ValuePair dp) throws UnsuitableTransformationException {
		super(dp);
	}

	@Override
	public boolean formulaUsesArgument() {
		return true;
	}

	@Override
	protected boolean canBeConstructedFrom(ValuePair example) {
		if (super.canBeConstructedFrom(example)) {
			try {
				Date sourceDate = DateUtil.parse(example.sourceValue);
				Date targetDate = DateUtil.parse(example.targetValue);

				if (sourceDate.equals(targetDate)) {
					return true;
				}
			} catch (ParseException e) {
			}
		}

		return false;
	}

	@Override
	protected void learnParameters(ValuePair valuePair) throws UnsuitableTransformationException {
		sourceFormat = DateUtil.determineDateFormat(valuePair.sourceValue);
		targetFormat = DateUtil.determineDateFormat(valuePair.targetValue);
	}

	@Override
	protected String transform(String source) {
		try {
			Calendar sourceDate = (Calendar.getInstance());
			sourceDate.setTime(DateUtil.parse(source));
			String targetFormatDescr = targetFormat.getValue();
			String targetValue = "";

			for (int i = 0; i < targetFormatDescr.length(); i++) {
				char c = targetFormatDescr.charAt(i);

				if (c == 'y') {
					if (i >= targetFormatDescr.length() - 1 || targetFormatDescr.charAt(i + 1) != 'y') {
						int year = sourceDate.get(Calendar.YEAR);
						targetValue += String.format("%4d", year);
					}
				} else if (c == 'M') {
					if (i >= targetFormatDescr.length() - 1 || targetFormatDescr.charAt(i + 1) != 'M') {
						int month = sourceDate.get(Calendar.MONTH) + 1;
						targetValue += String.format("%02d", month);
					}
				} else if (c == 'd') {
					if (i >= targetFormatDescr.length() - 1 || targetFormatDescr.charAt(i + 1) != 'd') {
						int day = sourceDate.get(Calendar.DAY_OF_MONTH);
						targetValue += String.format("%02d", day);
					}
				} else {
					targetValue += c;
				}
			}

			return targetValue;
		} catch (

		ParseException e) {
			System.err.println(e);
		}

		return source;
	}

	@Override
	protected String formulaToString() {
		return sourceFormat.getValue() + " -> " + targetFormat.getValue();
	}

	// Test
	public static void main(String[] args) {
		String s0 = "10/20/2018";
		String t0 = "20-10-2018";
		ValuePair vp0 = new ValuePair(s0, t0);

		System.out.println("Possible functions for value pair " + vp0 + ":");
		for (Transformation t : TransformationFactory.createPossibleTransformationsFor(vp0)) {
			System.out.println(t + ": " + s0 + "-> " + t.transform(s0));
		}
	}
}
