/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import mazes.gen.CellWalls;
import mazes.util.Direction;

public final class SidePiece implements Piece {
	
	public final Direction normalDirection;
	public final int width, height;
	private final int normalMazeLength;
	private final CellWalls[][] perfs;
	private final List<Coordinate> holes;
	
	public SidePiece(Direction normalDirection, int sideLength, int height, int normalMazeLength) {
		this.normalDirection = Objects.requireNonNull(normalDirection);
		if (normalDirection.getDimension() != 0 && normalDirection.getDimension() != 1) throw new IllegalArgumentException();
		if (sideLength < 2 || height < 2) throw new IllegalArgumentException();
		this.width = sideLength;
		this.height = height;
		this.perfs = new CellWalls[width][height];
		for (int x=0; x<width; x++) for (int y=0; y<height; y++) {
			perfs[x][y] = new CellWalls(3, false);
		}
		this.holes = new ArrayList<Coordinate>();
		this.normalMazeLength = normalMazeLength;
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public boolean isPositive() {
		return normalDirection.isPositive();
	}
	public int getNormalDimension() {
		return normalDirection.getDimension();
	}
	public int getParallelDimension() {
		return 1 - normalDirection.getDimension();
	}
	public int getVerticalDimension() {
		return 2;
	}
	public boolean getPerforation(int x, int y, Direction side) {
		return perfs[x][y].getWall(side);
	}
	public void setPerforation(int x, int y, Direction side, boolean isPerforation) {
		perfs[x][y].setWall(side, isPerforation);
		int newX = x + side.getOffset(0), newY = y + side.getOffset(1);
		if (!(newX < 0 || newX >= width || newY < 0 || newY >= height)) {
			perfs[newX][newY].setWall(side.getOpposite(), isPerforation);
		}
	}
	public void addHole(Coordinate coord) {
		holes.add(coord);
	}
	public List<Coordinate> getHoles() {
		return holes;
	}
	
	public Blueprint getBlueprint(Colors outlineColors, Colors perforationColors) {
		Blueprint lines = new Blueprint();
		lines.addAll(getRectangle(
				coordinateToLocation(-1, -1),
				getParallelDimension(), width,
				getVerticalDimension(), height,
				outlineColors
		));
		for (Coordinate hole : holes) {
			lines.addAll(getRectangle(
					coordinateToLocation(hole.x - 1, hole.y - 1),
					getParallelDimension(), 1,
					getVerticalDimension(), 1,
					outlineColors
			));
		}
		for (Direction d : new Direction[] {new Direction(0, true), new Direction(1, true)}) {
			for (int x = 0; x < width - (d.getDimension() == 0 ? 1 : 0); x++) {
				for (int y = 0; y < height - (d.getDimension() == 1 ? 1 : 0); y++) {
					if (perfs[x][y].getWall(d)) {
						lines.add(new XYZLine(
								coordinateToLocation(x - (d.getDimension() == 1 ? 1 : 0), y - (d.getDimension() == 0 ? 1 : 0)),
								coordinateToLocation(x, y),
								perforationColors
								));
					}
				}
			}
		}
		
		return lines;
	}
	private int[] coordinateToLocation(int x, int y) {
		int[] corner;
		switch (normalDirection.getDimension()) {
		case 0: // YZ
			corner = normalDirection.isPositive() ? new int[] {normalMazeLength - 1, x, y} : new int[] {-1, x, y};
			break;
		case 1: // XZ
			corner = normalDirection.isPositive() ? new int[] {x, normalMazeLength - 1, y} : new int[] {x, -1, y};
			break;
		default: throw new AssertionError();
		}
		return corner;
	}
	private List<XYZLine> getRectangle(int[] corner, int widthDimension, int width, int heightDimension, int height, Colors colors) {
		int[][] points = new int[4][];
		Arrays.setAll(points, index -> Arrays.copyOf(corner, 3));
		points[1][widthDimension] += width;
		points[2][widthDimension] += width;
		points[2][heightDimension] += height;
		points[3][heightDimension] += height;
		List<XYZLine> lines = new ArrayList<>();
		for (int i=0; i<4; i++) {
			int j = (i + 1) % 4;
			lines.add(new XYZLine(points[i], points[j], colors));
		}
		return lines;
	}
	
	@Override public int[] getSchematicGridCorner() {
		switch (normalDirection.getDimension()) {
		case 0: return normalDirection.isPositive() ? new int[] {normalMazeLength, 0, 0} : new int[] {0, 0, 0};
		case 1: return normalDirection.isPositive() ? new int[] {0, normalMazeLength, 0} : new int[] {0, 0, 0};
		default: throw new AssertionError();
		}
	}
	@Override public Direction getXDirection() {
		switch (normalDirection.getDimension()) {
		case 0: return new Direction(1, true);
		case 1: return new Direction(0, true);
		default: throw new AssertionError();
		}
	}
	@Override public Direction getYDirection() {
		return new Direction(2, true);
	}
	@Override public Direction getZDirection() {
		return new Direction(normalDirection.getDimension(), true);
	}
	@Override public String getType() {
		return "SidePiece";
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + ((holes == null) ? 0 : holes.hashCode());
		result = prime * result + ((normalDirection == null) ? 0 : normalDirection.hashCode());
		result = prime * result + normalMazeLength;
		result = prime * result + Arrays.hashCode(perfs);
		result = prime * result + width;
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SidePiece other = (SidePiece) obj;
		if (height != other.height)
			return false;
		if (holes == null) {
			if (other.holes != null)
				return false;
		} else if (!holes.equals(other.holes))
			return false;
		if (normalDirection == null) {
			if (other.normalDirection != null)
				return false;
		} else if (!normalDirection.equals(other.normalDirection))
			return false;
		if (normalMazeLength != other.normalMazeLength)
			return false;
		if (!Arrays.deepEquals(perfs, other.perfs))
			return false;
		if (width != other.width)
			return false;
		return true;
	}
	@Override public String toString() {
		return String.format("SidePiece with %s normal direction [%s]", normalDirection, Integer.toHexString(hashCode()));
	}
	
}
