/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package util;


public class Triplet<T, U, V> extends Pair<T, U> {
	
	protected final V third;
	
	public Triplet(T first, U second, V third) {
		super(first, second);
		this.third = third;
	}
	
	public V getThird() {
		return third;
	}
	
	// Object
	@Override public String toString() {
		return String.format("<%s, %s, %s>", first, second, third);
	}
	
}
