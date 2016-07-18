package mazes.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Direction {
	
	public static final String dimensionalLetters = "xyzw";
	
	private final int dimension;
	private final boolean isPositive;
	
	public Direction(int dimension, boolean isPositive) {
		if (dimension < 0) throw new IllegalArgumentException();
		this.dimension = dimension;
		this.isPositive = isPositive;
	}
	public Direction(int index) {
		if (index < 0) throw new IllegalArgumentException();
		this.dimension = index / 2;
		this.isPositive = index % 2 != 0;
	}
	
	public static List<Direction> getDirections(int dimension) {
		if (dimension < 1) throw new IllegalArgumentException();
		List<Direction> directions = new ArrayList<>();
		for (int d=0; d<dimension; d++) {
			directions.add(new Direction(d, false));
			directions.add(new Direction(d, true));
		}
		return directions;
	}
	public static List<Direction> getPositiveDirections(int dimension) {
		if (dimension < 1) throw new IllegalArgumentException();
		List<Direction> directions = new ArrayList<>();
		for (int d=0; d<dimension; d++) {
			directions.add(new Direction(d, true));
		}
		return directions;
	}
	
	public Direction getOpposite() {
		return new Direction(dimension, !isPositive);
	}
	
	public int[] getIncrement(int[] index) {
		if (index == null) throw new NullPointerException();
		int[] newIndex = Arrays.copyOf(index, index.length);
		newIndex[dimension] += isPositive ? 1 : -1;
		return newIndex;
	}
	public Integer[] getIncrement(Integer[] index) {
		if (index == null) throw new NullPointerException();
		Integer[] newIndex = Arrays.copyOf(index, index.length);
		newIndex[dimension] += isPositive ? 1 : -1;
		return newIndex;
	}
	
	public int getDimension() {
		return dimension;
	}
	public boolean isPositive() {
		return isPositive;
	}
	public int getIndex() {
		return dimension * 2 + (isPositive ? 1 : 0);
	}
	public int[] getOffsets(int dimensions) {
		int[] offset = new int[dimensions];
		offset[dimension] = isPositive ? 1 : -1;
		return offset;
	}
	public int getOffset(int dimension) {
		if (dimension < 0) throw new IllegalArgumentException();
		if (dimension == this.dimension) return isPositive ? 1 : -1;
		return 0;
	}
	public Direction crossProduct(Direction other) {
		switch (getDimension()) {
		case 0: switch (other.getDimension()) {
		case 0: return null;
		case 1: return new Direction(2, isPositive == other.isPositive);
		case 2: return new Direction(1, isPositive ^ other.isPositive); // ^  <==>  !(==)
		}
		case 1: switch (other.getDimension()) {
		case 0: return new Direction(2, isPositive ^ other.isPositive);
		case 1: return null;
		case 2: return new Direction(0, isPositive == other.isPositive);
		}
		case 2: switch (other.getDimension()) {
		case 0: return new Direction(1, isPositive == other.isPositive);
		case 1: return new Direction(0, isPositive ^ other.isPositive);
		case 2: return null;
		}
		}
		throw new UnsupportedOperationException();
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dimension;
		result = prime * result + (isPositive ? 1231 : 1237);
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Direction other = (Direction) obj;
		if (dimension != other.dimension)
			return false;
		if (isPositive != other.isPositive)
			return false;
		return true;
	}
	@Override public String toString() {
		try {
			return (isPositive ? "+" : "-") + dimensionalLetters.substring(dimension, dimension+1);
		}
		catch (StringIndexOutOfBoundsException e) {
			throw new UnsupportedOperationException();
		}
	}
	
}
