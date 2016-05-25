/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package util;

import java.util.Arrays;

public final class ArrObjPair<T, U> extends Pair<T[], U> {
	
	public ArrObjPair(T[] first, U second) {
		super(first, second);
	}
	
	@Override public T[] getFirst() {
		return super.getFirst();
	}
	
	// Object
	@Override public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Pair<?, ?>)) return false;
		ArrObjPair<?, ?> other = (ArrObjPair<?, ?>)obj;
		return Arrays.equals(first, other.first) && second.equals(other.second);
	}
	@Override public int hashCode() {
		return (first == null ? 0 : Arrays.hashCode(first)) + (second == null ? 0 : second.hashCode());
	}
	@Override public String toString() {
		return String.format("<%s, %s>", Arrays.toString(first), second);
	}
	
}
