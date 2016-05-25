/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package util;

import java.util.HashMap;

@SuppressWarnings("serial")
public class QHashMap<K, V> extends HashMap<K, V> {
	
	@SuppressWarnings("unchecked")
	public QHashMap(Object... objs) {
		for (int i=0, j=1; j<objs.length; i++, j++) {
			put((K)objs[i], (V)objs[j]);
		}
	}
	
}
