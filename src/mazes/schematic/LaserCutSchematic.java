/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import mazes.util.VectorUtil;
import util.Pair;
import util.StreamUtil;

public final class LaserCutSchematic implements Iterable<LaserCutPath> {
	
	private final Collection<LaserCutPath> paths;
	
	public LaserCutSchematic() {
		this.paths = new ArrayList<>();
	}
	
	public void addPath(LaserCutPath path) {
		paths.add(path);
	}
	public int getPathCount() {
		return paths.size();
	}
	public boolean contains(LaserCutPath path) {
		return paths.contains(path);
	}
	
	public void eliminateMidpoints() {
		paths.forEach(LaserCutPath::eliminateMidpoints);
	}
	public Blueprint getBlueprint(Piece piece, Colors colors) {
		Blueprint blueprint = new Blueprint();
		
		int[] rootTileLocation = piece.getSchematicGridCorner();
		int[] rootLocation = new int[] {
				Dimensions.tileToCell(rootTileLocation[0]),
				Dimensions.tileToCell(rootTileLocation[1]),
				Dimensions.tileToCell(rootTileLocation[2])
		};
		int[] xOffset = piece.getXDirection().getOffsets(3);
		int[] yOffset = piece.getYDirection().getOffsets(3);
		int[] zOffset = piece.getZDirection().getOffsets(3);
		for (LaserCutPath path : paths) {
			for (Pair<Coordinate, Coordinate> pair : StreamUtil.groupTwo(
					path,
					(p1, p2) -> new CoordinatePair(p1, p2),
					true)) {
				int[] frontLineStart = VectorUtil.sumVectors(rootLocation, VectorUtil.scalarMultiple(xOffset, pair.getFirst().x), VectorUtil.scalarMultiple(yOffset, pair.getFirst().y));
				int[] frontLineEnd = VectorUtil.sumVectors(rootLocation, VectorUtil.scalarMultiple(xOffset, pair.getSecond().x), VectorUtil.scalarMultiple(yOffset, pair.getSecond().y));
				int[] backLineStart = VectorUtil.sumVectors(rootLocation, VectorUtil.scalarMultiple(xOffset, pair.getFirst().x), VectorUtil.scalarMultiple(yOffset, pair.getFirst().y), zOffset);
				int[] backLineEnd = VectorUtil.sumVectors(rootLocation, VectorUtil.scalarMultiple(xOffset, pair.getSecond().x), VectorUtil.scalarMultiple(yOffset, pair.getSecond().y), zOffset);
				blueprint.add(new XYZLine(frontLineStart, frontLineEnd, colors));
				blueprint.add(new XYZLine(backLineStart, backLineEnd, colors));
				blueprint.add(new XYZLine(frontLineStart, backLineStart, colors));
			}
		}
		return blueprint;
	}
	
	public Coordinate getMinimumCoordinate() {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		for (LaserCutPath path : this) {
			for (Coordinate point : path) {
				minX = Math.min(point.x, minX);
				minY = Math.min(point.y, minY);
			}
		}
		return new Coordinate(minX, minY);
	}
	public LaserCutSchematic moveBy(Coordinate offset) {
		LaserCutSchematic newThis = new LaserCutSchematic();
		for (LaserCutPath path : this) {
			LaserCutPath newPath = new LaserCutPath();
			for (Coordinate point : path) {
				newPath.addPoint(new Coordinate(point.x + offset.x, point.y + offset.y));
			}
			newThis.addPath(newPath);
		}
		return newThis;
	}
	
	// Iterable<LaserCutPath>
	@Override public Iterator<LaserCutPath> iterator() {
		return paths.iterator();
	}
	// Object
	@Override public String toString() {
		return paths.stream().map(LaserCutPath::toString).collect(Collectors.joining("\n"));
	}
	
}
