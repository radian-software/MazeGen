/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import mazes.util.Direction;

public final class LayerPiece implements Piece {
	
	private CellPerforations[][] perfs;
	private boolean[][] holes;
	public final int width, depth;
	public final int z;
	
	// width = x length // depth = y length
	public LayerPiece(int width, int depth, int z) {
		if (width <= 0 || depth <= 0) throw new IllegalArgumentException();
		this.width = width;
		this.depth = depth;
		
		perfs = new CellPerforations[width][depth];
		for (CellPerforations[] row : perfs) {
			Arrays.setAll(row, i -> new CellPerforations(Perforation.NONE));
		}
		holes = new boolean[width][depth];
		
		if (z < -1) throw new IllegalArgumentException();
		this.z = z;
	}
	
	public Perforation getPerforation(int x, int y, Direction side) {
		return perfs[x][y].getEdge(side);
	}
	public void setPerforation(int x, int y, Direction side, Perforation type) {
		perfs[x][y].setEdge(side, type);
		int newX = x + side.getOffset(0), newY = y + side.getOffset(1);
		if (!(newX < 0 || newX >= width || newY < 0 || newY >= depth)) {
			perfs[newX][newY].setEdge(side.getOpposite(), type);
		}
	}
	public boolean getHole(int x, int y) {
		return holes[x][y];
	}
	public void setHole(int x, int y, boolean isHole) {
		holes[x][y] = isHole;
	}
	
