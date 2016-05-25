/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.gen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mazes.util.Direction;
import mazes.util.MultiDimensionalArray;
import mazes.util.VectorUtil;
import util.MathUtil;

public final class GrowingTreeMazeGenerator implements Serializable {
	
	@SuppressWarnings("serial")
	public static final class Seed implements Serializable {
		
		private final long seed;
		private final float randomness;
		private final byte size;
		
		private final String seedString;
		
		public Seed(long seed, float randomness, byte size) {
			this.seed = seed;
			this.randomness = randomness;
			this.size = size;
			
			this.seedString = MathUtil.toHexString(seed) + MathUtil.toHexString(randomness) + MathUtil.toHexString(size);
		}
		public Seed(String seedString) {
			this.seedString = seedString;
			
			if (seedString.length() != 26) throw new NumberFormatException("Seed string must be exactly 28 characters.");
			this.seed = MathUtil.parseLong(seedString.substring(0, 16), 16);
			this.randomness = Float.intBitsToFloat(MathUtil.parseInt(seedString.substring(16, 24), 16));
			this.size = MathUtil.parseByte(seedString.substring(24, 26), 16);
		}
		
		public long getSeed() {
			return seed;
		}
		public float getRandomness() {
			return randomness;
		}
		public byte getSize() {
			return size;
		}
		public String getSeedString() {
			return seedString;
		}
	}
	
	public static final long serialVersionUID = -3271334171441858266L;
	
	// Generation attributes
	private final int[] sideLengths;
	private final int dimensionCount;
	// 0 = always choose newest
	// 1 = always choose random
	private final double randomness;
	
	// Algorithm instance variables
	private final Seed seed;
	private final Random random;
	private final List<int[]> potentialCells;
	private final MultiDimensionalArray<Boolean> alreadyVisited;
	private final MultiDimensionalArray<Integer> distanceFromStem;
	private final MultiDimensionalArray<CellWalls> maze;
	
	private int[] entrance;
	private int[] exit;
	
	public GrowingTreeMazeGenerator(int[] sideLengths, float randomness, int[] entrance, long seed) {
		if (sideLengths == null || sideLengths.length < 2) throw new IllegalArgumentException();
		this.sideLengths = sideLengths;
		this.dimensionCount = sideLengths.length;
		this.randomness = randomness;
		if (sideLengths.length == 3 && sideLengths[0] == sideLengths[1] && sideLengths[1] == sideLengths[2]) {
			this.seed = new Seed(seed, randomness, (byte)sideLengths[0]);
		}
		else {
			this.seed = null;
		}
		this.random = new Random(seed);
		
		this.potentialCells = new ArrayList<int[]>();
		this.alreadyVisited = new MultiDimensionalArray<Boolean>(sideLengths);
		alreadyVisited.fill(false); // otherwise the array will be filled with null
		this.distanceFromStem = new MultiDimensionalArray<Integer>(sideLengths);
		this.maze = new MultiDimensionalArray<CellWalls>(sideLengths);
		maze.fill(() -> new CellWalls(dimensionCount, true));
		
		// Initialize arrays with first cell
		if (!maze.isInteriorOrBorder(entrance)) {
			throw new IndexOutOfBoundsException();
		}
		else {
			// if (maze.isBorder(entrance)) {
			if (!maze.isInterior(entrance)) {
				Direction entranceDirection = maze.getDirectionIntoGrid(entrance);
				this.entrance = entranceDirection.getIncrement(entrance);
				// entrance is the original location (outside the maze)
				// this.entrance is the nearest location inside the maze
				// There is only one copy of each edge wall, so only one removal is needed
				maze.get(this.entrance).setWall(entranceDirection.getOpposite(), false);
			}
			else {
				// both entrance and this.entrance are inside the maze
				this.entrance = entrance;
			}
			potentialCells.add(this.entrance);
			alreadyVisited.set(this.entrance, true);
			distanceFromStem.set(this.entrance, 0);
		}
	}
	
	public static int[] getEntrance(byte mazeSize, Random random) {
		int side = random.nextInt(3); // {0, 1, 2} x {false, true} = 6 possibilities, one for each side
		boolean reverseSide = random.nextBoolean();
		int[] entrance = new int[3];
		for (int i=0; i<3; i++) {
			if (i == side) {
				entrance[i] = reverseSide ? mazeSize : -1;
			}
			else {
				entrance[i] = random.nextInt(mazeSize);
			}
		}
		return entrance;
	}
	
