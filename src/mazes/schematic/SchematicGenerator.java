/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.util.Direction;
import mazes.util.MazeIO;
import mazes.util.MultiDimensionalArray;
import util.Pair;
import util.QuadConsumer;
import util.StreamUtil;
import util.Trilean;

public final class SchematicGenerator {
	
	private SchematicGenerator() {}
	
	// command-line functionality
	public static void main(String[] args) {
		promptAndSaveMazes(SchematicGenerator::saveValidMazes);
	}
	public static void promptAndSaveMazes(QuadConsumer<Byte, Integer, String, Integer> function) {
		System.out.print("Enter maze size (e.g. 3..6): ");
		byte mazeSize = MazeIO.scanner.nextByte();
		System.out.print("Enter number of mazes to save (e.g. 1..1000): ");
		int numberOfMazes = MazeIO.scanner.nextInt();
		System.out.print("Enter base maze name (e.g. 'maze'): ");
		String baseMazeName = MazeIO.scanner.next();
		System.out.print("Starting at maze #");
		int startNum = MazeIO.scanner.nextInt();
		function.accept(mazeSize, numberOfMazes, baseMazeName, startNum);
	}
	
	// search for valid mazes
	public static void saveValidMazes(byte mazeSize, int numberOfMazes, String baseMazeName, int startNum) {
		if (mazeSize < 2) throw new IllegalArgumentException();
		if (numberOfMazes < 1 || numberOfMazes > 1000) throw new IllegalArgumentException();
		if (MazeIO.fileExists(String.format("%s%d_%03d.chmz", baseMazeName, mazeSize, startNum))) {
			System.out.println("Please move, rename, or delete pre-existing maze files.");
			System.exit(0);
		}
		System.out.printf("Attempting to generate %d valid mazes of size %d...%n", numberOfMazes, mazeSize);
		int mazesSaved = startNum;
		int invalidMazesGenerated = 0;
		Random random = new Random();
		while (mazesSaved < numberOfMazes + startNum) {
			GrowingTreeMazeGenerator gen = GrowingTreeMazeGenerator.generate3DMaze(new GrowingTreeMazeGenerator.Seed(random.nextLong(), 0.5f, mazeSize));
			MultiDimensionalArray<CellWalls> maze = gen.getMaze();
			
			try {
				PieceSet pieceSet = generatePieces(maze, false);
				SchematicSet schematicSet = generateSchematics(maze.getSideLengths(), pieceSet, false);
				SchematicChecker.checkSchematics(maze, schematicSet, false);
				
				if (!MazeIO.saveMaze(gen, String.format("%s%d_%03d.chmz", baseMazeName, mazeSize, mazesSaved), true)) {
					String alternateName;
					do {
						System.out.print("Enter alternate filename (with extension): ");
						alternateName = MazeIO.scanner.nextLine();
					}
					while (!MazeIO.saveMaze(gen, alternateName, true));
				}
				System.out.printf("Maze %03d saved successfully [%d attempts] [%s].%n", mazesSaved, invalidMazesGenerated, getTime());
				invalidMazesGenerated = 0;
				mazesSaved += 1;
			}
			catch (SchematicException e) {
				if (e.displayMessage()) {
					System.out.printf("Maze %03d schematic generation failed: %s [%d attempts] [%s].%n", mazesSaved, e.getMessage(), invalidMazesGenerated, getTime());
					invalidMazesGenerated = 0;
				}
				else {
					invalidMazesGenerated += 1;
				}
			}
			catch (Throwable e) {
				System.out.printf("Maze %03d schematic generation encountered unexpected error:%n", mazesSaved);
				e.printStackTrace(System.out);
				saveInvalidMaze(gen, mazeSize);
			}
		}
	}
	private static void saveInvalidMaze(GrowingTreeMazeGenerator gen, int mazeSize) {
		int n = 0;
		while (!MazeIO.saveMaze(gen, String.format("inv%d_%03d.maze", mazeSize, n++), false)) {}
		System.out.printf("Saved maze to inv%d_%03d.maze. [%s]%n", mazeSize, n - 1, getTime());
	}
	private static String getTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}
	
	// piece generation
	public static PieceSet generatePieces(MultiDimensionalArray<CellWalls> maze, boolean doOutput) throws SchematicException {
		return generatePieces(maze, doOutput, false);
	}
	public static PieceSet generatePieces(MultiDimensionalArray<CellWalls> maze, boolean doOutput, boolean overrideExceptions) throws SchematicException {
		if (doOutput) System.out.print("Generating tetris pieces... ");
		List<TetrisWallPiece> tetrisPieces = generateTetrisPieces(maze);
		if (doOutput) System.out.print("Done.\nGenerating layer pieces... ");
		List<LayerPiece> layerPieces = generateLayerPieces(maze, tetrisPieces);
		if (!noUnsupportedPieces(tetrisPieces, layerPieces)) {
			if (doOutput) System.out.println();
			if (!overrideExceptions) throw new SchematicException("one or more tetris pieces with insufficient layer piece support", false);
		}
		if (doOutput) System.out.print("Done.\nGenerating side pieces... ");
		List<SidePiece> sidePieces = generateSidePieces(maze, tetrisPieces, layerPieces);
		if (doOutput) System.out.println("Done");
		
		if (doOutput) {
			System.out.println();
			
			System.out.println("=> Listing tetris pieces:");
			System.out.println();
			for (TetrisWallPiece piece : tetrisPieces) {
				System.out.println(piece);
			}
			
			System.out.println("=> Listing layer pieces:");
			System.out.println();
			List<LayerPiece> reversed = new ArrayList<>(layerPieces);
			Collections.reverse(reversed);
			for (LayerPiece layerPiece : reversed) {
				System.out.println(layerPiece);
			}
			
			System.out.println("=> Listing side pieces:");
			System.out.println();
			for (SidePiece sidePiece : sidePieces) {
				System.out.println(sidePiece);
			}
			
			System.out.println();
		}
		
		return new PieceSet(tetrisPieces, layerPieces, sidePieces);
	}
	private static List<TetrisWallPiece> generateTetrisPieces(MultiDimensionalArray<CellWalls> maze) {
		int[] mazeSize = maze.getSideLengths();
		int xLength = mazeSize[0], yLength = mazeSize[1], zLength = mazeSize[2];
		
		// +----+----+----+ y = 3
		// | 0, | 1, | 2, |
		// | 2  | 2  | 2  |
		// +----+----+----+ y = 2
		// | 0, | 1, | 2, |
		// | 1  | 1  | 1  |
		// +----+----+----+ y = 1
		// | 0, | 1, | 2, |
		// | 0  | 0  | 0  |
		// +----+----+----+ y = 0
	// 
	// x = 0    1    2    3
	// 
		// Generate tetris wall pieces parallel to the xz-plane (normal to the +y direction).
		// [Then, generate pieces parallel to the yz-plane.]
		// First, iterate through the +y planes for each cell except the top one.
		// [Then, iterate through the +x planes.]
		// Then, iterate through the x and z directions for all cells in each plane.
		// [Then, iterate through the y and z directions.]
		
		List<List<TetrisWallPiece>> tetrisYZPlanes = null,
				tetrisXZPlanes = null;
		for (boolean isXZ : new boolean[] {true, false}) {
//			System.out.println("==> isXZ = " + isXZ);
			// Create the current set of yz- [then xz-] planes.
			// This will be assigned to the appropriate variable later.
			List<List<TetrisWallPiece>> currentTetrisPlanes = new ArrayList<>();
			for (int yThenX=0; yThenX < (isXZ ? yLength : xLength) - 1; yThenX++) {
				// Separate [x-then-y]z-planes of tetris pieces are considered separately.
				List<TetrisWallPiece> currentTetrisPlane = new ArrayList<>();
				for (int z=0; z<zLength; z++) { // remember, z wall bounds are inclusive
					for (int xThenY=0; xThenY < (isXZ ? xLength : yLength); xThenY++) {
						// The location will always be {x, y, z}.
						int[] location = isXZ ? new int[] {xThenY, yThenX, z} : // first
							new int[] {yThenX, xThenY, z}; // then
						// Check if wall exists at this location.
						// Switch the direction of the wall gotten from a cell based
						// on whether the y-axis (first) or x-axis (next) is selected.
						// [The `true` refers to positive/negative direction, and does
						// not need to be switched.]
//						System.out.println("Checking wall at " + Arrays.toString(location));
						if (maze.get(location).getWall(new Direction(isXZ ? 1 : 0, true))) {
//							System.out.println("Wall exists.");
							// If so, check if the wall may be added to any existing piece.
							boolean addedSuccessfully = false;
//							System.out.println("Checking " + currentTetrisPlane.size() + " pieces on current plane...");
							int contiguousPieces = 0;
							TetrisWallPiece containingPiece = null;
							// If the new wall segment is adjacent to an existing piece, add it to that piece.
							// Otherwise, create a new piece with this wall segment as its root offset.
							// If the wall segment is adjacent to more than one existing piece, add it to the
							// first piece and then combine the first piece with any subsequent adjacent pieces,
							// if the resulting composites will be valid.
							for (int i=0; i<currentTetrisPlane.size(); i++) { // support in-loop modification
								TetrisWallPiece piece = currentTetrisPlane.get(i);
								if (piece.isValidPlacement(location)) {
									contiguousPieces += 1;
									if (contiguousPieces == 1) {
//										System.out.println("Adding to pre-existing piece.");
										piece.addCoordinate(location);
										containingPiece = piece;
//										System.out.println("Piece now reads:");
//										System.out.println(piece);
//										System.out.println();
										addedSuccessfully = true;
									}
									else {
										// Consider what will happen if we combine these two adjacent pieces.
										TetrisWallPiece combinedPiece = containingPiece.combineWith(piece);
										if (combinedPiece.isValid()) {
//											System.out.println("Combining following two pieces:");
//											System.out.println(containingPiece);
//											System.out.println();
//											System.out.println(piece);
//											System.out.println();
//											System.out.println("Composite piece is:");
//											System.out.println(combinedPiece);
//											System.out.println();
											// Remove the two component pieces and add the resulting composite piece.
											// We know that the original piece (containingPiece) will be first in the array.
											// Let's keep it that way:
											currentTetrisPlane.set(currentTetrisPlane.indexOf(containingPiece), combinedPiece);
											containingPiece = combinedPiece;
											currentTetrisPlane.remove(i);
											i -= 1; // once again, we might want to combine more than two pieces
													// so we make sure not to miss any pieces
										}
									}
								}
							}
							if (!addedSuccessfully) {
								// isYZ = false --> xz-plane (y-axis)                    |
								// isYZ = true --> yz-plane (x-axis)                isYZ v
								TetrisWallPiece newPiece = new TetrisWallPiece(location, !isXZ);
//								System.out.println("Creating new piece.");
								// Adding the (0, 0) Coordinate will place the initial wall
								// at the current location.
								newPiece.addCoordinate(new Coordinate(0, 0));
//								System.out.println("Piece now reads:");
//								System.out.println(newPiece);
//								System.out.println();
								currentTetrisPlane.add(newPiece);
							}
						}
					}
				}
//				System.out.println("--- Created " + (isXZ ? "XZ" : "YZ") + " plane at " + (isXZ ? "Y" : "X") + " = " + yThenX + " with following pieces:");
//				for (TetrisWallPiece piece : currentTetrisPlane) {
//					System.out.println(piece);
//				}
				currentTetrisPlanes.add(currentTetrisPlane);
			}
			if (isXZ) tetrisXZPlanes = currentTetrisPlanes;
			else tetrisYZPlanes = currentTetrisPlanes;
		}
		
		if (tetrisXZPlanes == null || tetrisYZPlanes == null) throw new AssertionError(); // fail fast
		
		splitIntersectingTetrisPieces(tetrisYZPlanes, tetrisXZPlanes);
		
		// Generation of tetris wall pieces is now complete. We have generated their
		// shapes and positions (these are stored with each piece) and dealt with
		// intersections between the two sets (stored in tetrisXZPlanes and tetrisYZPlanes).
		
		List<TetrisWallPiece> tetrisPieces = new ArrayList<>();
		for (List<List<TetrisWallPiece>> planes : Arrays.asList(tetrisXZPlanes, tetrisYZPlanes)) {
			for (List<TetrisWallPiece> plane : planes) {
				tetrisPieces.addAll(plane);
			}
		}
		return tetrisPieces;
	}
	private static void splitIntersectingTetrisPieces(
			List<List<TetrisWallPiece>> tetrisYZPlanes, List<List<TetrisWallPiece>> tetrisXZPlanes) {
		// Give the y-axis (XZ) pieces priority; they will remain unchanged.
		// Any x-axis (YZ) pieces that intersect a y-axis (XZ) piece will be split.
		
		// Given the following piece: (generated in the second, YZ-plane phase)
		//      +----+----+
		//      |    |    |
		//      |    |    |    +z
		// +----+----+----+   ^  
		// |    |    |        |
		// |    |    |        |     +y
		// +----+----+        +---->
		//         \         /
		//          \    +x /
		//           \
		//            --- root location, Coordinate(0, 0), location {3, 5, 4}.
		
		// Mapping: plane corresponding to x0 maps to adjacent cells at
		//          coordinates x0 and x0 + 1.
		
		for (List<TetrisWallPiece> yzPlane : tetrisYZPlanes) {
			iteratingThroughYZPieces:
				for (int i=0; i<yzPlane.size(); i++) { // supports concurrent modification
					TetrisWallPiece yzPiece = yzPlane.get(i);
					for (List<TetrisWallPiece> xzPlane : tetrisXZPlanes) {
						for (TetrisWallPiece xzPiece : xzPlane) {
//							System.out.println("Checking orthogonal intersection between following XZ and YZ pieces:");
//							System.out.println(xzPiece);
//							System.out.println(yzPiece);
							if (xzPiece.intersectsOrthogonally(yzPiece)) {
//								System.out.println("Found orthogonal intersection between the following two pieces:");
//								System.out.println(xzPiece);
//								System.out.println(yzPiece);
								// Split the YZ piece...
								yzPlane.remove(i); // or yzPlane.remove(yzPiece), same thing
								TetrisWallPiece[] splitPieces = yzPiece.splitAlongPlane(xzPiece);
//								System.out.println("Split YZ piece into the following two component pieces:");
//								System.out.println(splitPieces[0]);
//								System.out.println(splitPieces[1]);
								yzPlane.add(i, splitPieces[1]);
								yzPlane.add(i, splitPieces[0]); // add them in order, [0] then [1] in the resulting collection
								continue iteratingThroughYZPieces;
							}
						}
					}
				}
		}
	}
	private static List<LayerPiece> generateLayerPieces(MultiDimensionalArray<CellWalls> maze, List<TetrisWallPiece> tetrisPieces) {
		int[] mazeSize = maze.getSideLengths();
		int xLength = mazeSize[0], yLength = mazeSize[1], zLength = mazeSize[2];
		// For the tetris pieces, we mapped each cell to its more positive wall.
		// For the layer pieces we will do the same thing, but with a special case
		// to account for the bottom plane. (The bottom plane, as the positive wall of
		// cell layer -1, is also the negative wall of cell layer 0.)
		List<LayerPiece> layerPieces = new ArrayList<>();
		for (int z=-1; z<zLength; z++) {
//			System.out.println("z = " + z);
			// Punch holes for z-axis passages
			LayerPiece currentLayer = new LayerPiece(xLength, yLength, z);
			boolean isBottomLayer = z == -1;
			for (int x=0; x<xLength; x++) {
				for (int y=0; y<yLength; y++) {
					// on bottom layer, check negative direction from z = 0 (instead of typical positive direction from z = -1);
					// on other layers, check positive direction from the actual z layer
					currentLayer.setHole(x, y, !maze.get(new int[] {x, y, Math.max(0, z)})
							.getWall(new Direction(2, !isBottomLayer)));
				}
			}
			// Calculate all intersections of tetris pieces with the current layer
			// and make the requisite perforations (dotted or solid / slot)
			for (TetrisWallPiece piece : tetrisPieces) {
				boolean isXZ = piece.isXZ();
//				System.out.println("Intersecting piece:");
//				System.out.println(piece);
//				System.out.println("Layer changes from:");
//				System.out.println(currentLayer);
				// We intersect a vertical tetris piece with a horizontal plane...
				// We already know the Z coordinate; thus (suppose here that the piece
				// is XZ), we may simply iterate along the X axis of the piece, forming
				// a local Coordinate from the current X and the given Z (height of the
				// plane), and check if there is a tile above and below the Z-plane.
				// (Checking for tiles above and below a plane is equivalent to checking
				// for tiles at and above its Z-coordinate.)
				// If we have only one tile vertically adjacent, we make a perforation;
				// if we have two tiles vertically adjacent (from the same piece -- in
				// theory, it should be impossible for there to be two vertically adjacent
				// wall segments on different pieces), we make a slot.
				for (int xy /* x then y */ = piece.getMinimumXYCoordinate();
						xy <= piece.getMaximumXYCoordinate();
						xy++) {
//					System.out.println("Checking " + (isXZ ? "x" : "y") + " coordinate at " + xy);
//					System.out.println("Checking upper coordinate: " + Arrays.toString(new int[] {isXZ ? xy : -1, isXZ ? -1 : xy, Math.max(0, z) + 1}));
					boolean hasPieceAbove = piece.contains(new int[] {isXZ ? xy : -1, isXZ ? -1 : xy, z+1});
//					System.out.println("\t\t(" + hasPieceAbove + ")");
//					System.out.println("Checking lower coordinate: " + Arrays.toString(new int[] {isXZ ? xy : -1, isXZ ? -1 : xy, Math.max(0, z)}));
					boolean hasPieceBelow = piece.contains(new int[] {isXZ ? xy : -1, isXZ ? -1 : xy, z});
//					System.out.println("\t\t(" + hasPieceBelow + ")");
					if (hasPieceAbove || hasPieceBelow) {
						
						Perforation perfType;
						if (hasPieceAbove ^ hasPieceBelow) perfType = Perforation.PERF;
						else if (hasPieceAbove && hasPieceBelow) perfType = Perforation.SLOT;
						else throw new AssertionError();
						
//						System.out.println("Horizontal coordinates: {parallel = " + xy + " / normal = " + piece.getNormalOffset() + "}");
						int lx = isXZ ? xy : piece.getNormalOffset();
						// could be calculated using the index
						// of the piece plane, but this is more foolproof
						int ly = isXZ ? piece.getNormalOffset() : xy;
//						System.out.println("Resulting coordinates: {" + lx + ", " + ly + "}");
						
						currentLayer.setPerforation(lx, ly,
								new Direction(isXZ ? 1 : 0, true), // always positive because of my tetris piece indices
								perfType // also, the slot direction is specified by its normal dimension
								); // mirror perforation is added automatically in LayerPiece code
					}
				}
//				System.out.println("to:");
//				System.out.println(currentLayer);
			}
			layerPieces.add(currentLayer);
		}
		
		return layerPieces;
	}
	private static List<SidePiece> generateSidePieces(
			MultiDimensionalArray<CellWalls> maze, List<TetrisWallPiece> tetrisPieces, List<LayerPiece> layerPieces) {
		int[] mazeSize = maze.getSideLengths();
		int xLength = mazeSize[0], yLength = mazeSize[1], zLength = mazeSize[2];
		List<SidePiece> sidePieces = new ArrayList<>();
		for (Direction normalDirection : Direction.getDirections(2)) {
			int normalDimension = normalDirection.getDimension();
			boolean isXZ = normalDimension == 1, isYZ = !isXZ; // this'll be YZ-then-XZ.
			boolean isPositiveSide = normalDirection.isPositive();
			
			SidePiece sidePiece = new SidePiece(normalDirection, normalDimension == 0 ? yLength : xLength, zLength, normalDimension == 0 ? xLength : yLength);
			
			// We start to build up our coordinates. The first constraint is that we are on a particular
			// side of the maze -- either at 0 or length-1.
			int xThenY = isPositiveSide ? (isYZ ? xLength : yLength) - 1 : 0; // either this side or the other side
			
			// Add perforations for layer pieces.
			for (LayerPiece layerPiece : layerPieces) {
				int z = layerPiece.z;
				if (z == -1 || z == zLength) continue; // these will be easier dealt with as special cases
				for (int yThenX=0; yThenX<sidePiece.getWidth(); yThenX++) {
					// Since YZ comes first, read: if on first, [x]thenY; if on second, yThen[X].
					// We thus assure that we get X every time. (and vice versa for Y)
					boolean isLayerTile = !layerPiece.getHole(isYZ ? xThenY : yThenX, isYZ ? yThenX : xThenY);
					// If there is a layer tile, we want to perforate the side piece.
					// This will always be allowable, so we will perforate all outside edges of
					// all of the layer pieces. (No need to add more data to them!)
					sidePiece.setPerforation(yThenX, z, new Direction(1, true), isLayerTile);
					// Direction 1 is actually upwards in the local coordinate system (corresponds to Direction 2 in the global coordinate system)
				}
			}
			
			// Add perforations for tetris pieces.
			for (TetrisWallPiece tetrisPiece : tetrisPieces) {
				if (tetrisPiece.isYZ() == isYZ) continue; // only perpendicular tetris pieces can perforate
				int yThenX = tetrisPiece.getNormalOffset();
				for (int z=tetrisPiece.getMinimumZCoordinate(); z<=tetrisPiece.getMaximumZCoordinate(); z++) {
					if (tetrisPiece.contains(new int[] {isYZ ? xThenY : yThenX, isYZ ? yThenX : xThenY, z})) {
						sidePiece.setPerforation(yThenX, z, new Direction(0, true), true); // likewise with Direction 0.
					}
				}
			}
			
			// Add holes if necessary (for entrance and exit).
			for (int yThenX=0; yThenX<sidePiece.getWidth(); yThenX++) {
				for (int z=0; z<sidePiece.getHeight(); z++) {
					boolean wallPresent = maze.get(new int[] {isYZ ? xThenY : yThenX, isYZ ? yThenX : xThenY, z}).getWall(normalDirection);
					if (!wallPresent) {
						sidePiece.addHole(new Coordinate(yThenX, z));
					}
				}
			}
			
			sidePieces.add(sidePiece);
		}
		
		return sidePieces;
	}
	public static boolean noUnsupportedPieces(List<TetrisWallPiece> tetrisPieces, List<LayerPiece> layerPieces) {
		// Now, we must decide if this maze is valid, i.e. whether any of the pieces are
		// not supported well enough.
		boolean isValid = true;
		for (TetrisWallPiece tetrisPiece : tetrisPieces) {
			// Let us try to disprove the validity of this piece. (And thus of the entire maze.)
			int totalFaces = 0, supportedLowerFaces = 0, supportedUpperFaces = 0;
			// getMinimum() and getMaximum() return tile coordinates; we convert to line coordinates
			int lowestLineZ = tetrisPiece.getMinimumZCoordinate()-1, highestLineZ = tetrisPiece.getMaximumZCoordinate();
			for (int z=lowestLineZ; z<=highestLineZ; z++) {
				LayerPiece layerPiece = null;
				for (LayerPiece maybeLayerPiece : layerPieces) {
					if (maybeLayerPiece.z == z) {
						layerPiece = maybeLayerPiece;
					}
				}
				if (layerPiece == null) throw new AssertionError();
				for (int xy=tetrisPiece.getMinimumXYCoordinate(); xy<=tetrisPiece.getMaximumXYCoordinate(); xy++) {
					int x = tetrisPiece.getHorizontalNormalDimension() == 0 ? tetrisPiece.getNormalOffset() : xy;
					int y = tetrisPiece.getHorizontalNormalDimension() == 1 ? tetrisPiece.getNormalOffset() : xy;
					// LAYER PIECE contains horizontally displaced x and y coordinate tiles
					int firstTileX = x, firstTileY = y,
							secondTileX = x + (tetrisPiece.getHorizontalNormalDimension() == 0 ? 1 : 0),
							secondTileY = y + (tetrisPiece.getHorizontalNormalDimension() == 1 ? 1 : 0);
					// TETRIS PIECE contains upper & lower z coordinate tiles
					boolean containsUpper = tetrisPiece.contains(tetrisPiece.locationToCoordinate(new int[] {x, y, z+1})),
							containsLower = tetrisPiece.contains(tetrisPiece.locationToCoordinate(new int[] {x, y, z}));
					// ... We must be at the (top or bottom) edge of this tetris piece
					//     to consider the current location a "face"
					if (containsUpper == containsLower) continue; // (x == y) <==> !(x ^ y) <==> "both or none"
					totalFaces += 1; // it is a face
					// Is the face supported?
					boolean isSupported = !(layerPiece.getHole(firstTileX, firstTileY) && layerPiece.getHole(secondTileX, secondTileY)); // supported by at least one side out of two
					if (isSupported) {
						if (containsUpper && !containsLower) { // lower face
							supportedLowerFaces += 1;
						}
						else {
							supportedUpperFaces += 1;
						}
					}
				}
			}
			if (supportedLowerFaces == 0 || supportedUpperFaces == 0) {
				isValid = false;
				break;
			}
			if ((supportedLowerFaces + supportedUpperFaces) / (double) totalFaces < 0.5) {
				isValid = false;
				break;
			}
		}
		return isValid;
	}
	public static void countPoorlySupportedPieces(List<TetrisWallPiece> tetrisPieces, List<LayerPiece> layerPieces) {
		System.out.println("Counting poorly supported pieces...");
		// Now, we must decide if this maze is valid, i.e. whether any of the pieces are
		// not supported well enough.
		for (TetrisWallPiece tetrisPiece : tetrisPieces) {
			// Let us try to disprove the validity of this piece. (And thus of the entire maze.)
			int totalLowerFaces = 0, totalUpperFaces = 0, supportedLowerFaces = 0, supportedUpperFaces = 0;
			// getMinimum() and getMaximum() return tile coordinates; we convert to line coordinates
			int lowestLineZ = tetrisPiece.getMinimumZCoordinate()-1, highestLineZ = tetrisPiece.getMaximumZCoordinate();
			for (int z=lowestLineZ; z<=highestLineZ; z++) {
				LayerPiece layerPiece = null;
				for (LayerPiece maybeLayerPiece : layerPieces) {
					if (maybeLayerPiece.z == z) {
						layerPiece = maybeLayerPiece;
					}
				}
				if (layerPiece == null) throw new AssertionError();
				for (int xy=tetrisPiece.getMinimumXYCoordinate(); xy<=tetrisPiece.getMaximumXYCoordinate(); xy++) {
					int x = tetrisPiece.getHorizontalNormalDimension() == 0 ? tetrisPiece.getNormalOffset() : xy;
					int y = tetrisPiece.getHorizontalNormalDimension() == 1 ? tetrisPiece.getNormalOffset() : xy;
					// LAYER PIECE contains horizontally displaced x and y coordinate tiles
					int firstTileX = x, firstTileY = y,
							secondTileX = x + (tetrisPiece.getHorizontalNormalDimension() == 0 ? 1 : 0),
							secondTileY = y + (tetrisPiece.getHorizontalNormalDimension() == 1 ? 1 : 0);
					// TETRIS PIECE contains upper & lower z coordinate tiles
					boolean containsUpper = tetrisPiece.contains(tetrisPiece.locationToCoordinate(new int[] {x, y, z+1})),
							containsLower = tetrisPiece.contains(tetrisPiece.locationToCoordinate(new int[] {x, y, z}));
					// ... We must be at the (top or bottom) edge of this tetris piece
					//     to consider the current location a "face"
					if (containsUpper == containsLower) continue; // (x == y) <==> !(x ^ y) <==> "both or none"
					// Is the face supported?
					boolean isSupported = !layerPiece.getHole(firstTileX, firstTileY) && !layerPiece.getHole(secondTileX, secondTileY); // supported by at least one side out of two
					if (containsUpper && !containsLower) { // lower face
						totalLowerFaces += 1;
						if (isSupported) {
							supportedLowerFaces += 1;
						}
					}
					else {
						totalUpperFaces += 1;
						if (isSupported) {
							supportedUpperFaces += 1;
						}
					}
				}
			}
			if (supportedLowerFaces == 0 || supportedUpperFaces == 0) {
				System.out.println(tetrisPiece);
				System.out.printf("%d out of %d lower faces are fully supported.%n", supportedLowerFaces, totalLowerFaces);
				System.out.printf("%d out of %d upper faces are fully supported.%n", supportedUpperFaces, totalUpperFaces);
			}
		}
	}
	
	// schematic generation
	public static SchematicSet generateSchematics(
			int[] mazeSize, PieceSet pieceSet, boolean doOutput) throws SchematicException {
		return generateSchematics(mazeSize, pieceSet, doOutput, false);
	}
	public static SchematicSet generateSchematics(
			int[] mazeSize, PieceSet pieceSet, boolean doOutput, boolean overrideExceptions) throws SchematicException {
		
		List<TetrisWallPiece> tetrisPieces = pieceSet.tetrisPieces;
		List<LayerPiece> layerPieces = pieceSet.layerPieces;
		List<SidePiece> sidePieces = pieceSet.sidePieces;
		
		// Calculate placement of central columns and cubes on the tetris schematics.
		if (doOutput) System.out.print("Calculating central column locations... ");
		TetrisWallPiece[][][] centralColumns = calculateCentralColumns(mazeSize, tetrisPieces, overrideExceptions);
		Map<TetrisWallPiece, Collection<int[]>> centralColumnGroups = groupInterstices(
				centralColumns, tetrisPieces);
		if (doOutput) System.out.print("Done.\nCalculating central cube locations... ");
		TetrisWallPiece[][][] centralCubes = calculateCentralCubes(mazeSize, centralColumns);
		Map<TetrisWallPiece, Collection<int[]>> centralCubeGroups = groupInterstices(
				centralCubes, tetrisPieces);
		
		// Calculate the basic schematics for each type of piece.
		if (doOutput) System.out.print("Done.\nGenerating initial tetris schematics... ");;
		List<SchematicCellGrid> tetrisSchematics = calculateInitialTetrisSchematics(
				mazeSize, tetrisPieces, centralColumnGroups, centralCubeGroups, layerPieces);
		if (doOutput) System.out.print("Done.\nGenerating initial layer schematics... ");
		List<SchematicCellGrid> layerSchematics = calculateInitialLayerSchematics(
				mazeSize, layerPieces, sidePieces);
		if (doOutput) System.out.print("Done.\nGenerating initial side schematics... ");
		List<SchematicCellGrid> sideSchematics = calculateInitialSideSchematics(
				mazeSize, layerPieces, sidePieces);
		if (doOutput) System.out.println("Done.");
		
		if (doOutput) {
			System.out.println("=> Listing initial tetris schematics:");
			System.out.println();
			for (SchematicCellGrid schematic : tetrisSchematics) {
				System.out.println(schematic);
				System.out.println();
			}
			System.out.println("=> Listing initial layer schematics:");
			System.out.println();
			for (SchematicCellGrid schematic : layerSchematics) {
				System.out.println(schematic);
				System.out.println();
			}
			System.out.println("=> Listing initial side schematics:");
			System.out.println();
			for (SchematicCellGrid schematic : sideSchematics) {
				System.out.println(schematic);
				System.out.println();
			}
		}
		
		// Determine the perforations for these pieces. The perforations ought only to be
		// strictly on the edges, not the vertices, faces, or corners.
		if (doOutput) System.out.print("Determining tetris schematic perforations...");
		determineTetrisSchematicPerforations(tetrisSchematics, tetrisPieces);
		if (doOutput) System.out.print("Done.\nDetermining layer schematic perforations...");
		determineLayerSchematicPerforations(layerSchematics);
		if (doOutput) System.out.print("Done.\nDetermining side schematic perforations...");
		determineSideSchematicPerforations(sidePieces, sideSchematics);
		if (doOutput) System.out.println("Done.");
		
		tetrisSchematics.forEach(SchematicCellGrid::assertDetermined);
		layerSchematics.forEach(SchematicCellGrid::assertDetermined);
		sideSchematics.forEach(SchematicCellGrid::assertDetermined);
		
		if (doOutput) {
			System.out.println("=> Listing final tetris schematics:");
			System.out.println();
			for (SchematicCellGrid schematic : tetrisSchematics) {
				System.out.println(schematic);
				System.out.println();
			}
			System.out.println("=> Listing final layer schematics:");
			System.out.println();
			for (SchematicCellGrid schematic : layerSchematics) {
				System.out.println(schematic);
				System.out.println();
			}
			System.out.println("=> Listing final side schematics:");
			System.out.println();
			for (SchematicCellGrid schematic : sideSchematics) {
				System.out.println(schematic);
				System.out.println();
			}
		}
		
		return new SchematicSet(
				tetrisPieces, layerPieces, sidePieces,
				tetrisSchematics, layerSchematics, sideSchematics);
	}
	private static TetrisWallPiece[][][] calculateCentralColumns(
			int[] mazeSize, Collection<TetrisWallPiece> tetrisPieces, boolean overrideExceptions) throws SchematicException {
		// Determine, for each central column, which tetris pieces could be attached to it.
		// Also check (for each possibility) if the cell adjacent to the central column is
		// at the bottom of its respective tetris piece.
		MultiDimensionalArray<LinkedHashMap<TetrisWallPiece, Boolean>> columnPossibilities = new MultiDimensionalArray<>(
				new int[] {mazeSize[0] - 1, mazeSize[1] - 1, mazeSize[2]});
		columnPossibilities.fill(() -> new LinkedHashMap<>());
		Trilean[][][] alreadyOccupied = new Trilean[mazeSize[0]-1][mazeSize[1]-1][mazeSize[2]];
		for (int x=0; x<alreadyOccupied.length; x++)
			for (int y=0; y<alreadyOccupied[x].length; y++)
				for (int z=0; z<alreadyOccupied[x][y].length; z++)
					alreadyOccupied[x][y][z] = Trilean.False;
		// False -> True -> FileNotFound
//		System.out.println("FINDING COLUMN POSSIBILITIES");
		for (TetrisWallPiece tetrisPiece : tetrisPieces) {
//			System.out.println(tetrisPiece);
			for (Coordinate coord : tetrisPiece) {
//				System.out.println();
//				System.out.println("Coordinate: " + coord);
				
				Coordinate leftCoord = coord.getIncrement(new Direction(0, false));
				Coordinate rightCoord = coord.getIncrement(new Direction(0, true));
				int[] leftLocation = tetrisPiece.coordinateToLocation(leftCoord);
				int[] location = tetrisPiece.coordinateToLocation(coord); // note: NOT rightCoord. just coord.
				
				boolean isBottom = coord.y == tetrisPiece.getMinimumYCoordinate();
//				System.out.println("isBottom = " + isBottom);
				
//				System.out.println("Left column location: " + Arrays.toString(leftLocation));
				if (leftLocation[0] >= 0 && leftLocation[0] < mazeSize[0] - 1 &&
						leftLocation[1] >= 0 && leftLocation[1] < mazeSize[1] - 1 &&
						leftLocation[2] >= 0 && leftLocation[2] < mazeSize[2]) {
//					System.out.println("Location is within grid bounds.");
					// Stop adding new pieces if we have already found one that "already occupies" the column
					// Otherwise, add the piece we have found.
					if (alreadyOccupied[leftLocation[0]][leftLocation[1]][leftLocation[2]] != Trilean.FileNotFound) {
//						System.out.println("Adding this piece as a possibility.");
						columnPossibilities.get(leftLocation).put(tetrisPiece, isBottom);
					}
					else {
//						System.out.println("This column already fully occupied.");
					}
					// Tetris piece "already occupies" this column --
					// as a consequence of having tiles on either side of it
					if (tetrisPiece.contains(leftCoord)) {
//						System.out.println("Tetris piece fully contains this column.");
						switch (alreadyOccupied[leftLocation[0]][leftLocation[1]][leftLocation[2]]) {
						case False: alreadyOccupied[leftLocation[0]][leftLocation[1]][leftLocation[2]] = Trilean.True; break;
						case True: alreadyOccupied[leftLocation[0]][leftLocation[1]][leftLocation[2]] = Trilean.FileNotFound; break;
						case FileNotFound: throw new AssertionError();
						default: throw new AssertionError();
						}
					}
				}
				
//				System.out.println("Right column location: " + Arrays.toString(location));
				if (location[0] >= 0 && location[0] < mazeSize[0] - 1 &&
						location[1] >= 0 && location[1] < mazeSize[1] - 1 &&
						location[2] >= 0 && location[2] < mazeSize[2]) {
//					System.out.println("Location is within grid bounds.");
					if (alreadyOccupied[location[0]][location[1]][location[2]] != Trilean.FileNotFound) {
//						System.out.println("Adding this piece as a possibility.");
						columnPossibilities.get(location).put(tetrisPiece, isBottom);
					}
					else {
//						System.out.println("This column already fully occupied.");
					}
					if (tetrisPiece.contains(rightCoord)) {
//						System.out.println("Tetris piece fully contains this column.");
						switch (alreadyOccupied[location[0]][location[1]][location[2]]) {
						case False: alreadyOccupied[location[0]][location[1]][location[2]] = Trilean.True; break;
						case True: alreadyOccupied[location[0]][location[1]][location[2]] = Trilean.FileNotFound; break;
						case FileNotFound: throw new AssertionError();
						default: throw new AssertionError();
						}
					}
				}
			}
		}
//		System.out.println(columnPossibilities);
		
		for (int x=0; x<alreadyOccupied.length; x++)
			for (int y=0; y<alreadyOccupied[x].length; y++)
				for (int z=0; z<alreadyOccupied[x][y].length; z++)
					if (alreadyOccupied[x][y][z] == Trilean.True) throw new AssertionError(); // should be False or FileNotFound
		
		// Determine how for each tetris piece can go up. -1 indicates that it can't start there.
		MultiDimensionalArray<Map<TetrisWallPiece, Integer>> distanceUpward =
				new MultiDimensionalArray<>(new int[] {mazeSize[0]-1, mazeSize[1]-1, mazeSize[2]});
		for (int x=0; x<mazeSize[0]-1; x++) {
			for (int y=0; y<mazeSize[1]-1; y++) {
				for (int Z=0; Z<mazeSize[2]; Z++) { // initial z
					Map<TetrisWallPiece, Integer> newMap = new HashMap<>();
					for (Map.Entry<TetrisWallPiece, Boolean> entry : columnPossibilities.get(new int[] {x, y, Z}).entrySet()) {
						TetrisWallPiece tetrisPiece = entry.getKey();
						boolean canStartHere = entry.getValue();
						if (canStartHere) {
							int z = Z;
							while (z != mazeSize[2] && columnPossibilities.get(new int[] {x, y, z}).containsKey(tetrisPiece)) {
								z += 1;
							}
							newMap.put(tetrisPiece, z - Z);
						}
						else {
							newMap.put(tetrisPiece, -1);
						}
					}
					distanceUpward.set(new int[] {x, y, Z}, newMap);
				}
			}
		}
//		System.out.println(distanceUpward);
		
		// Decide which tetris piece will be used for each section of the central column. (If there is
		// a section that cannot be filled by any tetris piece according to their distanceUpward entries,
		// then raise an error.)
		TetrisWallPiece[][][] columnChoices = new TetrisWallPiece[mazeSize[0]-1][mazeSize[1]-1][mazeSize[2]];
		for (int x=0; x<mazeSize[0]-1; x++) {
			for (int y=0; y<mazeSize[1]-1; y++) {
//				System.out.println("Beginning upwards trek for (" + x + ", " + y + ")...");
				TetrisWallPiece currentPiece = null;
				int counter = 0;
				// if counter == 0, then we have run out of the tetris piece
				// we were following and MUST find a new one in the current square
				for (int z=0; z<mazeSize[2]; z++) {
//					System.out.println("Analyzing z = " + z + "...");
					int[] location = new int[] {x, y, z};
					Map.Entry<TetrisWallPiece, Integer> chosenEntry;
					if (alreadyOccupied[x][y][z] == Trilean.FileNotFound) {
//						System.out.println("Current column is already occupied.");
						// If a piece already occupies this central column, pick it -- for sure.
						// Since we used a LinkedHashMap, and since we prevented any pieces from
						// being added to the possibility list after one was discovered that
						// already occupied the column, the already-occupying-column piece will be
						// the last one in the list.
						TetrisWallPiece lastPiece = null;
						for (TetrisWallPiece piece : columnPossibilities.get(location).keySet()) {
							lastPiece = piece;
						}
						if (currentPiece == lastPiece) {
							// We can keep going up.
							chosenEntry = new AbstractMap.SimpleEntry<>(currentPiece, counter);
						}
						else {
							// We can't.
							chosenEntry = new AbstractMap.SimpleEntry<>(lastPiece, distanceUpward.get(location).get(lastPiece));
						}
//						System.out.println("Chosen piece " + chosenEntry.getKey() + ".");
					}
					else {
//						System.out.println("Looking for lucrative pieces.");
						// Otherwise, look for other pieces.
						// If we can start a new piece here, then do so. And pick the one that can go the farthest upward.
						chosenEntry = new AbstractMap.SimpleEntry<>(null, Integer.MIN_VALUE);
						for (Map.Entry<TetrisWallPiece, Integer> entry : distanceUpward.get(location).entrySet()) {
							if (entry.getValue() > chosenEntry.getValue()) {
								chosenEntry = entry;
							}
						}
//						if (chosenEntry.getKey() == null) System.out.println("No pieces available.");
//						else System.out.println("Chosen piece " + chosenEntry.getKey() + ".");
						if (chosenEntry.getValue() <= counter) {
							chosenEntry = new AbstractMap.SimpleEntry<>(currentPiece, counter);
						}
					}
					// Cheaty, cheaty... this will make a tetris piece that cannot actually be inserted into
					// the maze, but it will satisfy my constraints. Needless to say, please do not set
					// overrideExceptions for an actual maze.
					if (chosenEntry.getValue() == 0 && overrideExceptions) {
						Map.Entry<TetrisWallPiece, Integer> entry = distanceUpward.get(location).entrySet().iterator().next();
						chosenEntry = new AbstractMap.SimpleEntry<>(entry.getKey(), 1);
					}
					// If we found one, make a note of it and reset the game over counter.
					if (chosenEntry.getKey() != null) {
						currentPiece = chosenEntry.getKey();
						counter = chosenEntry.getValue();
//						System.out.println("Reset chosen piece. Counter set to " + counter);
					}
					// If we couldn't find one and we have run out of the last piece we found (or we couldn't
					// find a piece at z = 0), then BOOM.
					if (counter == 0) {
//						System.out.println("We ran out of pieces... *cries*");
						currentPiece = null;
						throw new SchematicException(String.format("cannot fill central column at (%d, %d, %d)", x, y, z), true);
					}
					// Make a note of our choice.
					columnChoices[x][y][z] = currentPiece;
//					if (currentPiece != null) System.out.println("columnChoices[" + x + "][" + y + "][" + z + "] noted.");
//					else System.out.println("columnChoices[" + x + "][" + y + "][" + z + "] could not be set. (Aw, shucks.)");
					// Move upwards.
					counter -= 1;
//					System.out.println("Counter decremented from " + (counter+1) + " to " + counter);
				}
			}
		}
		
		// There should be no way whatsoever that I miss an index when filling the array.
		// Every central column should be filled in my maze design.
		for (int x=0; x<mazeSize[0]-1; x++) {
			for (int y=0; y<mazeSize[1]-1; y++) {
				for (int z=0; z<mazeSize[2]; z++) {
					if (columnChoices[x][y][z] == null) {
						throw new AssertionError();
					}
				}
			}
		}
		
		return columnChoices;
	}
	private static TetrisWallPiece[][][] calculateCentralCubes(int[] mazeSize, TetrisWallPiece[][][] centralColumns) {
		TetrisWallPiece[][][] centralCubes = new TetrisWallPiece[mazeSize[0]-1][mazeSize[1]-1][mazeSize[2]-1];
		for (int x=0; x<mazeSize[0]-1; x++) {
			for (int y=0; y<mazeSize[1]-1; y++) {
				for (int z=0; z<mazeSize[2]-1; z++) {
					centralCubes[x][y][z] = centralColumns[x][y][z];
				}
			}
		}
		return centralCubes;
	}
	private static Map<TetrisWallPiece, Collection<int[]>> groupInterstices(TetrisWallPiece[][][] interstices, Collection<TetrisWallPiece> tetrisPieces) {
		Map<TetrisWallPiece, Collection<int[]>> groups = new HashMap<>();
		for (TetrisWallPiece tetrisPiece : tetrisPieces) {
			groups.put(tetrisPiece, new ArrayList<int[]>());
		}
		for (int x=0; x<interstices.length; x++) {
			for (int y=0; y<interstices[x].length; y++) {
				for (int z=0; z<interstices[x][y].length; z++) {
					for (TetrisWallPiece tetrisPiece : tetrisPieces) {
						if (tetrisPiece == interstices[x][y][z]) {
							groups.get(tetrisPiece).add(new int[] {x, y, z});
						}
					}
				}
			}
		}
		return groups;
	}
	private static List<SchematicCellGrid> calculateInitialTetrisSchematics(
			int[] mazeSize,
			Collection<TetrisWallPiece> tetrisPieces,
			Map<TetrisWallPiece, Collection<int[]>> centralColumnGroups,
			Map<TetrisWallPiece, Collection<int[]>> centralCubeGroups,
			Collection<LayerPiece> layerPieces) {
		List<SchematicCellGrid> tetrisSchematics = new ArrayList<>();
		for (TetrisWallPiece tetrisPiece : tetrisPieces) {
			int minX = tetrisPiece.getMinimumXCoordinate(), maxX = tetrisPiece.getMaximumXCoordinate();
			int minY = tetrisPiece.getMinimumYCoordinate(), maxY = tetrisPiece.getMaximumYCoordinate();
			SchematicCellGrid tetrisSchematic = new SchematicCellGrid(
					maxX - minX + 1, maxY - minY + 1, false);
			// -- FACES --
			// Create basic piece shape.
			for (int x=minX; x<=maxX; x++) {
				for (int y=minY; y<=maxY; y++) {
					if (tetrisPiece.contains(new Coordinate(x, y))) {
						tetrisSchematic.setTileAndExtendNeighbors(
								x - minX, y - minY, true);
					}
				}
			}
			// -- EDGES --
			// Add central column(s).
			for (int[] centralColumn : centralColumnGroups.get(tetrisPiece)) {
				Coordinate localCoord = tetrisPiece.locationToCoordinate(centralColumn);
				tetrisSchematic.setEdge(localCoord.x - minX, localCoord.y - minY, new Direction(0, true), SchematicCell.SOLID);
			}
			// Add perforations on left and right sides if they are against the walls.
			if (minX == tetrisPiece.locationToCoordinate(new int[] {0, 0, 0}).x) {
				for (int y=minY; y<=maxY; y++) {
					if (tetrisPiece.contains(new Coordinate(minX, y))) {
						tetrisSchematic.setEdge(0, y - minY, new Direction(0, false), SchematicCell.PERF);
					}
				}
			}
			// Add perforations (or tabs) on all bottom and top faces if they intersect a layer piece perforation
			// (or hole).
			for (Coordinate coord : tetrisPiece) {
				for (Direction dir : new Direction[] {new Direction(1, false), new Direction(1, true)}) {
					Coordinate offsetCoord = coord.getIncrement(dir);
					if (!tetrisPiece.contains(offsetCoord)) {
						// If dir.isPositive(), then this is at the top. Otherwise, it's at the bottom.
						int[] location = dir.isPositive() ?
								tetrisPiece.coordinateToLocation(coord) :
								tetrisPiece.coordinateToLocation(offsetCoord);
						LayerPiece layerPiece = null;
						for (LayerPiece potentialLayerPiece : layerPieces) {
							if (potentialLayerPiece.z == location[2]) {
								layerPiece = potentialLayerPiece;
							}
						}
						if (layerPiece == null) throw new AssertionError();
						Coordinate negativeTile = new Coordinate(location[0], location[1]);
						Coordinate positiveTile = negativeTile.getIncrement(new Direction(tetrisPiece.getHorizontalNormalDimension(), true));
						if (layerPiece.getHole(negativeTile.x, negativeTile.y) && layerPiece.getHole(positiveTile.x, positiveTile.y)) {
							tetrisSchematic.setEdge(coord.x - minX, coord.y - minY, dir, SchematicCell.SOLID);
						}
						else {
							tetrisSchematic.setEdge(coord.x - minX, coord.y - minY, dir, SchematicCell.PERF);
						}
					}
				}
			}
			// -- VERTICES --
			// Add central cube(s).
			for (int[] centralCube : centralCubeGroups.get(tetrisPiece)) {
				Coordinate localCoord = tetrisPiece.locationToCoordinate(centralCube);
				tetrisSchematic.setCorner(localCoord.x - minX, localCoord.y - minY, new Direction(0, true), SchematicCell.SOLID, true);
			}
			if (maxX == tetrisPiece.locationToCoordinate(mazeSize).x - 1) {
				for (int y=minY; y<=maxY; y++) {
					if (tetrisPiece.contains(new Coordinate(maxX, y))) {
						tetrisSchematic.setEdge(maxX - minX, y - minY, new Direction(0, true), SchematicCell.PERF);
					}
				}
			}
			tetrisSchematics.add(tetrisSchematic);
		}
		return tetrisSchematics;
	}
	private static List<SchematicCellGrid> calculateInitialLayerSchematics(
			int[] mazeSize,
			Collection<LayerPiece> layerPieces,
			Collection<SidePiece> sidePieces) {
		
		SidePiece leftSidePiece = null, rightSidePiece = null,
				frontSidePiece = null, backSidePiece = null;
		for (SidePiece sidePiece : sidePieces) {
			if (sidePiece.normalDirection.equals(new Direction(0, false))) leftSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(0, true))) rightSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(1, false))) frontSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(1, true))) backSidePiece = sidePiece;
		}
		if (leftSidePiece == null || rightSidePiece == null || frontSidePiece == null || backSidePiece == null)
			throw new AssertionError();
		
		List<SchematicCellGrid> layerSchematics = new ArrayList<>();
		for (LayerPiece layerPiece : layerPieces) {
			boolean isTopOrBottom = layerPiece.z == -1 || layerPiece.z == mazeSize[2] - 1;
			boolean isTop = layerPiece.z == mazeSize[2] - 1;//, isBottom = layerPiece.z == -1;
			int cellIndex = isTop ? mazeSize[2] - 1 : 0; // only valid if isTopOrBottom == true
			
			SchematicCellGrid layerSchematic = new SchematicCellGrid(layerPiece.width, layerPiece.depth, true);
			// The F-E-V order is a bit mixed up here, so that the ExtendNeighbors routine can overwrite the
			// tetris piece perforations.
			// -- EDGES --
			// Add perforations and slots for tetris pieces.
			for (Direction d : Direction.getPositiveDirections(2)) {
				for (int x=0; x < layerPiece.width - (d.getDimension() == 0 ? 1 : 0); x++) {
					for (int y=0; y < layerPiece.depth - (d.getDimension() == 1 ? 1 : 0); y++) {
						if (layerPiece.getPerforation(x, y, d) == Perforation.PERF) {
							layerSchematic.setEdge(x, y, d, SchematicCell.PERF);
						}
						else if (layerPiece.getPerforation(x, y, d) == Perforation.SLOT) {
							layerSchematic.setEdge(x, y, d, SchematicCell.HOLE);
						}
					}
				}
			}
			// Perforate supported edges. On the top and bottom layer pieces, if the adjacent
			// side piece has a hole, a tab instead of a perforation should be added.
			for (int x=0; x<layerPiece.width; x++) {
				if (!layerPiece.getHole(x, 0)) {
					if (isTopOrBottom && frontSidePiece.getHoles().contains(new Coordinate(x, cellIndex)))
						layerSchematic.setEdge(x, 0, new Direction(1, false), SchematicCell.SOLID);
					else
						layerSchematic.setEdge(x, 0, new Direction(1, false), SchematicCell.PERF);
				}
				if (!layerPiece.getHole(x, mazeSize[1] - 1)) {
					if (isTopOrBottom && backSidePiece.getHoles().contains(new Coordinate(x, cellIndex)))
						layerSchematic.setEdge(x, mazeSize[1] - 1, new Direction(1, true), SchematicCell.SOLID);
					else
						layerSchematic.setEdge(x, mazeSize[1] - 1, new Direction(1, true), SchematicCell.PERF);
				}
			}
			for (int y=0; y<layerPiece.depth; y++) {
				if (!layerPiece.getHole(0, y)) {
					if (isTopOrBottom && leftSidePiece.getHoles().contains(new Coordinate(y, cellIndex)))
						layerSchematic.setEdge(0, y, new Direction(0, false), SchematicCell.SOLID);
					else
						layerSchematic.setEdge(0, y, new Direction(0, false), SchematicCell.PERF);
				}
				if (!layerPiece.getHole(mazeSize[0] - 1, y)) {
					if (isTopOrBottom && rightSidePiece.getHoles().contains(new Coordinate(y, cellIndex)))
						layerSchematic.setEdge(mazeSize[0] - 1, y, new Direction(0, true), SchematicCell.SOLID);
					else
						layerSchematic.setEdge(mazeSize[0] - 1, y, new Direction(0, true), SchematicCell.PERF);
				}
			}
			// -- FACES --
			// Remove holes for vertical passages.
			for (int x=0; x<layerPiece.width; x++) {
				for (int y=0; y<layerPiece.depth; y++) {
					if (layerPiece.getHole(x, y)) {
						layerSchematic.setTileAndExtendNeighbors(x, y, false);
					}
				}
			}
			// -- VERTICES --
			// Remove central cubes (for middle layer pieces only).
			if (layerPiece.z != -1 && layerPiece.z != mazeSize[2] - 1) {
				for (int x=0; x<layerPiece.width-1; x++) {
					for (int y=0; y<layerPiece.depth-1; y++) {
						layerSchematic.setCorner(x, y, new Direction(0, true), SchematicCell.HOLE, true);
					}
				}
			}
			// Cubes that lie along the edge lines will be maintained in the bottom and top layer pieces,
			// but not in the others.
			if (layerPiece.z != -1 && layerPiece.z != mazeSize[2] - 1) {
				for (int x=0; x<layerPiece.width; x++) {
					layerSchematic.setCorner(x, 0, new Direction(1, false), SchematicCell.HOLE, false);
					layerSchematic.setCorner(x, 0, new Direction(1, false), SchematicCell.HOLE, true);
					layerSchematic.setCorner(x, mazeSize[1] - 1, new Direction(1, true), SchematicCell.HOLE, false);
					layerSchematic.setCorner(x, mazeSize[1] - 1, new Direction(1, true), SchematicCell.HOLE, true);
				}
				for (int y=0; y<layerPiece.depth; y++) {
					layerSchematic.setCorner(0, y, new Direction(0, false), SchematicCell.HOLE, false);
					layerSchematic.setCorner(0, y, new Direction(0, false), SchematicCell.HOLE, true);
					layerSchematic.setCorner(mazeSize[0] - 1, y, new Direction(0, true), SchematicCell.HOLE, false);
					layerSchematic.setCorner(mazeSize[0] - 1, y, new Direction(0, true), SchematicCell.HOLE, true);
				}
			}
			// Corners are special cases.
			if (layerPiece.z == -1 || layerPiece.z == mazeSize[2] - 1) {
				//                                                      Ah -- the infamous opposite-corner asymmetry --
				layerSchematic.setCorner(0, 0, new Direction(0, false), layerPiece.z == -1 ? SchematicCell.HOLE : SchematicCell.SOLID, false);
				layerSchematic.setCorner(mazeSize[0] - 1, 0, new Direction(0, true), SchematicCell.HOLE, false);
				layerSchematic.setCorner(0, mazeSize[1] - 1, new Direction(0, false), SchematicCell.SOLID, true);
				layerSchematic.setCorner(mazeSize[0] - 1, mazeSize[1] - 1, new Direction(0, true), layerPiece.z == -1 ? SchematicCell.SOLID : SchematicCell.HOLE, true);
			}
			else {
				layerSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.HOLE, false);
				layerSchematic.setCorner(mazeSize[0] - 1, 0, new Direction(0, true), SchematicCell.HOLE, false);
				layerSchematic.setCorner(0, mazeSize[1] - 1, new Direction(0, false), SchematicCell.HOLE, true);
				layerSchematic.setCorner(mazeSize[0] - 1, mazeSize[1] - 1, new Direction(0, true), SchematicCell.HOLE, true);
			}