	public Blueprint getOutlinesBlueprint(Colors colors) {
		// First, we'll create TetrisWallPieces that include piece size and hole data; thus,
		// we may reuse the TetrisWallPiece getBlueprint() code for the outer outline of the LayerPiece.
		
		// Examples:
//		+=====+     +-+ +-+
//		‖  O. ‖     |.|.|.|
//		‖ +.+ ‖     | | +-+
//		‖ |O.O‖ ==> |.|. . 
//		‖ + + ‖     | +-+  
//		‖ |  O‖     |. .|. 
//		+=====+     +---+  
		
//		+=====+     +-+ +-+
//		‖ .O  ‖     |.|.|.|
//		‖ +-+.‖     +-+-+-+
//		‖O| |O‖ ==>  .|.|.  (let's hope this never happens)
//		‖.+-+ ‖     +-+-+-+
//		‖  O. ‖     |.|.|.|
//		+=====+     +-+ +-+
		
//		+=====+     +-----+
//		‖     ‖     |. . .| To account for a ring shape like this, we will have to first
//		‖ + + ‖     | +-+ | find tetris pieces normally, then invert the walls and find
//		‖  O  ‖ ==> |.|.|.| new tetris pieces. Remember, all we need is the outline, so
//		‖ + + ‖     | +-+ | we don't have to worry about on which side of the outline the
//		‖     ‖     |. . .| actual piece lies. Finally, we need simply to eliminate duplicate lines.
//		+=====+     +-----+
		
		List<List<Coordinate>> resultingCoordLists = new ArrayList<>();
		List<Coordinate> currentCoordList = new ArrayList<>();
		for (boolean invert : new boolean[] {false, true}) {
			boolean[][] alreadyChecked = new boolean[width][depth]; // init'd to false
			for (int x=0; x<width; x++) {
				for (int y=0; y<depth; y++) {
					List<Coordinate> stack = new ArrayList<>(); // for flood fill
					stack.add(new Coordinate(x, y));
					int cx, cy; // convenience
					while (stack.size() > 0) {
						cx = stack.get(stack.size()-1).x; cy = stack.get(stack.size()-1).y;
						if (cx < 0 || cx >= width || cy < 0 || cy >= depth ||
								alreadyChecked[cx][cy] || holes[cx][cy] ^ invert) {
							stack.remove(stack.size()-1);
							continue;
						}
						
						// handle finding a valid space:
						alreadyChecked[cx][cy] = true;
						currentCoordList.add(stack.get(stack.size()-1));
						
						// consider neighboring spaces:
						stack.add(new Coordinate(cx-1, cy));
						stack.add(new Coordinate(cx+1, cy));
						stack.add(new Coordinate(cx, cy-1));
						stack.add(new Coordinate(cx, cy+1));
					}
					if (currentCoordList.size() > 0) {
						resultingCoordLists.add(currentCoordList);
						currentCoordList = new ArrayList<Coordinate>();
					}
				}
			}
		}
		LinkedHashSet<XYZLine> outlines = new LinkedHashSet<>(); // automagically remove duplicate lines
		for (List<Coordinate> coordList : resultingCoordLists) {
			TetrisWallPiece newPiece = new TetrisWallPiece(new int[] {0, 0, 0}, false,
					new CoordinateCollection(coordList));
			Blueprint tetrisBlueprint = newPiece.getHorizontalBlueprint(this.z, colors);
			for (int i=0; i<tetrisBlueprint.size(); i++) {
				tetrisBlueprint.set(i, new XYZLine(tetrisBlueprint.get(i), colors));
			}
			outlines.addAll(tetrisBlueprint);
		}
		return new Blueprint(outlines);
	}
	public Blueprint getPerforationsBlueprint(Colors perforationColors, Colors slotColors) {
		// Now we'll add differently colored lines for the perforations.
		Blueprint perfLines = new Blueprint();
		// Only use positive Directions -- we don't really want duplicates
		for (Direction d : new Direction[] {new Direction(0, true), new Direction(1, true)}) {
			for (int x=0; x < width - (d.getDimension() == 0 ? 1 : 0); x++) {
				for (int y=0; y < depth - (d.getDimension() == 1 ? 1 : 0); y++) {
					Perforation perf = perfs[x][y].getEdge(d);
					
					if (perf == Perforation.NONE) {
						continue;
					}
					Colors colors = perf == Perforation.SLOT ? slotColors : perforationColors;
					
					int startX = x - (d.getDimension() == 1 ? 1 : 0);
					int endX = x;
					int startY = y - (d.getDimension() == 0 ? 1 : 0);
					int endY = y;
					int startZ = z, endZ = z;
					
					perfLines.add(new XYZLine(startX, startY, startZ, endX, endY, endZ, colors));
				}
			}
		}
		return perfLines;
	}
	public Blueprint getBlueprint(Colors outlineColors, Colors perforationColors, Colors slotColors) {
		Blueprint outlines = getOutlinesBlueprint(outlineColors);
		Blueprint perfLines = getPerforationsBlueprint(perforationColors, slotColors);
		Blueprint allLines = outlines; // first (rendered underneath)
		allLines.addAll(perfLines); // last (rendered on top)
		return allLines;
	}
	private void oldHighlightPerforationsBlueprint(Blueprint perfLines, Blueprint outlines,
			Colors perforationColors, Colors slotColors,
			Colors outlineAndPerforationColors, Colors outlineAndSlotColors) {
		for (XYZLine outLine : outlines) {
			boolean alreadyPerf = false;
			for (XYZLine perfLine : perfLines) {
				if (perfLine.equals(outLine)) {
					if (!alreadyPerf) {
						alreadyPerf = true;
						if (perfLine.colors == perforationColors) {
							perfLines.set(perfLines.indexOf(perfLine), new XYZLine(perfLine, outlineAndPerforationColors));
						}
						else if (perfLine.colors == slotColors) {
							perfLines.set(perfLines.indexOf(perfLine), new XYZLine(perfLine, outlineAndSlotColors));
						}
						else {
							throw new AssertionError();
						}
					}
					else {
						throw new AssertionError("multiple perforations at a single location");
					}
				}
			}
		}
	}
	public Blueprint getOldErrorHighlightedPerforations(Colors perforationColors, Colors slotColors,
			Colors outlineAndPerforationColors, Colors outlineAndSlotColors) {
		Blueprint outlines = getOutlinesBlueprint(null);
		Blueprint perfLines = getPerforationsBlueprint(perforationColors, slotColors);
		oldHighlightPerforationsBlueprint(perfLines, outlines, perforationColors, slotColors, outlineAndPerforationColors, outlineAndSlotColors);
		return perfLines;
	}
	public Blueprint getOldErrorHighlightedBlueprint(Colors outlineColors, Colors perforationColors, Colors slotColors,
			Colors outlineAndPerforationColors, Colors outlineAndSlotColors) {
		Blueprint outlines = getOutlinesBlueprint(outlineColors);
		Blueprint perfLines = getPerforationsBlueprint(perforationColors, slotColors);
		oldHighlightPerforationsBlueprint(perfLines, outlines, perforationColors, slotColors, outlineAndPerforationColors, outlineAndSlotColors);
		Blueprint allLines = outlines;
		allLines.addAll(perfLines); // rendered last
		return allLines;
	}
	public void highlightPerforationsBlueprint(Blueprint perfLines, Colors colorsToHighlight,
			Colors edgeSuspensionColors, Colors totalSuspensionColors) {
		// Next line for testing only
//		perfLines.clear();
		for (Direction d : new Direction[] {new Direction(0, true), new Direction(1, true)}) {
			for (int x=0; x < width - (d.getDimension() == 0 ? 1 : 0); x++) {
				for (int y=0; y < depth - (d.getDimension() == 1 ? 1 : 0); y++) {
					int firstTileX = x, secondTileX = x + (d.getDimension() == 0 ? 1 : 0);
					int firstTileY = y, secondTileY = y + (d.getDimension() == 1 ? 1 : 0);
					if (firstTileX < 0 || secondTileX >= width || firstTileY < 0 || secondTileY >= depth) continue;
					boolean firstTileSolid = !holes[firstTileX][firstTileY],
							secondTileSolid = !holes[secondTileX][secondTileY];
					Colors colors;
					if (firstTileSolid ^ secondTileSolid) colors = edgeSuspensionColors;
					else if (firstTileSolid && secondTileSolid) continue;
					else colors = totalSuspensionColors;
					
					int startX = x - (d.getDimension() == 1 ? 1 : 0);
					int endX = x;
					int startY = y - (d.getDimension() == 0 ? 1 : 0);
					int endY = y;
					int startZ = z, endZ = z;
					
					XYZLine newPerfLine = new XYZLine(startX, startY, startZ, endX, endY, endZ, colors);
					// This can (and will) be calculated for every line, even ones that are not in perfLines.
					// It is just easier (I admit laziness) to go through every line possible and check if it
					// should replace one I already have.
					
					// Next line for testing only
//					perfLines.add(newPerfLine);
					for (XYZLine perfLine : perfLines) {
						if (newPerfLine.equals(perfLine)) { // except for the colors!
							if (perfLine.colors.equals(colorsToHighlight)) {
								perfLines.set(perfLines.indexOf(perfLine), newPerfLine);
							}
						}
					}
				}
			}
		}
	}
	public Blueprint getErrorHighlightedPerforations(Colors perforationColors, Colors slotColors,
			Colors edgeSuspensionColors, Colors totalSuspensionColors) {
		Blueprint perfLines = getPerforationsBlueprint(perforationColors, slotColors);
		highlightPerforationsBlueprint(perfLines, perforationColors, edgeSuspensionColors, totalSuspensionColors);
		return perfLines;
	}
	public Blueprint getErrorHighlightedBlueprint(Colors outlineColors, Colors perforationColors, Colors slotColors,
			Colors edgeSuspensionColors, Colors totalSuspensionColors) {
		Blueprint outlines = getOutlinesBlueprint(outlineColors);
		Blueprint perfLines = getPerforationsBlueprint(perforationColors, slotColors);
		highlightPerforationsBlueprint(perfLines, perforationColors, edgeSuspensionColors, totalSuspensionColors);
		Blueprint allLines = outlines;
		allLines.addAll(perfLines); // rendered last
		return allLines;
	}
	
