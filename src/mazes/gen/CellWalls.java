package mazes.gen;

import java.io.Serializable;
import java.util.Arrays;

import mazes.util.Direction;

public final class CellWalls implements Serializable {
	
	public static final long serialVersionUID = 2708271265367661455L;
	
	private final int dimensionCount;
	private final int directionCount;
	private final boolean[] walls;
	
	public CellWalls(int dimensionCount, boolean placeInitialWalls) {
		if (dimensionCount < 1) throw new IllegalArgumentException();
		this.dimensionCount = dimensionCount;
		this.directionCount = dimensionCount * 2;
		this.walls = new boolean[directionCount];
		Arrays.fill(walls, placeInitialWalls);
	}
	
	public boolean getWall(Direction side) {
		if (side == null) throw new NullPointerException();
		if (side.getDimension() >= dimensionCount) throw new IndexOutOfBoundsException();
		return walls[side.getIndex()];
	}
	public void setWall(Direction side, boolean isWall) {
		if (side == null) throw new NullPointerException();
		if (side.getDimension() >= dimensionCount) throw new IndexOutOfBoundsException();
		walls[side.getIndex()] = isWall;
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dimensionCount;
		result = prime * result + directionCount;
		result = prime * result + Arrays.hashCode(walls);
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellWalls other = (CellWalls) obj;
		if (dimensionCount != other.dimensionCount)
			return false;
		if (directionCount != other.directionCount)
			return false;
		if (!Arrays.equals(walls, other.walls))
			return false;
		return true;
	}
	@Override public String toString() {
		StringBuilder sb = new StringBuilder("{");
		for (int d=0; d<directionCount; d++) {
			if (walls[d]) {
				sb.append(new Direction(d)).append(",");
			}
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("}");
		return sb.toString();
	}
	
}
