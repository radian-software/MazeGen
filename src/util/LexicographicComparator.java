/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package util;

import java.util.Comparator;
import java.util.Objects;

public class LexicographicComparator<T> implements Comparator<T> {
	
	private final Comparator<T>[] comparators;
	
	@SafeVarargs public LexicographicComparator(Comparator<T>... comparators) {
		this.comparators = Objects.requireNonNull(comparators);
	}
	
	@Override public int compare(T o1, T o2) {
		int cmp = 0;
		for (int i=0; i<comparators.length; i++) {
			cmp = comparators[i].compare(o1, o2);
			if (cmp != 0) return cmp;
			i += 1;
		}
		return cmp;
	}
	
}