	@Override public int[] getSchematicGridCorner() {
		return new int[] {0, 0, z+1};
	}
	@Override public Direction getXDirection() {
		return new Direction(0, true);
	}
	@Override public Direction getYDirection() {
		return new Direction(1, true);
	}
	@Override public Direction getZDirection() {
		return new Direction(2, true);
	}
	@Override public String getType() {
		return "LayerPiece";
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + depth;
		result = prime * result + Arrays.hashCode(holes);
		result = prime * result + Arrays.hashCode(perfs);
		result = prime * result + width;
		result = prime * result + z;
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerPiece other = (LayerPiece) obj;
		if (depth != other.depth)
			return false;
		if (!Arrays.deepEquals(holes, other.holes))
			return false;
		if (!Arrays.deepEquals(perfs, other.perfs))
			return false;
		if (width != other.width)
			return false;
		if (z != other.z)
			return false;
		return true;
	}
	@Override public String toString() {
//		width == 3; example:
//		LayerPiece at z = 1 [cafebabe]
//		+=====+
//		‖  O. ‖
//		‖ +.+ ‖
//		‖ |O^O‖ The ^ represents a place where I really hope a Perforation.PERF (.) is never
//		‖ + + ‖ generated, because it would be impossible to construct physically.
//		‖ |  O‖
//		+=====+
		// Addendum. Well, it turns out perforations certainly can and will be generated there.
		// That did complicate matters a bit.
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("LayerPiece at z = %d [%s]%n", z, Integer.toHexString(hashCode())));
		int swidth = width * 2 + 1;
		int sheight = depth * 2 + 1;
		for (int sy=sheight-1; sy>=0; sy--) {
			for (int sx=0; sx<swidth; sx++) {
				if ((sx == 0 || sx == swidth-1) && (sy == 0 || sy == sheight-1)) {
					sb.append("+"); // corners
				}
				else if (sy == 0 || sy == sheight-1) {
					sb.append("="); // top and bottom
				}
				else if (sx == 0 || sx == swidth-1) {
					sb.append("‖"); // left and right sides (nifty character -- U+01C1)
				}
				else if (sx % 2 == 0 && sy % 2 == 0) {
					sb.append("+"); // tile corners
				}
				// even though sx and sy are always positive, it is good practice to use sx % 2 != 0 instead
				// of sx % 2 == 1 in general because they work for negative numbers, while the former doesn't.
				else if (sx % 2 != 0 && sy % 2 != 0) {
					// holes at centers of tiles
					int x = (sx - 1) / 2;
					int y = (sy - 1) / 2;
					sb.append(holes[x][y] ? "O" : " ");
				}
				else if (sx % 2 == 0 && sy % 2 != 0) {
					// vertical tile edges
					int x = sx / 2;
					int y = (sy - 1) / 2;
					switch (perfs[x][y].getEdge(new Direction(0, false))) { // left
					case NONE: sb.append(" "); break;
					case PERF: sb.append("."); break;
					case SLOT: sb.append("|"); break;
					default: throw new AssertionError();
					}
				}
				else if (sx % 2 != 0 && sy % 2 == 0) {
					// vertical tile edges
					int x = (sx - 1) / 2;
					int y = sy / 2;
					switch (perfs[x][y].getEdge(new Direction(1, false))) { // down
					case NONE: sb.append(" "); break;
					case PERF: sb.append("."); break;
					case SLOT: sb.append("-"); break;
					default: throw new AssertionError();
					}
				}
				else {
					throw new AssertionError();
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
}