	public static GrowingTreeMazeGenerator generate3DMaze(Seed seed) {
		Random random = new Random(seed.getSeed());
		
		byte mazeSize = seed.getSize();
		GrowingTreeMazeGenerator gen;
		do {
			int[] entrance = getEntrance(mazeSize, random);
			gen = new GrowingTreeMazeGenerator(new int[] {mazeSize, mazeSize, mazeSize}, seed.getRandomness(), entrance, random.nextLong());
			while (!gen.isFinished()) {
				gen.performIteration();
			}
		}
		while (gen.entranceExitAdjacent());
		return gen;
	}
	public static GrowingTreeMazeGenerator generate2DMaze(int mazeSize, float randomness, long seed) {
		return new GrowingTreeMazeGenerator(new int[] {mazeSize, mazeSize}, randomness, new int[] {-1, 0}, seed);
	}
	public static GrowingTreeMazeGenerator generate4DMaze(int mazeSize, float randomness, long seed) {
		return new GrowingTreeMazeGenerator(new int[] {mazeSize, mazeSize, mazeSize, mazeSize}, randomness, new int[] {-1, 0, 0, 0}, seed);
	}
	private boolean entranceExitAdjacent() {
		int[] adjEntrance = entrance;
		try {
			adjEntrance = VectorUtil.sumVectors(adjEntrance, maze.getDirectionIntoGrid(adjEntrance).getOffsets(3));
		}
		catch (IllegalArgumentException e) {}
		int[] adjExit = exit;
		try {
			adjExit = VectorUtil.sumVectors(adjExit, maze.getDirectionIntoGrid(adjExit).getOffsets(3));
		}
		catch (IllegalArgumentException e) {}
		int[] difference = VectorUtil.subtractVectors(adjEntrance, adjExit);
		VectorUtil.forEach(difference, component -> Math.abs(component));
		return VectorUtil.countFilter(difference, component -> component != 0) <= 1;
	}
	public void performIteration() {
		if (isFinished()) return;
		int baseCellIndex;
		if (random.nextDouble() < randomness) {
			// Choose newest cell
			baseCellIndex = potentialCells.size()-1;
		}
		else {
			// Choose random cell
			baseCellIndex = random.nextInt(potentialCells.size());
		}
		int[] baseCell = potentialCells.get(baseCellIndex);
		// Pick a direction
		List<Direction> possibleBranchDirections = new ArrayList<>();
		List<int[]> possibleBranchCells = new ArrayList<>();
		for (Direction potentialDirection : Direction.getDirections(dimensionCount)) {
			int[] targetCell = potentialDirection.getIncrement(baseCell);
			if (maze.isInterior(targetCell) && !alreadyVisited.get(targetCell)) {
				possibleBranchDirections.add(potentialDirection);
				possibleBranchCells.add(targetCell);
			}
		}
		if (possibleBranchDirections.size() == 0) {
			// Turns out this cell is boxed in; no longer consider it
			potentialCells.remove(baseCellIndex);
		}
		else {
			// Pick a random (valid) direction and destroy the walls in that direction
			int randomIndex = random.nextInt(possibleBranchDirections.size());
			Direction branchDirection = possibleBranchDirections.get(randomIndex);
			int[] branchCell = possibleBranchCells.get(randomIndex);
			maze.get(baseCell).setWall(branchDirection, false);
			maze.get(branchCell).setWall(branchDirection.getOpposite(), false);
			potentialCells.add(branchCell);
			alreadyVisited.set(branchCell, true);
			distanceFromStem.set(branchCell, distanceFromStem.get(baseCell) + 1);
		}
		
		if (isFinished()) {
			// Locate the exit (only once; after this, performIteration will
			// terminate immediately.)
			List<int[]> borderCells = MultiDimensionalArray.getBorderIndexList(sideLengths);
			int[] farthestCell = null;
			int farthestDistance = 0;
			for (int[] borderCell : borderCells) {
				if (distanceFromStem.get(borderCell) >= farthestDistance) {
					farthestCell = borderCell;
					farthestDistance = distanceFromStem.get(borderCell);
				}
			}
			if (farthestCell == null) throw new AssertionError();
			this.exit = farthestCell;
			// Make a physical exit at this location
			maze.get(this.exit).setWall(maze.getDirectionOutOfGrid(this.exit), false);
		}
	}
	
	public boolean isFinished() {
		return potentialCells.size() == 0;
	}
	public List<int[]> getPotentialCells() {
		return potentialCells;
	}
	public MultiDimensionalArray<Boolean> getAlreadyVisited() {
		return alreadyVisited;
	}
	public MultiDimensionalArray<CellWalls> getMaze() {
		return maze;
	}
	public int[] getEntrance() {
		return entrance;
	}
	public int[] getExit() {
		return exit;
	}
	public Seed getSeed() {
		return seed;
	}
	
}
