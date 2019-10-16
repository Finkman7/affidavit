package affidavit.search;

import java.util.*;

import affidavit.*;
import affidavit.data.*;
import affidavit.search.state.State;
import affidavit.transformations.*;

public class Blocker {
	public static void buildBlocksFor(State state, Iterable<String[]> sourceRows,
			Iterable<String[]> targetRows) {
		Map<Integer, Transformation> joinCriteria = state.getBlockingCriteria();
		Set<Integer> columnsToCheck = joinCriteria.keySet();

		BlockingResult blockingResult = new BlockingResult();
		for (String[] row : sourceRows) {
			BlockIndex key = new BlockIndex();

			for (Integer index : columnsToCheck) {
				String transformed = row[index];
				Transformation t = joinCriteria.get(index);

				if (!(t instanceof IDTransformation)) {
					transformed = t.applyTo(row[index]);
				}

				key.setAttributeValue(index, transformed);
			}

			blockingResult.addSourceRow(key, row);
		}

		for (String[] row : targetRows) {
			BlockIndex key = new BlockIndex();

			for (int index : columnsToCheck) {
				key.setAttributeValue(index, row[index]);
			}

			blockingResult.addTargetRecord(key, row);
		}

		state.setBlockingResult(blockingResult);
	}

	public static void buildBlocksFor(State state) {
		buildBlocksFor(state, Affidavit.ENVIRONMENT.SOURCE_TABLE.rows,
				Affidavit.ENVIRONMENT.TARGET_TABLE.rows);
	}
}
