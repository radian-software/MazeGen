package mazes.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import mazes.util.Direction;

public final class TetrisWallPiece implements Piece, Iterable<Coordinate> {
	
	private int[] rootOffset;
	private CoordinateCollection shape;
	// parallel to (isYZ ? yz-plane : xz-plane)
	// normal to (isYZ ? x direction : y direction)
	// indexed by axes (isYZ ? Y and Z : X and Z)
	private final boolean isYZ;
	public boolean isYZ() { return isYZ; }
	public boolean isXZ() { return !isYZ; }
	// isYZ ? 1 : 0 gives parallel horizontal dimension
	// isXZ ? 1 : 0 gives normal horizontal dimension
	
	public TetrisWallPiece(int[] rootLocation, boolean isYZ) {
		if (rootLocation == null) throw new NullPointerException();
		if (rootLocation.length != 3) throw new IllegalArgumentException();
		this.rootOffset = rootLocation;
		this.isYZ = isYZ;
		this.shape = new CoordinateCollection();
	}
	public TetrisWallPiece(int[] rootLocation, boolean isYZ, CoordinateCollection coords) {
		this(rootLocation, isYZ);
		if (coords == null) throw new NullPointerException();
		this.shape = coords;
	}
	
	public boolean contains(Coordinate coord) {
		return shape.contains(coord);
	}
	public boolean contains(int[] location) {
//		System.out.println("Checking if " + (isYZ ? "YZ" : "XZ") + " piece contains " + Arrays.toString(location) + " ==> " + locationToCoordinate(location));
//		System.out.println("\t\t(" + (contains(locationToCoordinate(location)) ? "yes" : "no") + ")");
		return contains(locationToCoordinate(location));
	}
	public boolean isValidPlacement(Coordinate coord) {
		return shape.isValidPlacement(coord);
	}
	public boolean isValidPlacement(int[] location) {
		return isValidPlacement(locationToCoordinate(location));
	}
	public boolean isValid() {
		return shape.isValid();
	}
	public void addCoordinate(Coordinate coord) {
		shape.addCoordinate(coord);
	}
	public void addCoordinate(int[] location) {
		shape.addCoordinate(locationToCoordinate(location));
	}
	public void popCoordinate() {
		shape.popCoordinate();
	}
	
	// The following two methods return global positions, not
	// local coordinates.
	// Note that the Y coordinate of a Coordinate corresponds
	// to the Z coordinate in the global position space.
	public int getMinimumZCoordinate() {
		return rootOffset[2] + shape.getMinimumYCoordinate();
	}
	public int getMaximumZCoordinate() {
		return rootOffset[2] + shape.getMaximumYCoordinate();
	}
	public int getMinimumXYCoordinate() {
		return rootOffset[isYZ ? 1 : 0] + shape.getMinimumXCoordinate();
	}
	public int getMaximumXYCoordinate() {
		return rootOffset[isYZ ? 1 : 0] + shape.getMaximumXCoordinate();
	}
	// So I added these methods if ya want the local coordinates:
	public int getMinimumYCoordinate() {
		return shape.getMinimumYCoordinate();
	}
	public int getMaximumYCoordinate() {
		return shape.getMaximumYCoordinate();
	}
	public int getMinimumXCoordinate() {
		return shape.getMinimumXCoordinate();
	}
	public int getMaximumXCoordinate() {
		return shape.getMaximumXCoordinate();
	}
	
