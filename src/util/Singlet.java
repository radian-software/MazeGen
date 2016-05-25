/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package util;

public class Singlet<T> {
	
	protected T first;
	
	public Singlet(T first) {
		this.first = first;
	}
	
	public T getFirst() {
		return first;
	}
	
	// Object
	@Override public String toString() {
		return String.format("<%s>", first);
	}
	
}
