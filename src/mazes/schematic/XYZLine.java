/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;



public final class XYZLine {
	
	public final int x1, y1, z1;
	public final int x2, y2, z2;
	public final Colors colors;
	
	public XYZLine(int x1, int y1, int z1, int x2, int y2, int z2, Colors colors) {
//		if (activeColor == null || inactiveColor == null) throw new NullPointerException();
		// I decided that a null color means no render.
		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
		this.colors = colors;
	}
	public XYZLine(int[] p1, int[] p2, Colors colors) {
		this(p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], colors);
	}
	public XYZLine(XYZLine base, Colors colors) {
		this(base.x1, base.y1, base.z1, base.x2, base.y2, base.z2, colors);
	}
	
	public int get(int dimension, int point) {
		if (point != 1 && point != 2) throw new IllegalArgumentException();
		switch (dimension) {
		case 0: return point == 1 ? x1 : x2;
		case 1: return point == 1 ? y1 : y2;
		case 2: return point == 1 ? z1 : z2;
		default: throw new IllegalArgumentException();
		}
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		// Make hashCode return the same value for symmetric lines
		if (getOrderInt() > 0) {
			result = prime * result + x1;
			result = prime * result + x2;
			result = prime * result + y1;
			result = prime * result + y2;
			result = prime * result + z1;
			result = prime * result + z2;
		}
		else {
			result = prime * result + x2;
			result = prime * result + x1;
			result = prime * result + y2;
			result = prime * result + y1;
			result = prime * result + z2;
			result = prime * result + z1;
		}
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XYZLine other = (XYZLine) obj;
		return x1 == other.x1 && x2 == other.x2 && y1 == other.y1 && y2 == other.y2 && z1 == other.z1 && z2 == other.z2 ||
				x1 == other.x2 && x2 == other.x1 && y1 == other.y2 && y2 == other.y1 && z1 == other.z2 && z2 == other.z1;
	}
	public int getOrderInt() {
		if (x1 != x2) return x1 - x2;
		if (y1 != y2) return y1 - y2;
		if (z1 != z2) return z1 - z2;
		return 0;
	}
	@Override public String toString() { // delete to display color info:               ====
		return "[" + x1 + "," + y1 + "," + z1 + "] -> [" + x2 + "," + y2 + "," + z2 + "]";// in " + activeColor + "/" + inactiveColor;
	}
	
}
