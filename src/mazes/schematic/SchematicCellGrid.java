package mazes.schematic;

import mazes.util.DeepCopyable;
import mazes.util.Direction;

public final class SchematicCellGrid implements DeepCopyable<SchematicCellGrid> {
	
	public final int widthInTiles, heightInTiles;
	public final int width, height;
	
	private final boolean[][] tiles;
	private final SchematicCell[][] cells;
	
	public SchematicCellGrid(int widthInTiles, int heightInTiles, boolean initToSolid) {
		this.widthInTiles = widthInTiles;
		this.heightInTiles = heightInTiles;
		this.width = (Dimensions.tileSize() + 1) * widthInTiles + 1;
		this.height = (Dimensions.tileSize() + 1) * heightInTiles + 1;
		this.cells = new SchematicCell[width][height];
		this.tiles = new boolean[widthInTiles][heightInTiles];
		for (int x=0; x<widthInTiles; x++) {
			for (int y=0; y<heightInTiles; y++) {
				tiles[x][y] = initToSolid;
			}
		}
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				cells[x][y] = initToSolid ? SchematicCell.SOLID : SchematicCell.HOLE;
			}
		}
	}
	private SchematicCellGrid(boolean[][] tiles, SchematicCell[][] cells) {
		this.widthInTiles = tiles.length;
		this.heightInTiles = tiles[0].length;
		this.width = cells.length;
		this.height = cells[0].length;
		this.tiles = tiles;
		this.cells = cells;
	}
	
	public SchematicCell getCell(int x, int y) {
		return cells[x][y];
	}
	public void setCell(int x, int y, SchematicCell value) {
		cells[x][y] = value;
	}
	
	public void setTile(int tileX, int tileY, boolean value) {
		tiles[tileX][tileY] = value;
		for (int x = (Dimensions.tileSize() + 1) * tileX + 1; x < (Dimensions.tileSize() + 1) * (tileX + 1); x++) {
			for (int y = (Dimensions.tileSize() + 1) * tileY + 1; y < (Dimensions.tileSize() + 1) * (tileY + 1); y++) {
				cells[x][y] = value ? SchematicCell.SOLID : SchematicCell.HOLE;
			}
		}
	}
	public void setTileAndExtendNeighbors(int tileX, int tileY, boolean value) {
		setTile(tileX, tileY, value);
		for (Direction d : Direction.getDirections(2)) {
			int newX = tileX + d.getOffset(0);
			int newY = tileY + d.getOffset(1);
			if (value) {
				if (newX >= 0 && newX < widthInTiles && newY >= 0 && newY < heightInTiles && tiles[newX][newY] == value) {
					setEdge(tileX, tileY, d, SchematicCell.SOLID);
				}
			}
			else {
				if (newX < 0 || newX >= widthInTiles || newY < 0 || newY >= heightInTiles || tiles[newX][newY] == value) {
					setEdge(tileX, tileY, d, SchematicCell.HOLE);
				}
			}
		}
	}
	
	public void setEdge(int tileX, int tileY, Direction side, SchematicCell value) {
		int[] bounds = getEdgeBounds(tileX, tileY, side);
		for (int x=bounds[0]; x<=bounds[1]; x++) {
			for (int y=bounds[2]; y<=bounds[3]; y++) {
				cells[x][y] = value;
			}
		}
	}
	public void setEdgeCorners(int tileX, int tileY, Direction side, SchematicCell value, SchematicCell check, boolean checkGood, boolean addForEdges) {
		// Decide whether to add corners, and if so when
		int[] bounds = getEdgeBounds(tileX, tileY, side);
		setCorner(bounds, side, false, value, check, checkGood, addForEdges);
		setCorner(bounds, side, true, value, check, checkGood, addForEdges);
	}
	public void setCorner(int tileX, int tileY, Direction side, SchematicCell value, boolean upper) {
		setCorner(getEdgeBounds(tileX, tileY, side), side, upper, value, null, false, true);
	}
	private void setCorner(int[] bounds, Direction side, boolean upper, SchematicCell value, SchematicCell check, boolean checkGood, boolean addForEdges) {
		switch (side.getDimension()) {
		case 0:
			if (!upper) {
				try {
					if ((cells[bounds[0]][bounds[2]-2] == check) == checkGood) {
						cells[bounds[0]][bounds[2]-1] = value;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					if (addForEdges)
						cells[bounds[0]][bounds[2]-1] = value;
				}
			}
			else {
				try {
					if ((cells[bounds[1]][bounds[3]+2] == check) == checkGood) {
						cells[bounds[1]][bounds[3]+1] = value;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					if (addForEdges)
						cells[bounds[1]][bounds[3]+1] = value;
				}
			}
			break;
		case 1:
			if (!upper) {
				try {
					if ((cells[bounds[0]-2][bounds[2]] == check) == checkGood) {
						cells[bounds[0]-1][bounds[2]] = value;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					if (addForEdges)
						cells[bounds[0]-1][bounds[2]] = value;
				}
			}
			else {
				try {
					if ((cells[bounds[1]+2][bounds[3]] == check) == checkGood) {
						cells[bounds[1]+1][bounds[3]] = value;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					if (addForEdges)
						cells[bounds[1]+1][bounds[3]] = value;
				}
			}
			break;
		default: throw new AssertionError();
		}
	}
	// Returns inclusive bounds
	private int[] getEdgeBounds(int tileX, int tileY, Direction side) {
		int lowerX = (Dimensions.tileSize() + 1) * tileX;
		int lowerY = (Dimensions.tileSize() + 1) * tileY;
		switch (side.getDimension()) {
		case 0:
			lowerY += 1;
			break;
		case 1:
			lowerX += 1;
			break;
		default: throw new AssertionError();
		}
		if (side.isPositive()) {
			switch (side.getDimension()) {
			case 0:
				lowerX += Dimensions.tileSize() + 1;
				break;
			case 1:
				lowerY += Dimensions.tileSize() + 1;
				break;
			default: throw new AssertionError();
			}
		}
		int upperX = lowerX, upperY = lowerY;
		switch (side.getDimension()) {
		case 0:
			upperY += Dimensions.tileSize() - 1;
			break;
		case 1:
			upperX += Dimensions.tileSize() - 1;
			break;
		default: throw new AssertionError();
		}
		return new int[] {lowerX, upperX, lowerY, upperY};
	}
	
	public void assertDetermined() {
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				if (cells[x][y] == SchematicCell.PERF) throw new AssertionError();
			}
		}
	}
	public LaserCutSchematic getLaserCutSchematic() {
		boolean[][] solid = new boolean[width][height];
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				if (cells[x][y] == SchematicCell.SOLID) solid[x][y] = true;
				else if (cells[x][y] == SchematicCell.HOLE) solid[x][y] = false;
				else throw new IllegalStateException("must determine perforations before converting to schematic");
			}
		}
		boolean[][][] filled = new boolean[width+1][height+1][4]; // init'd to false, one for each direction
		// In THIS case, the bijection is different from usual. The cell coordinate corresponds to
		// the coordinate of the intersection on its negative side.
		// (Well, actually it's consistent, but it doesn't look like it at first.)
		
		// We iterate in row-major order through the grid. This order ensures when we
		// hit a shape boundary it will be from the lower-left.
		
		LaserCutSchematic schematic = new LaserCutSchematic();
		// we don't iterate over the entire matrix here 'cause we shouldn't be
		// starting any paths in the highest row and column
		for (int Y=0; Y<height; Y++) {
			for (int X=0; X<width; X++) {
				boolean negativeOutline = !solid[X][Y] && (X != 0 && solid[X-1][Y]) && (Y != 0 && solid[X][Y-1]);
				boolean positiveOutline = solid[X][Y] && (X == 0 || !solid[X-1][Y]) && (Y == 0 || !solid[X][Y-1]);
				if (positiveOutline && negativeOutline) throw new AssertionError();
				if (positiveOutline || negativeOutline) {
//					System.out.println("TRACING:");
//					System.out.println(this);
//					System.out.println("Found contour to trace at (" + X + ", " + Y + ")");
					// Begin tracing operation, with X and Y the initial coordinates
					LaserCutPath newPath = new LaserCutPath();
					int x = X, y = Y;
					int dir = 1; // 0 = left, 1 = right, 2 = down, 3 = up
					// Trace outline until the initial point (or some other point -- but in theory it
					// should always be the initial point) is found again, thus forming a closed path.
//					System.out.printf("Tracing %s outline.%n", positiveOutline ? "positive" : "negative");
					boolean killMe = false;
					do {
//						System.out.println("Visited (" + x + ", " + y + ")");
						// Note that we have visited the current node (we do not want to start a new path here)
						filled[x][y][dir] = true;
						// Add the current node to the path (the do-while loop will have already checked that the path hasn't ended yet)
						newPath.addPoint(new Coordinate(x, y));
						// Check neighboring cells
						//  
						//      3   +
						//      |  
						//     D|C    A = [-1, -1]
						//  0 --+-- 1 B = [0, -1]
						//     A|B    C = [0, 0]
						//      |     D = [-1, 0]
						//  -   2
						//  
						boolean A, B, C, D; // is solid
						// I could be tracing along a border. This is fine. In this case,
						// we assume that all space outside the designated area is empty.
						// (Or full in the case of a negative outline.)
						try { A = solid[x-1][y-1]; } catch (ArrayIndexOutOfBoundsException e) { A = false; }
						try { B = solid[x][y-1]; } catch (ArrayIndexOutOfBoundsException e) { B = false; }
						try { C = solid[x][y]; } catch (ArrayIndexOutOfBoundsException e) { C = false; }
						try { D = solid[x-1][y]; } catch (ArrayIndexOutOfBoundsException e) { D = false; }
//						System.out.printf("A = %b, B = %b, C = %b, D = %b, dir = %d%n", A, B, C, D, dir);
						A ^= negativeOutline; B ^= negativeOutline;
						C ^= negativeOutline; D ^= negativeOutline;
						// Figure out which direction to go next
						switch (dir) {
						// We like to go counterclockwise if possible.
						case 0: // left, we must be on top and A and/or B must be solid
							if (!B && !A) {
								// Oh, ****! We're going the wrong way!
								killMe = true;
								break;
							}
							if (D) dir = 3;
							else if (A) dir = 0;
							else dir = 2;
							break;
						case 1: // right, we must be on the bottom and C and/or D must be solid
							if (!D && !C) {
								killMe = true;
								break;
							}
							if (B) dir = 2;
							else if (C) dir = 1;
							else dir = 3;
							break;
						case 2: // down, we must be on the left side and B and/or C must be solid
							if (!C && !B) {
								killMe = true;
								break;
							}
							if (A) dir = 0;
							else if (B) dir = 2;
							else dir = 1;
							break;
						case 3: // up, we must be on the right side and A and/or D must be solid
							if (!A && !D) {
								killMe = true;
								break;
							}
							if (C) dir = 1;
							else if (D) dir = 3;
							else dir = 0;
							break;
						default: throw new AssertionError();
						}
						// Go in that direction
						switch (dir) {
						case 0: // left
							x -= 1;
							break;
						case 1: // right
							x += 1;
							break;
						case 2: // down
							y -= 1;
							break;
						case 3: // up
							y += 1;
							break;
						default: throw new AssertionError();
						}
					}
					while (!(x == X && y == Y));
//					System.out.println("Ended at (" + x + ", " + y + ")");
					if (killMe) {
//						System.out.println("Killed.");
						// path was killed partway through
					}
					else {
//						System.out.println("Finished, adding path.");
						schematic.addPath(newPath);
					}
				}
			}
		}
		
//		System.out.println(this);
//		System.out.println(schematic.toCombinedString());
		return schematic;
	}
	
	@Override public SchematicCellGrid deepCopy() {
		boolean[][] newTiles = new boolean[widthInTiles][heightInTiles];
		for (int x=0; x<widthInTiles; x++) {
			for (int y=0; y<heightInTiles; y++) {
				newTiles[x][y] = tiles[x][y];
			}
		}
		SchematicCell[][] newCells = new SchematicCell[width][height];
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				newCells[x][y] = cells[x][y];
			}
		}
		return new SchematicCellGrid(newTiles, newCells);
	}
	
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y=height-1; y>-1; y--) {
			for (int x=0; x<width; x++) {
				switch (cells[x][y]) {
				case HOLE: sb.append('`'); break;
				case PERF: sb.append('•'); break;
				case SOLID: sb.append('%'); break;
				default: throw new AssertionError();
				}
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
}
