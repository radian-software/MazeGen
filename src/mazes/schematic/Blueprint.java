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
