package mazes.schematic;

import java.util.Arrays;

import mazes.util.Direction;

public final class CellPerforations {
	
	private final Perforation[] edges;
	
	public CellPerforations(Perforation initialValue) {
		if (initialValue == null) throw new NullPointerException();
		this.edges = new Perforation[4];
		Arrays.fill(edges, initialValue);
	}
	
	public Perforation getEdge(Direction side) {
		if (side == null) throw new NullPointerException();
		return edges[side.getIndex()];
	}
	public void setEdge(Direction side, Perforation type) {
		if (side == null) throw new NullPointerException();
		edges[side.getIndex()] = type;
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(edges);
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellPerforations other = (CellPerforations) obj;
		if (!Arrays.equals(edges, other.edges))
			return false;
		return true;
	}
	@Override public String toString() {
		return Arrays.toString(edges).replace(" ", "");
	}
	
}
