package mazes.schematic;

import mazes.util.Direction;

public final class Coordinate implements Comparable<Coordinate> {
	
	// x represents the [isYZ ? x coordinate : y coordinate.]
	// y always represents the z coordinate.
	public final int x, y;
	
	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public boolean isDirectlyBelow(Coordinate base) {
		return x == base.x && base.y - y == 1;
	}
	public boolean isDirectlyAdjacentTo(Coordinate base) {
		return y == base.y && Math.abs(x - base.x) == 1;
	}
	public boolean isIndirectlyBelow(Coordinate base) {
		return x == base.x && base.y - y > 1;
	}
	
	public Coordinate negative() {
		return new Coordinate(-x, -y);
	}
	public Coordinate getIncrement(Direction dir) {
		switch (dir.getDimension()) {
		case 0: return new Coordinate(x + (dir.isPositive() ? 1 : -1), y);
		case 1: return new Coordinate(x, y + (dir.isPositive() ? 1 : -1));
		default: return this;
		}
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Coordinate other = (Coordinate) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
	@Override public int compareTo(Coordinate other) {
		int cmp = y - other.y;
		return cmp != 0 ? cmp : x - other.x;
	}
	@Override public String toString() {
		return "(" + x + ", " + y + ")";
	}
	
}