	public boolean intersectsOrthogonally(TetrisWallPiece other) {
		if (isYZ == other.isYZ) return false;
		// So we can deal with two concrete sets of dimensions:
		TetrisWallPiece xzPiece = isYZ ? other : this,
				yzPiece = isYZ ? this : other;
		
		int yCoordinateOfXZPiece = xzPiece.rootOffset[1]; // the y coordinate is invariant
														  // under translation
		int xCoordinateOfYZPiece = yzPiece.rootOffset[0]; // likewise.
		// Identifying the X and Y intersection coordinates gives us a vertical
		// line, which is equivalent to two adjacent columns of wall tiles on
		// each plane. (The line has two columns coincident with it in each of
		// the two (xz- and yz-plane) directions.)
		int lowerYCoordinateOnYZPiece = yCoordinateOfXZPiece,
				upperYCoordinateOnYZPiece = yCoordinateOfXZPiece + 1;
		int lowerXCoordinateOnXZPiece = xCoordinateOfYZPiece,
				upperXCoordinateOnXZPiece = xCoordinateOfYZPiece + 1;
		// Now we may iterate in the +z direction up the columns and check if
		// there is an intersection (i.e. there are wall tiles at all four of
		// the coordinates corresponding to the four variables just declared).
		// z wall indices are inclusive --
//		System.out.println("Y coordinate of XZ piece = " + yCoordinateOfXZPiece);
//		System.out.println("X coordinate of YZ piece = " + xCoordinateOfYZPiece);
//		System.out.println("YZ tile coordinates are " + lowerYCoordinateOnYZPiece + " and " + upperYCoordinateOnYZPiece);
//		System.out.println("XZ tile coordinates are " + lowerXCoordinateOnXZPiece + " and " + upperXCoordinateOnXZPiece);
		
		int minimumZCoordinate = Math.min(xzPiece.getMinimumZCoordinate(), yzPiece.getMinimumZCoordinate());
		int maximumZCoordinate = Math.max(xzPiece.getMaximumZCoordinate(), yzPiece.getMaximumZCoordinate()); // cached to eliminate profiled bottleneck
		for (int z = minimumZCoordinate; z <= maximumZCoordinate; z++) {
//			System.out.println("Iterating for z = " + z);
			// The y coordinate does not matter because it will be discarded
			// when the global position is transformed to a local Coordinate object.
			// In other words, we need only specify the X and Z coordinates
			// when indexing an XZ piece.
			if (xzPiece.contains(new int[] {lowerXCoordinateOnXZPiece, -1, z}) &&
					xzPiece.contains(new int[] {upperXCoordinateOnXZPiece, -1, z}) &&
					yzPiece.contains(new int[] {-1, lowerYCoordinateOnYZPiece, z}) &&
					yzPiece.contains(new int[] {-1, upperYCoordinateOnYZPiece, z})) {
				return true;
			}
		}
		return false;
	}
	public TetrisWallPiece[] splitAlongPlane(TetrisWallPiece splitter) {
		// We want an XZ piece to split a YZ piece. Let's make sure:
		if (splitter.isYZ /* orentiation == true implies a YZ piece */) {
			throw new IllegalArgumentException();
		}
		if (!isYZ /* !orentiation implies an XZ piece */) {
			throw new AssertionError();
		}
		
		TetrisWallPiece xzPiece = splitter, yzPiece = this;
		// The split location is transformed from one coordinate system into
		// another. Since we only need the y coordinate, we need only do a
		// transformation from a Coordinate that is y-invariant and then discard
		// the x and z coordinates.
		int ySplitLocalCoordinate = yzPiece.locationToCoordinate(xzPiece.rootOffset).x;
		// (Also, Coordinate.x corresponds to y for a YZ piece, remember.)
		
		// Actually split piece --
		CoordinateCollection[] newCoordinateCollections = yzPiece.shape.splitGreaterThan(ySplitLocalCoordinate);
		Coordinate[] adjustmentOffsets = new Coordinate[2];
		adjustmentOffsets[0] = newCoordinateCollections[0].adjustCenter();
		adjustmentOffsets[1] = newCoordinateCollections[1].adjustCenter();
		return new TetrisWallPiece[] {
				// yzPiece.isYZ will always be the same, but this is more intuitive.
				new TetrisWallPiece(new int[] {yzPiece.rootOffset[0], yzPiece.rootOffset[1] + adjustmentOffsets[0].x, yzPiece.rootOffset[2] + adjustmentOffsets[0].y}, yzPiece.isYZ, newCoordinateCollections[0]),
				new TetrisWallPiece(new int[] {yzPiece.rootOffset[0], yzPiece.rootOffset[1] + adjustmentOffsets[1].x, yzPiece.rootOffset[2] + adjustmentOffsets[1].y}, yzPiece.isYZ, newCoordinateCollections[1])
		};
	}
	public TetrisWallPiece combineWith(TetrisWallPiece other) {
		if (this.isYZ != other.isYZ) throw new IllegalArgumentException();
		TetrisWallPiece newPiece = new TetrisWallPiece(this.rootOffset, isYZ);
		for (Coordinate coord : this) {
			newPiece.addCoordinate(coord);
		}
		for (Coordinate coord : other) {
			// Transform between the coordinate systems
			newPiece.addCoordinate(this.locationToCoordinate(other.coordinateToLocation(coord)));
		}
		return newPiece;
	}
	
