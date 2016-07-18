package mazes.schematic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CoordinateCollection implements Iterable<Coordinate> {
	
	private List<Coordinate> coordList;
	
	public CoordinateCollection() {
		coordList = new ArrayList<Coordinate>();
	}
	public CoordinateCollection(Coordinate firstCoord) {
		this();
		addCoordinate(firstCoord);
	}
	public CoordinateCollection(List<Coordinate> coordList) {
		if (coordList == null) throw new NullPointerException();
		this.coordList = coordList;
		coordList.sort(null);
	}
	
	public boolean contains(Coordinate coord) {
		return coordList.contains(coord);
	}
	// Note: the following method will not check if the proposed
	// addition would cause this piece to intersect another
	// orthogonally.
	public boolean isValidPlacement(Coordinate coord) {
		// For the suggested coordinate:
		// To make it possible to assemble the tetris pieces with the layer pieces,
		// we are required to have NO overhangs.
		// Thus, the piece must EITHER be on the minimum Y level and be horizontally 
		// adjacent to another piece, OR directly above another piece.
		/* also, this would be stupid: */ if (coordList.contains(coord)) return false;
		boolean	existsDirectlyAdjacent = false;
		for (Coordinate other : coordList) {
			if (other.isDirectlyBelow(coord)) return true; // short-circuit
			else if (other.isDirectlyAdjacentTo(coord)) existsDirectlyAdjacent = true;
		}
		return existsDirectlyAdjacent && coord.y == getMinimumYCoordinate();
	}
	// Note: the following method will not check if the piece
	// is contiguous.
	public boolean isValid() {
		// The only way we can invalidate the piece is if it has a tile
		// that is not on the lowest level, AND does not have a tile
		// directly below it.
		int lowestLevel = getMinimumYCoordinate();
		for (Coordinate coord : coordList) {
			if (coord.y == lowestLevel) continue; // ok
			for (Coordinate other : coordList) {
				if (other.isDirectlyBelow(coord)) continue; // ok
			}
			return false;
		}
		return true;
	}
	public void addCoordinate(Coordinate coord) {
		coordList.add(coord);
		coordList.sort(null);
	}
	public void popCoordinate() {
		coordList.remove(coordList.size()-1);
	}
	public CoordinateCollection[] splitGreaterThan(int localX) {
		List<Coordinate> lessThanOrEqualTo = new ArrayList<>(),
				greaterThan = new ArrayList<>();
		for (Coordinate coord : coordList) {
			if (coord.x <= localX) lessThanOrEqualTo.add(coord);
			else greaterThan.add(coord);
		}
		return new CoordinateCollection[] {
				new CoordinateCollection(lessThanOrEqualTo),
				new CoordinateCollection(greaterThan)
		};
	}
	public Coordinate adjustCenter() {
		Coordinate firstCoord = coordList.get(0);
		// Subtract firstCoord from each of the coordinates. Of course, this will make the coordinate at
		// firstCoord's location in the list equal new Coordinate(0, 0).
		for (int i=0; i<coordList.size(); i++) {
			coordList.set(i, new Coordinate(coordList.get(i).x - firstCoord.x, coordList.get(i).y - firstCoord.y));
		}
		return firstCoord;
	}
	
	public int getMinimumXCoordinate() {
		int minX = Integer.MAX_VALUE;
		for (Coordinate coord : coordList) {
			if (coord.x < minX) minX = coord.x;
		}
		return minX;
	}
	public int getMaximumXCoordinate() {
		int maxX = Integer.MIN_VALUE;
		for (Coordinate coord : coordList) {
			if (coord.x > maxX) maxX = coord.x;
		}
		return maxX;
	}
	public int getMinimumYCoordinate() {
		return coordList.get(0).y; // because Coordinates order by y-coordinate
	}
	public int getMaximumYCoordinate() {
		return coordList.get(coordList.size()-1).y;
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coordList == null) ? 0 : coordList.hashCode());
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CoordinateCollection other = (CoordinateCollection) obj;
		if (coordList == null) {
			if (other.coordList != null)
				return false;
		} else if (!coordList.equals(other.coordList))
			return false;
		return true;
	}
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y=getMaximumYCoordinate(); y>=getMinimumYCoordinate(); y--) {
			for (int x=getMinimumXCoordinate(); x<=getMaximumXCoordinate(); x++) {
				sb.append(coordList.contains(new Coordinate(x, y)) ? (x == 0 && y == 0 ? "×" : "■") : " ");
			}
			sb.append("\n");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	@Override public Iterator<Coordinate> iterator() {
		return coordList.iterator();
	}
	
}
