package affidavit.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import affidavit.data.Table;
import affidavit.search.state.State;
import affidavit.transformations.OperationalTransformation;

public class Testcase implements Serializable {
	private static final String						CSV_SEP	= "|";

	public String									name;
	public Table									sourceTable;
	public Table									targetTable;
	public Map<Integer, OperationalTransformation>	transformations;
	public Collection<Integer>						columnsToAssign;
	public Collection<Integer>						columnsToIgnore;
	public List<String[]>							sourceNoise;
	public List<String[]>							targetNoise;
	public State									endState;

	public Testcase(String name, Table sourceTable, Table targetTable,
			Map<Integer, OperationalTransformation> transformations, Collection<Integer> columnsToAssign,
			Collection<Integer> columnsToIgnore, List<String[]> sourceNoise, List<String[]> targetNoise,
			State endState) {
		this.name = name;
		this.sourceTable = sourceTable;
		this.targetTable = targetTable;
		this.transformations = transformations;
		this.columnsToAssign = columnsToAssign;
		this.columnsToIgnore = columnsToIgnore;
		this.sourceNoise = sourceNoise;
		this.targetNoise = targetNoise;
		this.endState = endState;
	}

	@Override
	public String toString() {
		return endState.toVerboseString();
	}

	public Collection<Integer> getColumnsToAssign() {
		return columnsToAssign;
	}

	public Collection<Integer> getColumnsToIgnore() {
		return columnsToIgnore;
	}

	public void writeTo(File file) {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
			out.writeObject(this);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Testcase readFrom(File file) throws ClassNotFoundException, IOException {
		try (ObjectInputStream out = new ObjectInputStream(new FileInputStream(file))) {
			return (Testcase) out.readObject();
		}
	}

	public void writeCoreTo(File file) throws FileNotFoundException {
		List<String[]> records = sourceTable.rows.stream().filter(r -> !sourceNoise.contains(r))
				.collect(Collectors.toList());
		writeRecordsTo(records, file);
	}

	public void writeCoreImageTo(File file) throws FileNotFoundException {
		List<String[]> records = targetTable.rows.stream().filter(r -> !targetNoise.contains(r))
				.collect(Collectors.toList());
		writeRecordsTo(records, file);
	}

	public void writeSourceNoiseTo(File file) throws FileNotFoundException {
		List<String[]> records = sourceNoise;
		writeRecordsTo(records, file);
	}

	public void writeTargetNoiseTo(File file) throws FileNotFoundException {
		List<String[]> records = targetNoise;
		writeRecordsTo(records, file);
	}

	private void writeRecordsTo(List<String[]> records, File file) throws FileNotFoundException {
		try (PrintWriter out = new PrintWriter(file)) {
			out.println(Arrays.stream(sourceTable.headers).collect(Collectors.joining(CSV_SEP)));
			for (String[] record : records) {
				out.println(Arrays.stream(record).collect(Collectors.joining(CSV_SEP)));
			}
		}
	}

}