	public int[] coordinateToLocation(Coordinate coord) {
		int[] location = Arrays.copyOf(rootOffset, 3);
		location[isYZ ? 1 : 0] += coord.x;
		location[2] += coord.y;
		return location;
	}
	public Coordinate locationToCoordinate(int[] location) {
		return new Coordinate(location[isYZ ? 1 : 0] - rootOffset[isYZ ? 1 : 0], location[2] - rootOffset[2]);
	}
	
	public int getNormalOffset() {
		return rootOffset[isYZ ? 0 : 1];
	}
	
	public int getHorizontalNormalDimension() {
		return isYZ ? 0 : 1;
	}
	public int getHorizontalParallelDimension() {
		return isYZ ? 1 : 0;
	}
	public int getVerticalDimension() {
		return 2;
	}
	
	public Blueprint getBlueprint(Colors colors) {
//		System.out.println("Generating blueprint for following piece:");
//		System.out.println(this);
		// Mosey around the outside edge of the piece.
		// In general, 0 should denote the positive edge of the first tile. Thus,
		// -1 will denote the negative edge of the first tile and we must use -2
		// to signify an invalid coordinate.
		int firstX = -2, firstY = -2;
		findFirstCoordinateLoop: {
			for (int y=shape.getMinimumYCoordinate(); y<=shape.getMaximumYCoordinate(); y++) {
				for (int x=shape.getMinimumXCoordinate(); x<=shape.getMaximumXCoordinate(); x++) {
					if (shape.contains(new Coordinate(x, y))) {
//						System.out.println("Starting iteration at (" + x + ", " + y + ")");
						firstX = x - 1; // pick upper-left edge (looking, *temporarily*, at the
						firstY = y - 1; // shape as placed on a traditional vertical XY plane)
						break findFirstCoordinateLoop;
						// We have ensured we find an upper-left corner of the piece.
						// Also, we shall always go around clockwise. This simplifies things.
					}
				}
			}
		}
		if (firstX == -2 || firstY == -2) return new Blueprint(); // apparently there is no piece
		List<Integer> xList = new ArrayList<>(), yList = new ArrayList<>();
		int currentX = firstX, currentY = firstY;
		byte lastDirection = 3;
		// 1 = left
		// 2 = right
		// 3 = up
		// 4 = down
		// normally, I would frown upon something like this, but I have no convenient data structure or enum available.
		do { // while (currentX != firstX || currentY != firstY)
			xList.add(currentX); yList.add(currentY);
//			System.out.println("Adding coordinate (" + currentX + ", " + currentY + ")");
//			+--+--+ we are at the X (x, y).
//			|c | d| piece a is at (x, y)
//			+--X--+ piece b is at (x+1, y)
//			|a | b| piece c is at (x, y+1)
//			+--+--+ piece d is at (x+1, y+1)
			boolean a = shape.contains(new Coordinate(currentX, currentY)),
					b = shape.contains(new Coordinate(currentX+1, currentY)),
					c = shape.contains(new Coordinate(currentX, currentY+1)),
					d = shape.contains(new Coordinate(currentX+1, currentY+1));
//			System.out.println("abcd = " + Arrays.toString(new boolean[] {a, b, c, d}));
			// Determine new direction
			switch (lastDirection) {
			case 1: // left
				if (!c) {
					lastDirection = 3; // up
					break;
				}
				if (!a) {
					lastDirection = 1; // left
					break;
				}
				lastDirection = 4; // down
				break;
			case 2: // right
				if (!b) {
					lastDirection = 4; // down
					break;
				}
				if (!d) {
					lastDirection = 2; // right
					break;
				}
				lastDirection = 3; // up
				break;
			case 3: // up
				if (!d) {
					lastDirection = 2; // right
					break;
				}
				if (!c) {
					lastDirection = 3; // up
					break;
				}
				lastDirection = 1; // left
				break;
			case 4: // down
				if (!a) {
					lastDirection = 1; // left
					break;
				}
				if (!b) {
					lastDirection = 4; // down
					break;
				}
				lastDirection = 2; // right
				break;
			default: throw new AssertionError();
			}
			// Move in the determined direction
			switch (lastDirection) {
			case 1: currentX -= 1; break; // left
			case 2: currentX += 1; break; // right
			case 3: currentY += 1; break; // up
			case 4: currentY -= 1; break; // down
			default: throw new AssertionError();
			}
		}
		while (currentX != firstX || currentY != firstY);
		// We now have a list of local XY Coordinates that (hopefully) formed a loop.
		Blueprint lines = new Blueprint();
		for (int i=0; i<xList.size(); i++) {
			int firstIndex = i, secondIndex = (i + 1) % xList.size(); // wrap around
			lines.add(new XYZLine(
					coordinateToLocation(new Coordinate(
							xList.get(firstIndex),
							yList.get(firstIndex)
					)),
					coordinateToLocation(new Coordinate(
							xList.get(secondIndex),
							yList.get(secondIndex)
					)),
					colors
			));
		}
		return lines;
	}
	public Blueprint getHorizontalBlueprint(int z, Colors colors) {
		if (isYZ) throw new AssertionError(); // Doesn't make a lot of sense to horizontalize a YZ piece. For me, anyway.
		Blueprint originalBlueprint = getBlueprint(colors);
		Blueprint newBlueprint = new Blueprint();
		for (XYZLine originalLine : originalBlueprint) {
			XYZLine newLine = new XYZLine(originalLine.x1, originalLine.z1, z,
					originalLine.x2, originalLine.z2, z,
					originalLine.colors
					);
			newBlueprint.add(newLine);
		}
		return newBlueprint;
	}
	