//			// Trim off the corners if there are inconvenient holes.
//			if (layerPiece.getHole(0, 0)) layerSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.HOLE, false);
//			if (layerPiece.getHole(mazeSize[0] - 1, 0)) layerSchematic.setCorner(mazeSize[0] - 1, 0, new Direction(0, true), SchematicCell.HOLE, false);
//			if (layerPiece.getHole(0, mazeSize[1] - 1)) layerSchematic.setCorner(0, mazeSize[1] - 1, new Direction(0, false), SchematicCell.HOLE, true);
//			if (layerPiece.getHole(mazeSize[0] - 1, mazeSize[1] - 1)) layerSchematic.setCorner(mazeSize[0] - 1, mazeSize[1] - 1, new Direction(0, true), SchematicCell.HOLE, true);
//			// If a corner has been trimmed in one of the side pieces, we have a missing cube. We'll fill it in on the adjacent layer piece.
//			if (isBottom) {
//				if (leftSidePiece.getHoles().contains(new Coordinate(0, 0)) || frontSidePiece.getHoles().contains(new Coordinate(0, 0)))
//					layerSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.SOLID, false);
//				if (rightSidePiece.getHoles().contains(new Coordinate(0, 0)) || frontSidePiece.getHoles().contains(new Coordinate(frontSidePiece.width - 1, 0)))
//					layerSchematic.setCorner(layerPiece.width - 1, 0, new Direction(0, true), SchematicCell.SOLID, false);
//				if (leftSidePiece.getHoles().contains(new Coordinate(leftSidePiece.width - 1, 0)) || backSidePiece.getHoles().contains(new Coordinate(0, 0)))
//					layerSchematic.setCorner(0, layerPiece.depth - 1, new Direction(0, false), SchematicCell.SOLID, true);
//				if (rightSidePiece.getHoles().contains(new Coordinate(rightSidePiece.width - 1, 0)) || backSidePiece.getHoles().contains(new Coordinate(backSidePiece.width - 1, 0)))
//					layerSchematic.setCorner(layerPiece.width - 1, layerPiece.depth - 1, new Direction(0, true), SchematicCell.SOLID, true);
//			}
//			if (isTop) {
//				if (leftSidePiece.getHoles().contains(new Coordinate(0, leftSidePiece.height - 1)) || frontSidePiece.getHoles().contains(new Coordinate(0, frontSidePiece.height - 1)))
//					layerSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.SOLID, false);
//				if (rightSidePiece.getHoles().contains(new Coordinate(0, rightSidePiece.height - 1)) || frontSidePiece.getHoles().contains(new Coordinate(frontSidePiece.width - 1, frontSidePiece.height - 1)))
//					layerSchematic.setCorner(layerPiece.width - 1, 0, new Direction(0, true), SchematicCell.SOLID, false);
//				if (leftSidePiece.getHoles().contains(new Coordinate(leftSidePiece.width - 1, leftSidePiece.height - 1)) || backSidePiece.getHoles().contains(new Coordinate(0, backSidePiece.height - 1)))
//					layerSchematic.setCorner(0, layerPiece.depth - 1, new Direction(0, false), SchematicCell.SOLID, true);
//				if (rightSidePiece.getHoles().contains(new Coordinate(rightSidePiece.width - 1, rightSidePiece.height - 1)) || backSidePiece.getHoles().contains(new Coordinate(backSidePiece.width - 1, backSidePiece.height - 1)))
//					layerSchematic.setCorner(layerPiece.width - 1, layerPiece.depth - 1, new Direction(0, true), SchematicCell.SOLID, true);
//			}
			
			layerSchematics.add(layerSchematic);
		}
		return layerSchematics;
	}
	private static List<SchematicCellGrid> calculateInitialSideSchematics(
			int[] mazeSize,
			Collection<LayerPiece> layerPieces,
			Collection<SidePiece> sidePieces) {
		
		SidePiece leftSidePiece = null, rightSidePiece = null,
				frontSidePiece = null, backSidePiece = null;
		for (SidePiece sidePiece : sidePieces) {
			if (sidePiece.normalDirection.equals(new Direction(0, false))) leftSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(0, true))) rightSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(1, false))) frontSidePiece = sidePiece;
			else if (sidePiece.normalDirection.equals(new Direction(1, true))) backSidePiece = sidePiece;
		}
		if (leftSidePiece == null || rightSidePiece == null || frontSidePiece == null || backSidePiece == null)
			throw new AssertionError();
		LayerPiece topLayerPiece = null, bottomLayerPiece = null;
		for (LayerPiece layerPiece : layerPieces) {
			if (layerPiece.z == -1) bottomLayerPiece = layerPiece;
			else if (layerPiece.z == mazeSize[2] - 1) topLayerPiece = layerPiece;
		}
		if (topLayerPiece == null || bottomLayerPiece == null)
			throw new AssertionError();
		
		List<SchematicCellGrid> sideSchematics = new ArrayList<>();
		for (SidePiece sidePiece : sidePieces) {
			SchematicCellGrid sideSchematic = new SchematicCellGrid(
					sidePiece.width, sidePiece.height, true);
			// The F-E-V order is a bit mixed up here, so that the ExtendNeighbors routine can overwrite the
			// tetris piece perforations.
			// -- EDGES --
			// Add perforations for layer pieces and tetris pieces.
			for (Direction d : Direction.getPositiveDirections(2)) {
				for (int x=0; x < sidePiece.width - (d.getDimension() == 0 ? 1 : 0); x++) {
					for (int y=0; y < sidePiece.height - (d.getDimension() == 1 ? 1 : 0); y++) {
						if (sidePiece.getPerforation(x, y, d)) {
							sideSchematic.setEdge(x, y, d, SchematicCell.PERF);
						}
					}
				}
			}
			// Add perforations for the left and right sides. (Unless there is a hole in the next side
			// piece, in which case we of course must add a tab instead.)
			{ // left
				SidePiece nextPiece = null;
				if (sidePiece == frontSidePiece || sidePiece == backSidePiece) nextPiece = leftSidePiece;
				if (sidePiece == leftSidePiece || sidePiece == rightSidePiece) nextPiece = frontSidePiece;
				if (nextPiece == null) throw new AssertionError();
				int nextX = -1;
				if (sidePiece == frontSidePiece || sidePiece == leftSidePiece) nextX = 0;
				else if (sidePiece == backSidePiece || sidePiece == rightSidePiece) nextX = nextPiece.width - 1;
				if (nextX == -1) throw new AssertionError();
				
				for (int y=0; y<sidePiece.height; y++) {
					if (nextPiece.getHoles().contains(new Coordinate(nextX, y))) {
						sideSchematic.setEdge(0, y, new Direction(0, false), SchematicCell.SOLID);
					}
					else
						sideSchematic.setEdge(0, y, new Direction(0, false), SchematicCell.PERF);
				}
			}
			{ // right
				SidePiece nextPiece = null;
				if (sidePiece == frontSidePiece || sidePiece == backSidePiece) nextPiece = rightSidePiece;
				if (sidePiece == leftSidePiece || sidePiece == rightSidePiece) nextPiece = backSidePiece;
				if (nextPiece == null) throw new AssertionError();
				int nextX = -1;
				if (sidePiece == frontSidePiece || sidePiece == leftSidePiece) nextX = 0;
				else if (sidePiece == backSidePiece || sidePiece == rightSidePiece) nextX = nextPiece.width - 1;
				if (nextX == -1) throw new AssertionError();
				
				for (int y=0; y<sidePiece.height; y++) {
					if (nextPiece.getHoles().contains(new Coordinate(nextX, y))) {
						sideSchematic.setEdge(sidePiece.width - 1, y, new Direction(0, true), SchematicCell.SOLID);
					}
					else
						sideSchematic.setEdge(sidePiece.width - 1, y, new Direction(0, true), SchematicCell.PERF);
				}
			}
			// Add perforations for the top and bottom sides. (Unless there are holes in the top or bottom
			// layer pieces, in which case we must of course add tabs instead.)
			{ // bottom
				for (int x=0; x<sidePiece.width; x++) {
					boolean isHole;
					if (sidePiece == frontSidePiece) isHole = bottomLayerPiece.getHole(x, 0);
					else if (sidePiece == backSidePiece) isHole = bottomLayerPiece.getHole(x, bottomLayerPiece.depth - 1);
					else if (sidePiece == leftSidePiece) isHole = bottomLayerPiece.getHole(0, x);
					else if (sidePiece == rightSidePiece) isHole = bottomLayerPiece.getHole(bottomLayerPiece.width - 1, x);
					else throw new AssertionError();
					if (isHole) {
						sideSchematic.setEdge(x, 0, new Direction(1, false), SchematicCell.SOLID);
					}
					else {
						sideSchematic.setEdge(x, 0, new Direction(1, false), SchematicCell.PERF);
					}
				}
			}
			{ // top
				for (int x=0; x<sidePiece.width; x++) {
					boolean isHole;
					if (sidePiece == frontSidePiece) isHole = topLayerPiece.getHole(x, 0);
					else if (sidePiece == backSidePiece) isHole = topLayerPiece.getHole(x, topLayerPiece.depth - 1);
					else if (sidePiece == leftSidePiece) isHole = topLayerPiece.getHole(0, x);
					else if (sidePiece == rightSidePiece) isHole = topLayerPiece.getHole(topLayerPiece.width - 1, x);
					else throw new AssertionError();
					if (isHole) {
						sideSchematic.setEdge(x, sidePiece.height - 1, new Direction(1, true), SchematicCell.SOLID);
					}
					else {
						sideSchematic.setEdge(x, sidePiece.height - 1, new Direction(1, true), SchematicCell.PERF);
					}
				}
			}
			// -- FACES --
			// Add hole(s) for entrance and/or exit.
			for (Coordinate hole : sidePiece.getHoles()) {
				sideSchematic.setTileAndExtendNeighbors(hole.x, hole.y, false);
			}
			// -- VERTICES --
			// Add holes at the edge-index cubes on the top and bottom edges (for all side pieces).
			for (int x=0; x<sidePiece.width; x++) {
				sideSchematic.setCorner(x, 0, new Direction(1, false), SchematicCell.HOLE, false);
				sideSchematic.setCorner(x, 0, new Direction(1, false), SchematicCell.HOLE, true);
				
				sideSchematic.setCorner(x, sidePiece.height - 1, new Direction(1, true), SchematicCell.HOLE, false);
				sideSchematic.setCorner(x, sidePiece.height - 1, new Direction(1, true), SchematicCell.HOLE, true);
			}
			// Add holes at the edge-index cubes on the left and right edges (for YZ pieces),
			// and solid cubes for XZ pieces.
			for (int y=0; y<sidePiece.height; y++) {
				SchematicCell cell = sidePiece.normalDirection.getDimension() == 0 ? SchematicCell.HOLE : SchematicCell.SOLID;
				sideSchematic.setCorner(0, y, new Direction(0, false), cell, false);
				sideSchematic.setCorner(0, y, new Direction(0, false), cell, true);
				
				sideSchematic.setCorner(sidePiece.width - 1, y, new Direction(0, true), cell, false);
				sideSchematic.setCorner(sidePiece.width - 1, y, new Direction(0, true), cell, true);
			}
			// The corners, as always, are special cases.
			if (sidePiece == frontSidePiece || sidePiece == backSidePiece) {
				sideSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.HOLE, false);
				sideSchematic.setCorner(sidePiece.width - 1, 0, new Direction(0, true), SchematicCell.HOLE, false);
				sideSchematic.setCorner(0, sidePiece.height - 1, new Direction(0, false), SchematicCell.HOLE, true);
				sideSchematic.setCorner(sidePiece.width - 1, sidePiece.height - 1, new Direction(0, true), SchematicCell.SOLID, true);
			}
			else if (sidePiece == leftSidePiece || sidePiece == rightSidePiece) {
				sideSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.SOLID, false);
				sideSchematic.setCorner(sidePiece.width - 1, 0, new Direction(0, true), SchematicCell.HOLE, false);
				sideSchematic.setCorner(0, sidePiece.height - 1, new Direction(0, false), SchematicCell.HOLE, true);
				sideSchematic.setCorner(sidePiece.width - 1, sidePiece.height - 1, new Direction(0, true), SchematicCell.HOLE, true);
			}
