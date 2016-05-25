/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("serial")
public final class Blueprint extends ArrayList<XYZLine> {
	
	public Blueprint() {}
	
	public Blueprint(int initialCapacity) {
		super(initialCapacity);
	}
	
	public Blueprint(Collection<? extends XYZLine> c) {
		super(c);
	}
	
}