	@Override public int[] getSchematicGridCorner() {
		int[] loc = coordinateToLocation(new Coordinate(getMinimumXCoordinate(), getMinimumYCoordinate()));
		return new int[] {isYZ ? loc[0] + 1 : loc[0], isYZ ? loc[1] : loc[1] + 1, loc[2]};
	}
	@Override public Direction getXDirection() {
		return isYZ ? new Direction(1, true) : new Direction(0, true);
	}
	@Override public Direction getYDirection() {
		return new Direction(2, true);
	}
	@Override public Direction getZDirection() {
		return isYZ ? new Direction(0, true) : new Direction(1, true);
	}
	@Override public String getType() {
		return "TetrisWallPiece";
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isYZ ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(rootOffset);
		result = prime * result + ((shape == null) ? 0 : shape.hashCode());
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TetrisWallPiece other = (TetrisWallPiece) obj;
		if (isYZ != other.isYZ)
			return false;
		if (!Arrays.equals(rootOffset, other.rootOffset))
			return false;
		if (shape == null) {
			if (other.shape != null)
				return false;
		} else if (!shape.equals(other.shape))
			return false;
		return true;
	}
	@Override public String toString() {
		return String.format("TetrisWallPiece on %s plane [%s]%nRoot offset is %s%n%n%s%n", isYZ ? "YZ" : "XZ", Integer.toHexString(hashCode()), Arrays.toString(rootOffset), shape);
	}
	@Override public Iterator<Coordinate> iterator() {
		return shape.iterator();
	}
	
}
