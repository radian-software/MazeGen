package mazes.svg;

import mazes.schematic.Coordinate;
import mazes.schematic.Dimensions;

public final class SVGCoordinate implements Comparable<SVGCoordinate> {
	
	public final int x, y;
	
	public SVGCoordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public SVGCoordinate(Coordinate p) {
		this(p.x, p.y);
	}
	
	public int svgX() {
		return x;
	}
	public int svgY() {
		return Dimensions.documentHeight() - y;
	}
	
	// Comparable<SVGCoordinate>
	@Override public int compareTo(SVGCoordinate other) {
		int cmp = y - other.y;
		return cmp == 0 ? x - other.x : cmp;
	}
	// Object
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SVGCoordinate other = (SVGCoordinate) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}
	@Override public String toString() {
		return String.format("(%d, %d)", x, y);
	}
	
}
