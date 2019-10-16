package affidavit.finishing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import affidavit.Affidavit;
import affidavit.config.Config;
import affidavit.data.RowPair;
import affidavit.search.Matcher;
import affidavit.search.StateEvaluator;
import affidavit.search.state.State;
import affidavit.util.L;

public class StateRefiner {
	private State											state;
	private Map<Integer, Map<String, Collection<String[]>>>	sourceValueToTargetRecords;

	public static void refine(State endState) {
		StateRefiner refiner = new StateRefiner(endState);
		refiner.refine();
	}

	public StateRefiner(State endState) {
		state = endState;
	}

	public void refine() {
		while (true) {
			initSourceValueToTargetRecords();

			L.logln("\n--- Refining " + state + "... ");
			Map<RowPair, Collection<Integer>> refinementPairs = Matcher.produceRefinementPairs(
					state.getBlockingCriteria(), state.getBlockingResult().getUnmatchedSourceRows(),
					state.getBlockingResult().getUnmatchedTargetRows());

			Collection<Refinement> possibleRefinements = findPossibleRefinements(refinementPairs);

			if (possibleRefinements.isEmpty()) {
				break;
			}

			apply(possibleRefinements);
			StateEvaluator.evaluateState(state, false);
		}

		StateEvaluator.evaluateState(state, false);
		L.logln("\nRefinement Result:\n" + state.toVerboseString());
	}

	private void initSourceValueToTargetRecords() {
		sourceValueToTargetRecords = new HashMap<>();
		for (int i : state.getBlockingCriteria().keySet()) {
			sourceValueToTargetRecords.put(i, new HashMap<>());
		}

		state.getBlockingResult().getBlocksWithMatches().stream().forEach(block -> {
			for (int i : state.getBlockingCriteria().keySet()) {
				block.sourceRows.stream().map(r -> r[i]).distinct().forEach(sourceValue -> {
					if (!sourceValueToTargetRecords.get(i).containsKey(sourceValue)) {
						sourceValueToTargetRecords.get(i).put(sourceValue, new HashSet<>());
					}

					sourceValueToTargetRecords.get(i).get(sourceValue).addAll(block.targetRows);
				});
			}
		});
	}

	private Collection<Refinement> findPossibleRefinements(Map<RowPair, Collection<Integer>> refinementPairs) {
		Map<Refinement, Collection<RowPair>> refinementTargetRecords = new HashMap<>();
		for (Entry<RowPair, Collection<Integer>> pair : refinementPairs.entrySet()) {
			RowPair rp = pair.getKey();
			Collection<Integer> columnsToAdjust = pair.getValue();
			if (columnsToAdjust.isEmpty()) {
				L.log("");
			}
			Refinement r = new Refinement(rp, columnsToAdjust);

			if (!refinementTargetRecords.containsKey(r)) {
				refinementTargetRecords.put(r, new HashSet<>());
			}

			refinementTargetRecords.get(r).add(rp);
		}

		Collection<Refinement> possibleRefinements = refinementTargetRecords.entrySet().stream().map(e -> {
			Refinement r = e.getKey();

			for (RowPair rp : e.getValue()) {
				r.addGainedSourceRecord(rp.sourceRow);
				r.addGainedTargetRecord(rp.targetRow);
			}

			return r;
		}).filter(r -> r.netGainedPairs() >= Config.MIN_REFINEMENT_GAIN).collect(Collectors.toList());

		L.log("Found " + possibleRefinements.size() + " possible Refinements. Filtering... ");

		filter(possibleRefinements);
		L.logln(possibleRefinements.size() + " remaining.");
		return possibleRefinements;
	}

	private void filter(Collection<Refinement> possibleRefinements) {
		Iterator<Refinement> iter = possibleRefinements.iterator();

		outer: while (iter.hasNext()) {
			Refinement r = iter.next();

			if (!r.isWorthIt()) {
				if (r.replacements.size() <= 2) {
					System.out.println(r);
				}

				iter.remove();
				continue;
			}

			for (Entry<Integer, Pair<String, String>> replacement : r.replacements.entrySet()) {
				String valuetoReplace = replacement.getValue().getLeft();

				if (sourceValueToTargetRecords.get(replacement.getKey()).containsKey(valuetoReplace)) {
					Collection<String[]> lostTargetRecords = sourceValueToTargetRecords.get(replacement.getKey())
							.get(valuetoReplace);

					if (r.gainedPairs() >= lostTargetRecords.size() + Config.MIN_REFINEMENT_GAIN) {
						r.addLostTargetRecords(lostTargetRecords);

						if (r.isWorthIt()) {
							continue;
						}
					}

					iter.remove();
					continue outer;
				}
			}
		}
	}

	private void apply(Collection<Refinement> possibleRefinements) {
		List<Refinement> sortedRefinements = possibleRefinements.stream().sorted().collect(Collectors.toList());

		while (!sortedRefinements.isEmpty()) {
			Iterator<Refinement> iter = sortedRefinements.iterator();
			Refinement r = iter.next();
			iter.remove();
			L.logln("- Adding refinement " + r);

			for (int columnIndex : r.replacements.keySet()) {
				state.addRefinement(columnIndex, r.replacements.get(columnIndex).getLeft(),
						r.replacements.get(columnIndex).getRight());
			}

			propagateLastRefinement(sortedRefinements, r);
		}
	}