//			// Trim off the corners if there are inconvenient holes.
//			if (sidePiece.getHoles().contains(new Coordinate(0, 0)))
//				sideSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.HOLE, false);
//			if (sidePiece.getHoles().contains(new Coordinate(mazeSize[0] - 1, 0)))
//				sideSchematic.setCorner(mazeSize[0] - 1, 0, new Direction(0, true), SchematicCell.HOLE, false);
//			if (sidePiece.getHoles().contains(new Coordinate(0, mazeSize[1] - 1)))
//				sideSchematic.setCorner(0, mazeSize[1] - 1, new Direction(0, false), SchematicCell.HOLE, true);
//			if (sidePiece.getHoles().contains(new Coordinate(mazeSize[0] - 1, mazeSize[1] - 1)))
//				sideSchematic.setCorner(mazeSize[0] - 1, mazeSize[1] - 1, new Direction(0, true), SchematicCell.HOLE, true);
//			// If a corner has been trimmed in one of the layer pieces, we have a missing cube. We'll fill it in on one of the adjacent side pieces.
//			if (sidePiece.normalDirection.getDimension() == 1) {
//				if (sidePiece.normalDirection.isPositive()) {
//					if (bottomLayerPiece.getHole(0, bottomLayerPiece.depth - 1))
//						sideSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.SOLID, false);
//					if (bottomLayerPiece.getHole(bottomLayerPiece.width - 1, bottomLayerPiece.depth - 1))
//						sideSchematic.setCorner(sidePiece.width - 1, 0, new Direction(0, true), SchematicCell.SOLID, false);
//					if (topLayerPiece.getHole(0, topLayerPiece.depth - 1))
//						sideSchematic.setCorner(0, sidePiece.height - 1, new Direction(0, false), SchematicCell.SOLID, true);
//					if (topLayerPiece.getHole(topLayerPiece.width - 1, topLayerPiece.depth - 1))
//						sideSchematic.setCorner(sidePiece.width - 1, sidePiece.height - 1, new Direction(0, true), SchematicCell.SOLID, true);
//				}
//				else {
//					if (bottomLayerPiece.getHole(0, 0))
//						sideSchematic.setCorner(0, 0, new Direction(0, false), SchematicCell.SOLID, false);
//					if (bottomLayerPiece.getHole(bottomLayerPiece.width - 1, 0))
//						sideSchematic.setCorner(sidePiece.width - 1, 0, new Direction(0, true), SchematicCell.SOLID, false);
//					if (topLayerPiece.getHole(0, 0))
//						sideSchematic.setCorner(0, sidePiece.height - 1, new Direction(0, false), SchematicCell.SOLID, true);
//					if (topLayerPiece.getHole(topLayerPiece.width - 1, 0))
//						sideSchematic.setCorner(sidePiece.width - 1, sidePiece.height - 1, new Direction(0, true), SchematicCell.SOLID, true);
//				}
//			}
			
			sideSchematics.add(sideSchematic);
		}
		return sideSchematics;
	}
	private static void determineTetrisSchematicPerforations(
			Collection<SchematicCellGrid> tetrisSchematics, Collection<TetrisWallPiece> tetrisPieces) {
		for (Pair<SchematicCellGrid, TetrisWallPiece> pair : StreamUtil.zip(
				tetrisSchematics,
				tetrisPieces,
				(tetrisSchematic, tetrisPiece) -> new Pair<>(tetrisSchematic, tetrisPiece))) {
			SchematicCellGrid tetrisSchematic = pair.getFirst();
			TetrisWallPiece tetrisPiece = pair.getSecond();
			for (int x=0; x<tetrisSchematic.width; x++) {
				for (int y=0; y<tetrisSchematic.height; y++) {
					if (tetrisSchematic.getCell(x, y) != SchematicCell.PERF) continue;
					boolean horizontal = y % (Dimensions.tileSize() + 1) == 0;
					boolean vertical = x % (Dimensions.tileSize() + 1) == 0;
					if (horizontal ^ vertical) {
						if (horizontal) {
							if (tetrisPiece.isXZ()) {
								tetrisSchematic.setCell(x, y, x % 7 % 2 == 0 ? SchematicCell.SOLID : SchematicCell.HOLE);
							}
							else {
								tetrisSchematic.setCell(x, y, (x + 6) % 7 % 2 == 0 ? SchematicCell.SOLID : SchematicCell.HOLE);
							}
						}
						if (vertical) {
							if (tetrisPiece.isXZ()) {
								tetrisSchematic.setCell(x, y, (y + 6) % 7 % 2 == 0 ? SchematicCell.HOLE : SchematicCell.SOLID);
							}
							else {
								tetrisSchematic.setCell(x, y, y % 7 % 2 == 0 ? SchematicCell.HOLE : SchematicCell.SOLID);
							}
						}
					}
					else throw new AssertionError();
				}
			}
		}
	}
	private static void determineLayerSchematicPerforations(Collection<SchematicCellGrid> layerSchematics) {
		for (SchematicCellGrid layerSchematic : layerSchematics) {
			for (int x=0; x<layerSchematic.width; x++) {
				for (int y=0; y<layerSchematic.height; y++) {
					if (layerSchematic.getCell(x, y) != SchematicCell.PERF) continue;
					boolean horizontal = y % (Dimensions.tileSize() + 1) == 0;
					boolean vertical = x % (Dimensions.tileSize() + 1) == 0;
					if (horizontal ^ vertical) {
						if (horizontal) {
							layerSchematic.setCell(x, y, (x + 6) % 7 % 2 == 0 ? SchematicCell.SOLID : SchematicCell.HOLE);
						}
						if (vertical) {
							layerSchematic.setCell(x, y, y % 7 % 2 == 0 ? SchematicCell.SOLID : SchematicCell.HOLE);
						}
					}
					else throw new AssertionError();
				}
			}
		}
	}
	private static void determineSideSchematicPerforations(
			Collection<SidePiece> sidePieces, Collection<SchematicCellGrid> sideSchematics) {
		for (Pair<SidePiece, SchematicCellGrid> pair : StreamUtil.zip(
				sidePieces,
				sideSchematics,
				(sidePiece, sideSchematic) -> new Pair<>(sidePiece, sideSchematic))) {
			SidePiece sidePiece = pair.getFirst();
			SchematicCellGrid sideSchematic = pair.getSecond();
			for (int x=0; x<sideSchematic.width; x++) {
				for (int y=0; y<sideSchematic.height; y++) {
					if (sideSchematic.getCell(x, y) != SchematicCell.PERF) continue;
					boolean horizontal = y % (Dimensions.tileSize() + 1) == 0;
					boolean vertical = x % (Dimensions.tileSize() + 1) == 0;
					if (horizontal ^ vertical) {
						if (horizontal) {
							if (sidePiece.normalDirection.getDimension() == 0) {
								sideSchematic.setCell(x, y, x % 7 % 2 == 0 ? SchematicCell.HOLE : SchematicCell.SOLID);
							}
							else if (sidePiece.normalDirection.getDimension() == 1) {
								sideSchematic.setCell(x, y, x % 7 % 2 == 0 ? SchematicCell.SOLID : SchematicCell.HOLE);
							}
							else throw new AssertionError();
						}
						if (vertical) {
							if (sidePiece.normalDirection.getDimension() == 0) {
								sideSchematic.setCell(x, y, y % 7 % 2 == 0 ? SchematicCell.HOLE : SchematicCell.SOLID);
							}
							else if (sidePiece.normalDirection.getDimension() == 1) {
								sideSchematic.setCell(x, y, y % 7 % 2 == 0 ? SchematicCell.SOLID : SchematicCell.HOLE);
							}
							else throw new AssertionError();
						}
					}
					else throw new AssertionError();
				}
			}
		}
	}
	
	// laser-cut schematic generation
	public static LaserCutSchematicSet generateLaserCutSchematics(SchematicSet schematicSet, boolean eliminateMidpoints, boolean doOutput) {
		List<LaserCutSchematic> tetrisLaserCutSchematics = new ArrayList<>(),
				layerLaserCutSchematics = new ArrayList<>(),
				sideLaserCutSchematics = new ArrayList<>();
		
		if (doOutput) System.out.print("Tracing schematics... ");
		schematicSet.tetrisSchematics.stream().map(SchematicCellGrid::getLaserCutSchematic).forEach(tetrisLaserCutSchematics::add);
		schematicSet.layerSchematics.stream().map(SchematicCellGrid::getLaserCutSchematic).forEach(layerLaserCutSchematics::add);
		schematicSet.sideSchematics.stream().map(SchematicCellGrid::getLaserCutSchematic).forEach(sideLaserCutSchematics::add);
		if (doOutput) System.out.println("Done.");
		
		if (eliminateMidpoints) {
			if (doOutput) System.out.print("Eliminating midpoints... ");
			tetrisLaserCutSchematics.forEach(LaserCutSchematic::eliminateMidpoints);
			layerLaserCutSchematics.forEach(LaserCutSchematic::eliminateMidpoints);
			sideLaserCutSchematics.forEach(LaserCutSchematic::eliminateMidpoints);
			if (doOutput) System.out.println("Done.");
		}
		
		return new LaserCutSchematicSet(
				schematicSet,
				tetrisLaserCutSchematics,
				layerLaserCutSchematics,
				sideLaserCutSchematics);
	}
	
}
