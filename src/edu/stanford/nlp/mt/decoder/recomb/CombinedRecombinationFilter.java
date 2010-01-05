package edu.stanford.nlp.mt.decoder.recomb;

import java.util.*;



/**
 * 
 * @author danielcer
 *
 * @param <S>
 */
public class CombinedRecombinationFilter<S> implements
		RecombinationFilter<S> {
	public static enum CombinationRule { AND, OR }
	public static final CombinationRule DEFAULT_COMBINATION_RULE = CombinationRule.AND;
	
	final RecombinationFilter<S>[] filters;
	final CombinationRule combinationRule;
	
	
	
	@SuppressWarnings("unchecked")
	public RecombinationFilter<S> clone() {
		try {			
			return (RecombinationFilter<S>)super.clone();
		} catch (CloneNotSupportedException e) { return null; /* wnh */ }
	}
	/**
	 * 
	 */
	public CombinedRecombinationFilter(RecombinationFilter<S>... filters) {
		this.filters = Arrays.copyOf(filters, filters.length);
		combinationRule = DEFAULT_COMBINATION_RULE;
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public CombinedRecombinationFilter(List<RecombinationFilter<S>> filters) {
		this.filters = filters.toArray(new RecombinationFilter[0]);
		combinationRule = DEFAULT_COMBINATION_RULE;
	}
	
	
	/**
	 * 
	 */
	public CombinedRecombinationFilter(CombinationRule combinationRule, RecombinationFilter<S>... filters) {
		this.filters = Arrays.copyOf(filters, filters.length);
		this.combinationRule = combinationRule;
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public CombinedRecombinationFilter(CombinationRule combinationRule, List<RecombinationFilter<S>> filters) {
		this.filters = (RecombinationFilter<S>[])filters.toArray();
		this.combinationRule = combinationRule;
	}
	
	@Override
	public boolean combinable(S hypA, S hypB) {
		switch(combinationRule) {
		case AND:
			for (RecombinationFilter<S> filter : filters) {
				if (!filter.combinable(hypA, hypB)) return false;
			}
			return true;
		case OR:
			for (RecombinationFilter<S> filter : filters) {
				if (filter.combinable(hypA, hypB)) return true;
			}
			return false;
		default:
			throw new RuntimeException(String.format("Unsupported combination rule: %s", combinationRule));
		}
	}

	@Override
	public long recombinationHashCode(S hyp) {
		long hashCode = 0;
		long multiplier = 0x5DEECE66DL;
		for (RecombinationFilter<S> filter : filters) {
			long localHashCode = filter.recombinationHashCode(hyp);
			hashCode = multiplier*hashCode + 0xBL;
			hashCode += multiplier*localHashCode + 0xBL;
		}
		return hashCode;
	}
}
