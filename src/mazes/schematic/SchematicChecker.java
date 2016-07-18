package mazes.schematic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import mazes.gen.CellWalls;
import mazes.util.Direction;
import mazes.util.MultiDimensionalArray;
import mazes.util.VectorUtil;

public final class SchematicChecker {
	
	private SchematicChecker() {}
	
	private static final class LexicographicComparator<T extends Comparable<T>> implements Comparator<List<T>> {
		@Override public int compare(List<T> first, List<T> second) {
			if (first.size() != second.size()) throw new ArrayIndexOutOfBoundsException();
			for (int i=0; i<first.size(); i++) {
				int cmp = first.get(i).compareTo(second.get(i));
				if (cmp != 0) return cmp;
			}
			return 0;
		}
	}
	
	public static void checkSchematics(MultiDimensionalArray<CellWalls> maze, SchematicSet schematicSet, boolean doOutput) throws SchematicException {
		checkSchematics(maze, schematicSet, doOutput, false);
	}
	public static void checkSchematics(MultiDimensionalArray<CellWalls> maze, SchematicSet schematicSet, boolean doOutput, boolean overrideExceptions) throws SchematicException {
		if (doOutput) System.out.println("Checking schematics...");
		if (doOutput) System.out.print("Fixing islanded tiles... ");
		checkForIslandedTiles(schematicSet, maze.getSideLengths(), true);
		if (doOutput) System.out.print("Done.\nVerifying fixes for islanded tiles... ");
		checkForIslandedTiles(schematicSet, maze.getSideLengths(), false);
		if (doOutput) System.out.print("Done.\nChecking for islanded layer piece sections... ");
		checkForIslandedLayerPieceSections(schematicSet, overrideExceptions);
		if (doOutput) System.out.print("Done.\nChecking adherence of generated pieces to maze plan... ");
		checkAdherenceToMazePlan(schematicSet, maze);
		if (doOutput) System.out.println("Done.");
	}
	private static void checkForIslandedTiles(SchematicSet schematicSet, int[] mazeSize, boolean fixProblems) {
		for (int t=0; t<3; t++) {
			List<Piece> pieces;
			List<SchematicCellGrid> schematics;
			switch (t) {
			case 0:
				pieces = new ArrayList<Piece>(schematicSet.tetrisPieces);
				schematics = schematicSet.tetrisSchematics;
				break;
			case 1:
				pieces = new ArrayList<Piece>(schematicSet.layerPieces);
				schematics = schematicSet.layerSchematics;
				break;
			case 2:
				pieces = new ArrayList<Piece>(schematicSet.sidePieces);
				schematics = schematicSet.sideSchematics;
				break;
			default: throw new AssertionError();
			}
			for (int i=0; i<pieces.size(); i++) {
				Piece piece = pieces.get(i);
				SchematicCellGrid schematic = schematics.get(i);
				for (int x=0; x<schematic.width; x++) {
					for (int y=0; y<schematic.height; y++) {
						if (schematic.getCell(x, y) == SchematicCell.PERF) throw new UnsupportedOperationException();
						if (schematic.getCell(x, y) == SchematicCell.HOLE) continue;
						boolean valid = false;
						for (Direction d : Direction.getDirections(2)) {
							int nx = x + d.getOffset(0);
							int ny = y + d.getOffset(1);
							try {
								if (schematic.getCell(nx, ny) == SchematicCell.SOLID) {
									valid = true;
									break;
								}
							}
							catch (ArrayIndexOutOfBoundsException e) {}
						}
						if (!valid) {
							if (fixProblems) {
								fixIslandedTile(piece, x, y, schematicSet, mazeSize);
							}
							else throw new AssertionError("Islanded tile could not be fixed");
						}
					}
				}
			}
		}
	}
	private static boolean fixIslandedTile(Piece fromPiece, int x, int y, SchematicSet schematicSet, int[] mazeSize) {
		if (fromPiece instanceof TetrisWallPiece) throw new AssertionError("islanded cell on tetris piece");
		
		// Some useful reference information --
		SidePiece leftSidePiece = null, rightSidePiece = null,
				frontSidePiece = null, backSidePiece = null;
		for (SidePiece sidePiece : schematicSet.sidePieces) {
			if (sidePiece.normalDirection.equals(new Direction(0, false))) leftSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(0, true))) rightSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(1, false))) frontSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(1, true))) backSidePiece = sidePiece;
		}
		if (leftSidePiece == null || rightSidePiece == null || frontSidePiece == null || backSidePiece == null)
			throw new AssertionError();
		LayerPiece topLayerPiece = null, bottomLayerPiece = null;
		for (LayerPiece layerPiece : schematicSet.layerPieces) {
			if (layerPiece.z == -1) bottomLayerPiece = layerPiece;
			else if (layerPiece.z == mazeSize[2] - 1) topLayerPiece = layerPiece;
		}
		if (topLayerPiece == null || bottomLayerPiece == null)
			throw new AssertionError();
		
		// Transform coordinate of problem cell to location --
		int[] rootTileLocation = fromPiece.getSchematicGridCorner();
		int[] rootLocation = new int[] {
				Dimensions.tileToCell(rootTileLocation[0]),
				Dimensions.tileToCell(rootTileLocation[1]),
				Dimensions.tileToCell(rootTileLocation[2])
		};
		int[] xOffset = fromPiece.getXDirection().getOffsets(3);
		int[] yOffset = fromPiece.getYDirection().getOffsets(3);
		
		int[] loc = VectorUtil.sumVectors(rootLocation, VectorUtil.scalarMultiple(xOffset, x), VectorUtil.scalarMultiple(yOffset, y));
		
		// Decide which pieces this cell intersects.
		Collection<Piece> intersectingPieces = new HashSet<>();
		if (loc[0] == 0) intersectingPieces.add(leftSidePiece);
		if (loc[0] == Dimensions.tileToCell(mazeSize[0])) intersectingPieces.add(rightSidePiece);
		if (loc[1] == 0) intersectingPieces.add(frontSidePiece);
		if (loc[1] == Dimensions.tileToCell(mazeSize[1])) intersectingPieces.add(backSidePiece);
		if (loc[2] == 0) intersectingPieces.add(bottomLayerPiece);
		if (loc[2] == Dimensions.tileToCell(mazeSize[2])) intersectingPieces.add(topLayerPiece);
		if (intersectingPieces.size() != 2 && intersectingPieces.size() != 3) throw new AssertionError();
		
		// Which pieces are we NOT allowed to pick?
		intersectingPieces.remove(fromPiece);
		
		if (loc[0] == 0 && loc[1] == 0 && loc[2] == 0)
			intersectingPieces.remove(frontSidePiece);
		if (loc[0] == 0 && loc[1] == Dimensions.tileToCell(mazeSize[1]) && loc[2] == 0)
			intersectingPieces.remove(backSidePiece);
		
		if (loc[0] == Dimensions.tileToCell(mazeSize[0]) && loc[1] == 0 && loc[2] == 0)
			intersectingPieces.remove(bottomLayerPiece);
		if (loc[0] == Dimensions.tileToCell(mazeSize[0]) && loc[1] == 0 && loc[2] == Dimensions.tileToCell(mazeSize[2]))
			intersectingPieces.remove(topLayerPiece);
		
		if (loc[0] == 0 && loc[1] == Dimensions.tileToCell(mazeSize[1]) && loc[2] == Dimensions.tileToCell(mazeSize[2]))
			intersectingPieces.remove(leftSidePiece);
		if (loc[0] == Dimensions.tileToCell(mazeSize[0]) && loc[1] == 0 && loc[1] == Dimensions.tileToCell(mazeSize[1]) && loc[2] == Dimensions.tileToCell(mazeSize[2]))
			intersectingPieces.remove(rightSidePiece);
		
		// Identify the piece we are removing a tile from, and the piece we are adding a tile to.
		Piece toPiece = intersectingPieces.iterator().next();
		SchematicCellGrid fromSchematic = getCorrespondingSchematic(fromPiece, schematicSet),
				toSchematic = getCorrespondingSchematic(toPiece, schematicSet);
		
		// Remove tile.
		fromSchematic.setCell(x, y, SchematicCell.HOLE);
		
		// Transform location of problem cell to (new) coordinate --
		// rootLocation + x * xOffset + y * yOffset = loc
		// newRootLocation + newX * newXOffset + newY * newYOffset = loc
		int[] newRootTileLocation = toPiece.getSchematicGridCorner();
		int[] newRootLocation = new int[] {
				Dimensions.tileToCell(newRootTileLocation[0]),
				Dimensions.tileToCell(newRootTileLocation[1]),
				Dimensions.tileToCell(newRootTileLocation[2])
		};
		int[] combinedOffset = VectorUtil.subtractVectors(loc, newRootLocation);
		int[] newXOffset = toPiece.getXDirection().getOffsets(3);
		int[] newYOffset = toPiece.getYDirection().getOffsets(3);
		// Solve a system of two linear equations:
		int[] a, b, c;
		if (newXOffset[0] == 0 && newYOffset[0] == 0) {
			a = new int[] {newXOffset[1], newXOffset[2]};
			b = new int[] {newYOffset[1], newYOffset[2]};
			c = new int[] {combinedOffset[1], combinedOffset[2]};
		}
		else if (newXOffset[1] == 0 && newYOffset[1] == 0) {
			a = new int[] {newXOffset[0], newXOffset[2]};
			b = new int[] {newYOffset[0], newYOffset[2]};
			c = new int[] {combinedOffset[0], combinedOffset[2]};
		}
		else if (newXOffset[2] == 0 && newYOffset[2] == 0) {
			a = new int[] {newXOffset[0], newXOffset[1]};
			b = new int[] {newYOffset[0], newYOffset[1]};
			c = new int[] {combinedOffset[0], combinedOffset[1]};
		}
		else throw new AssertionError();
		int D = a[0] * b[1] - a[1] * b[0];
		int Da = c[0] * b[1] - c[1] * b[0];
		int Db = a[0] * c[1] - a[1] * c[0];
		int newX = Da / D, newY = Db / D;
		
		// Add tile.
		toSchematic.setCell(newX, newY, SchematicCell.SOLID);
		
		return true;
	}
	private static SchematicCellGrid getCorrespondingSchematic(Piece piece, SchematicSet schematicSet) {
		if (piece instanceof TetrisWallPiece)
			return schematicSet.tetrisSchematics.get(schematicSet.tetrisPieces.indexOf(piece));
		if (piece instanceof LayerPiece)
			return schematicSet.layerSchematics.get(schematicSet.layerPieces.indexOf(piece));
		if (piece instanceof SidePiece)
			return schematicSet.sideSchematics.get(schematicSet.sidePieces.indexOf(piece));
		throw new AssertionError();
	}
	private static void checkForIslandedLayerPieceSections(SchematicSet schematicSet, boolean overrideExceptions) throws SchematicException {
		for (SchematicCellGrid schematic : schematicSet.layerSchematics) {
			boolean[][] visited = new boolean[schematic.width][schematic.height]; // init to false
			for (int X=0; X<schematic.width; X++) {
				for (int Y=0; Y<schematic.height; Y++) {
					if (X != 0 && Y != 0 && X != schematic.width - 1 && Y != schematic.height - 1) continue;
					if (!visited[X][Y] && schematic.getCell(X, Y) == SchematicCell.SOLID) {
						Deque<Coordinate> stack = new ArrayDeque<>();
						stack.addLast(new Coordinate(X, Y));
						while (stack.size() > 0) {
							Coordinate coord = stack.removeLast();
							int x = coord.x, y = coord.y;
							if (x < 0 || x >= schematic.width || y < 0 || y >= schematic.height) continue;
							if (!visited[x][y] && schematic.getCell(x, y) == SchematicCell.SOLID) {
								visited[x][y] = true;
								for (Direction d : Direction.getDirections(2)) {
									int newX = coord.x + d.getOffset(0), newY = coord.y + d.getOffset(1);
									stack.add(new Coordinate(newX, newY));
								}
							}
						}
					}
				}
			}
			for (int x=0; x<schematic.width; x++) {
				for (int y=0; y<schematic.height; y++) {
					if (!visited[x][y] && schematic.getCell(x, y) == SchematicCell.SOLID) {
						if (!overrideExceptions) throw new SchematicException("islanded layer piece section", true);
					}
				}
			}
		}
	}
	private static void checkAdherenceToMazePlan(SchematicSet schematicSet, MultiDimensionalArray<CellWalls> maze) {
		int tileSize = Dimensions.tileSize();
		// Get a set (because there WILL be intersections) of all cells that should be occupied
		TreeSet<List<Integer>> mazeLocs = new TreeSet<>(new LexicographicComparator<>());
		for (Direction d : Direction.getDirections(3)) {
			for (int x = 0; x < maze.getSideLength(0); x++) {
				for (int y = 0; y < maze.getSideLength(1); y++) {
					for (int z = 0; z < maze.getSideLength(2); z++) {
						if (maze.get(new int[] {x, y, z}).getWall(d)) {
//							System.out.println("Adding wall at " + Arrays.asList(x, y, z) + " in " + d + " direction.");
							for (int fy=0; fy<tileSize+2; fy++) {
								for (int fx=0; fx<tileSize+2; fx++) {
									int xOffset, yOffset, zOffset;
									try {
										xOffset =
												d.getDimension() == 0 ? 0 :
												d.getDimension() == 1 ? fx :
												d.getDimension() == 2 ? fx :
													0/0;
										yOffset = 
												d.getDimension() == 0 ? fx :
												d.getDimension() == 1 ? 0 :
												d.getDimension() == 2 ? fy :
													0/0;
										zOffset =
												d.getDimension() == 0 ? fy :
												d.getDimension() == 1 ? fy :
												d.getDimension() == 2 ? 0 :
													0/0;
									}
									catch (ArithmeticException e) {
										throw new AssertionError();
									}
									if (d.isPositive()) {
										if (d.getDimension() == 0) xOffset += tileSize + 1;
										if (d.getDimension() == 1) yOffset += tileSize + 1;
										if (d.getDimension() == 2) zOffset += tileSize + 1;
									}
//									System.out.println(Arrays.asList(x * (tileSize + 1) + xOffset, y * (tileSize + 1) + yOffset, z * (tileSize + 1) + zOffset));
									mazeLocs.add(Arrays.asList(x * (tileSize + 1) + xOffset, y * (tileSize + 1) + yOffset, z * (tileSize + 1) + zOffset));
								}
							}
						}
					}
				}
			}
		}
		// Get a list of all cells that are actually occupied
		List<List<Integer>> pieceLocs = new ArrayList<>();
		for (int t=0; t<3; t++) {
			List<Piece> pieces;
			List<SchematicCellGrid> schematics;
			switch (t) {
			case 0:
				pieces = new ArrayList<Piece>(schematicSet.tetrisPieces);
				schematics = schematicSet.tetrisSchematics;
				break;
			case 1:
				pieces = new ArrayList<Piece>(schematicSet.layerPieces);
				schematics = schematicSet.layerSchematics;
				break;
			case 2:
				pieces = new ArrayList<Piece>(schematicSet.sidePieces);
				schematics = schematicSet.sideSchematics;
				break;
			default: throw new AssertionError();
			}
			for (int i=0; i<pieces.size(); i++) {
				Piece piece = pieces.get(i);
				SchematicCellGrid schematic = schematics.get(i);
				
				int[] rootTileLocation = piece.getSchematicGridCorner();
				int[] rootLocation = new int[] {
						Dimensions.tileToCell(rootTileLocation[0]),
						Dimensions.tileToCell(rootTileLocation[1]),
						Dimensions.tileToCell(rootTileLocation[2])
				};
				int[] xOffset = piece.getXDirection().getOffsets(3);
				int[] yOffset = piece.getYDirection().getOffsets(3);
//				int[] zOffset = piece.getZDirection().getOffsets(3);
				
//				System.out.println(piece);
				for (int y=0; y<schematic.height; y++) {
					for (int x=0; x<schematic.width; x++) {
						if (schematic.getCell(x, y) == SchematicCell.SOLID) {
							int[] loc = VectorUtil.sumVectors(rootLocation, VectorUtil.scalarMultiple(xOffset, x), VectorUtil.scalarMultiple(yOffset, y));
//							System.out.println("Added solid piece cell at " + Arrays.toString(loc));
							pieceLocs.add(Arrays.asList(loc[0], loc[1], loc[2]));
						}
					}
				}
			}
		}
		pieceLocs.sort(new LexicographicComparator<>());
		
		TreeSet<List<Integer>> pieceLocsNoRep = new TreeSet<>(new LexicographicComparator<>());
		TreeSet<List<Integer>> pieceLocReps = new TreeSet<>(new LexicographicComparator<>());
		for (List<Integer> loc : pieceLocs) {
			if (!pieceLocsNoRep.add(loc)) {
				pieceLocReps.add(loc);
			}
		}
		
		TreeSet<List<Integer>> missingLocs = new TreeSet<>(mazeLocs);
		missingLocs.removeAll(pieceLocsNoRep);
		TreeSet<List<Integer>> extraLocs = new TreeSet<>(pieceLocsNoRep);
		extraLocs.removeAll(mazeLocs);
		
		if (pieceLocReps.size() != 0) throw new AssertionError("multiple pieces occupying the same cube");
		if (missingLocs.size() != 0) throw new AssertionError("cube specified by maze contract not occupied by any piece");
		if (extraLocs.size() != 0) throw new AssertionError("piece occupies cube not specified by maze contract");
	}
	
}