	private void propagateLastRefinement(List<Refinement> refinements, Refinement lastRefinement) {
		Iterator<Refinement> iter = refinements.iterator();

		outer: while (iter.hasNext()) {
			Refinement otherRefinement = iter.next();

			for (int attributeIndex : lastRefinement.replacements.keySet()) {
				if (otherRefinement.replacements.containsKey(attributeIndex)) {
					String lastSourceValueToReplace = lastRefinement.replacements.get(attributeIndex).getLeft();
					String otherSourceValueToReplace = otherRefinement.replacements.get(attributeIndex).getLeft();

					if (otherSourceValueToReplace.equals(lastSourceValueToReplace)) {
						String lastTargetValueToReplace = lastRefinement.replacements.get(attributeIndex).getRight();
						String otherTargetValueToReplace = otherRefinement.replacements.get(attributeIndex).getRight();

						if (otherTargetValueToReplace.equals(lastTargetValueToReplace)) {
							otherRefinement.replacements.remove(attributeIndex);
							if (otherRefinement.isEmpty()) {
								iter.remove();
								continue outer;
							}
						} else { // conflicting refinements
							iter.remove();
							continue outer;
						}
					}
				}
			}

			otherRefinement.gainedSourceRecords.removeAll(lastRefinement.gainedSourceRecords);
			otherRefinement.gainedTargetRecords.removeAll(lastRefinement.gainedTargetRecords);
			if (otherRefinement.netGainedPairs() < Config.MIN_REFINEMENT_GAIN) {
				iter.remove();
			}
		}
	}

	private class Refinement implements Comparable<Refinement> {
		public Map<Integer, Pair<String, String>>	replacements;
		private Set<String[]>						gainedSourceRecords	= new HashSet<>();
		private Set<String[]>						gainedTargetRecords	= new HashSet<>();
		private Set<String[]>						lostTargetRecords	= new HashSet<>();

		public boolean isWorthIt() {
			return 2 * replacements.size() < netGainedPairs() * Affidavit.ENVIRONMENT.columnCount();
		}

		public int netGainedPairs() {
			return gainedPairs() - lostTargetRecords.size();
		}

		public int gainedPairs() {
			return Math.min(gainedSourceRecords.size(), gainedTargetRecords.size());
		}

		public void addGainedSourceRecord(String[] sourceRecord) {
			this.gainedSourceRecords.add(sourceRecord);
		}

		public void addGainedTargetRecord(String[] targetRecord) {
			this.gainedTargetRecords.add(targetRecord);
		}

		public void addLostTargetRecords(Collection<String[]> lostTargetRecords) {
			this.lostTargetRecords.addAll(lostTargetRecords);
		}

		public boolean isEmpty() {
			return replacements.isEmpty();
		}

		public Refinement(RowPair rp, Collection<Integer> columnsToAdjust) {
			this.replacements = new HashMap<>();

			for (int i : columnsToAdjust) {
				String sourceValue = rp.sourceRow[i];
				String targetValue = rp.targetRow[i];

				replacements.put(i, Pair.of(sourceValue, targetValue));
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("{\n");

			replacements.keySet().stream().forEach(i -> {
				sb.append(i).append(": ").append(replacements.get(i).getKey()).append(" -> ")
						.append(replacements.get(i).getValue()).append("\n");
			});

			sb.append("} +[").append(gainedTargetRecords.size()).append("|").append(gainedSourceRecords.size())
					.append("] -").append(lostTargetRecords.size());

			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((replacements == null) ? 0 : replacements.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Refinement other = (Refinement) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (replacements == null) {
				if (other.replacements != null) {
					return false;
				}
			} else if (!replacements.equals(other.replacements)) {
				return false;
			}
			return true;
		}

		private StateRefiner getOuterType() {
			return StateRefiner.this;
		}

		@Override
		public int compareTo(Refinement that) {
			int result = -Integer.compare(this.netGainedPairs(), that.netGainedPairs());

			if (result == 0) {
				result = Integer.compare(this.replacements.size(), that.replacements.size());
			}

			if (result == 0) {
				result = Integer.compare(this.lostTargetRecords.size(), that.lostTargetRecords.size());
			}

			if (result == 0) {
				Iterator<Entry<Integer, Pair<String, String>>> iter1 = this.replacements.entrySet().iterator();
				Iterator<Entry<Integer, Pair<String, String>>> iter2 = that.replacements.entrySet().iterator();
				while (result == 0 && iter1.hasNext() && iter2.hasNext()) {
					Entry<Integer, Pair<String, String>> replacement1 = iter1.next();
					Entry<Integer, Pair<String, String>> replacement2 = iter2.next();

					result = replacement1.getValue().getLeft().compareTo(replacement2.getValue().getLeft());
				}
			}

			if (result == 0) {
				Iterator<Entry<Integer, Pair<String, String>>> iter1 = this.replacements.entrySet().iterator();
				Iterator<Entry<Integer, Pair<String, String>>> iter2 = that.replacements.entrySet().iterator();
				while (result == 0 && iter1.hasNext() && iter2.hasNext()) {
					Entry<Integer, Pair<String, String>> replacement1 = iter1.next();
					Entry<Integer, Pair<String, String>> replacement2 = iter2.next();

					result = replacement1.getValue().getRight().compareTo(replacement2.getValue().getRight());
				}
			}

			if (result == 0) {
				Iterator<Entry<Integer, Pair<String, String>>> iter1 = this.replacements.entrySet().iterator();
				Iterator<Entry<Integer, Pair<String, String>>> iter2 = that.replacements.entrySet().iterator();
				while (result == 0 && iter1.hasNext() && iter2.hasNext()) {
					Entry<Integer, Pair<String, String>> replacement1 = iter1.next();
					Entry<Integer, Pair<String, String>> replacement2 = iter2.next();

					result = replacement1.getKey().compareTo(replacement2.getKey());
				}
			}

			if (result == 0 && !this.equals(that)) {
				System.err.println("warning! compareto of refinement violates contract on " + this + " and " + that);
			}

			return result;
		}
	}

}
