package mazes.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.util.Direction;
import mazes.util.MazeIO;
import mazes.util.MultiDimensionalArray;
import util.ArrObjPair;
import util.ArrayUtil;

@SuppressWarnings("serial")
public final class MazePanel3D extends JPanel {
	
	private final int gridSize;
	private int layerOffsetX, layerOffsetY;
	private final byte mazeSize;
	private final int ovalSize;
	private final int windowBuffer;
	private final int windowSizeX, windowSizeY;
	private final int lineWidth;
	private boolean swapOrientation = true;
	
	private GrowingTreeMazeGenerator gen;
	private long seed;
	
	private Collection<ArrObjPair<Integer, Direction>> solution;
	
	public MazePanel3D(byte mazeSize, int gridSize, int layerOffsetX, int layerOffsetY, int windowBuffer, int lineWidth) {
		this(System.currentTimeMillis(), mazeSize, gridSize, layerOffsetX, layerOffsetY, windowBuffer, lineWidth);
//		this(GrowingTreeMazeGenerator.generate3DMaze(new GrowingTreeMazeGenerator.Seed(System.currentTimeMillis(), 0.5f, mazeSize)),
//				gridSize, layerOffsetX, layerOffsetY, windowBuffer, lineWidth);
	}
	public MazePanel3D(long seed, byte mazeSize, int gridSize, int layerOffsetX, int layerOffsetY, int windowBuffer, int lineWidth) {
		this(new GrowingTreeMazeGenerator(
				new int[] {mazeSize, mazeSize, mazeSize},
				0.5f,
				GrowingTreeMazeGenerator.getEntrance(mazeSize, new Random(seed)),
				seed
				),
				gridSize, layerOffsetX, layerOffsetY, windowBuffer, lineWidth);
		this.seed = seed;
	}
	public MazePanel3D(GrowingTreeMazeGenerator maze, int gridSize, int layerOffsetX, int layerOffsetY, int windowBuffer, int lineWidth) {
		super();
		this.gridSize = gridSize;
		this.layerOffsetX = layerOffsetX;
		this.layerOffsetY = layerOffsetY;
		this.gen = maze;
		this.mazeSize = (byte) maze.getMaze().getSideLength(0);
		this.ovalSize = gridSize / 3;
		this.windowBuffer = windowBuffer;
		this.windowSizeX = (gridSize + layerOffsetX) * (mazeSize - 1) + ovalSize + windowBuffer;
		this.windowSizeY = (gridSize + layerOffsetY) * (mazeSize - 1) + ovalSize + windowBuffer;
		this.lineWidth = lineWidth;
		
		setPreferredSize(new Dimension(windowSizeX, windowSizeY));
		validate();
		setVisible(true);
		
		AbstractAction updateMazeGeneration = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				gen.performIteration();
				repaint();
			}
		};
		AbstractAction finishMazeGeneration = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				while (!gen.isFinished()) {
					gen.performIteration();
				}
				System.out.println("Finalized maze generator state.");
				repaint();
			}
		};
		AbstractAction resetMazeGeneration = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				gen = new GrowingTreeMazeGenerator(
						new int[] {mazeSize, mazeSize, mazeSize},
						0.5f,
						GrowingTreeMazeGenerator.getEntrance(mazeSize, new Random(seed)),
						seed);
				System.out.println("Reset maze generator state.");
				repaint();
			}
		};
		Timer timer = new Timer(10, updateMazeGeneration);
		
		// Press space to increment maze generation
		getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "SPACE");
		getActionMap().put("SPACE", updateMazeGeneration);
		
		// Press F to finish maze generation instantly
		getInputMap().put(KeyStroke.getKeyStroke("F"), "F");
		getActionMap().put("F", finishMazeGeneration);
		
		// Press R to reset maze generation
		getInputMap().put(KeyStroke.getKeyStroke("R"), "R");
		getActionMap().put("R", resetMazeGeneration);
		
		// Press A to animate generation of maze
		getInputMap().put(KeyStroke.getKeyStroke("A"), "A");
		getActionMap().put("A", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				if (timer.isRunning()) {
					System.out.println("Stopped maze generation animation.");
					timer.stop();
				}
				else {
					System.out.println("Began maze generation animation.");
					timer.start();
				}
			}
		});
		
		// Press S to save current maze generator status to a file
		getInputMap().put(KeyStroke.getKeyStroke("S"), "S");
		getActionMap().put("S", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				System.out.print("Enter maze name: ");
				String mazeName = MazeIO.scanner.nextLine();
				MazeIO.saveMaze(gen, mazeName + ".maze", true);
				System.out.println("Saved maze.");
			}
		});
		
		// Press L to load a file to the current maze generator
		getInputMap().put(KeyStroke.getKeyStroke("L"), "L");
		getActionMap().put("L", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				System.out.print("Enter maze name: ");
				String mazeName = MazeIO.scanner.nextLine();
				GrowingTreeMazeGenerator newGen = MazeIO.loadMaze(mazeName + ".chmz", false);
				if (newGen == null) newGen = MazeIO.loadMaze(mazeName + ".maze", true);
				if (newGen != null) {
					gen = newGen;
					try {
						seed = gen.getSeed().getSeed();
					}
					catch (NullPointerException e) {
						System.out.println("Sorry, this maze appears to be in an old format. Can't retrieve the seed.");
					}
					System.out.println("Loaded maze.");
					repaint();
				}
			}
		});
		
		// Use the arrow keys to change the layer offset
		getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "LEFT");
		getActionMap().put("LEFT", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				MazePanel3D.this.layerOffsetX -= 1;
				repaint();
			}
		});
		getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "RIGHT");
		getActionMap().put("RIGHT", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				MazePanel3D.this.layerOffsetX += 1;
				repaint();
			}
		});
		getInputMap().put(KeyStroke.getKeyStroke("UP"), "UP");
		getActionMap().put("UP", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				MazePanel3D.this.layerOffsetY -= 1;
				repaint();
			}
		});
		getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "DOWN");
		getActionMap().put("DOWN", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				MazePanel3D.this.layerOffsetY += 1;
				repaint();
			}
		});
		
		// Press O to switch orientation from right-down-out to right-out-up.
		getInputMap().put(KeyStroke.getKeyStroke("O"), "O");
		getActionMap().put("O", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				swapOrientation = !swapOrientation;
				System.out.println("Orientation is now " + (swapOrientation ? "normal" : "classic") + ".");
				repaint();
			}
		});
		
		// Press Q to generate a new random maze.
		getInputMap().put(KeyStroke.getKeyStroke("Q"), "Q");
		getActionMap().put("Q", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				MazePanel3D.this.seed = (int) System.currentTimeMillis();
				resetMazeGeneration.actionPerformed(null);
				while (!gen.isFinished()) {
					gen.performIteration();
				}
				System.out.println("Generated random maze.");
				repaint();
			}
		});
		
		// Press C to toggle display of solution.
		getInputMap().put(KeyStroke.getKeyStroke("C"), "C");
		getActionMap().put("C", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				if (gen.getExit() == null) {
					System.out.println("Maze generation is not complete. Solution cannot be displayed.");
				}
				else if (solution == null) {
					Integer[] entrance = ArrayUtil.box(gen.getEntrance());
					Integer[] exit = ArrayUtil.box(gen.getExit());
					MultiDimensionalArray<CellWalls> maze = gen.getMaze();
					Deque<ArrObjPair<Integer, Integer>> stack = new ArrayDeque<>();
					stack.addLast(new ArrObjPair<>(entrance, 0));
					while (true) {
//						System.out.println(stack);
						ArrObjPair<Integer, Integer> pair = stack.removeLast();
						Integer[] position = pair.getFirst();
						int index = pair.getSecond();
						Direction dir = new Direction(index);
						// If we've reached the exit, yay!
						if (Arrays.equals(position, exit)) {
//							System.out.println("Reached exit.");
							break;
						}
						// If we're out of bounds, backtrack.
						if (!maze.isValidIndex(ArrayUtil.unbox(position))) {
//							System.out.println("Out of bounds. Backtracking.");
							continue;
						}
						// If we've looked at all the directions, backtrack.
						if (index >= 6) {
//							System.out.println("All directions examined. Backtracking.");
							continue;
						}
						// If we're looking backwards, just look at the next direction.
						if (!stack.isEmpty() && Arrays.equals(stack.getLast().getFirst(), dir.getIncrement(position))) {
//							System.out.println("Looking backwards. Checking next direction.");
							stack.addLast(new ArrObjPair<>(position, index + 1));
							continue;
						}
						// If we can advance onwards, do so. If we backtrack to here,
						// look at the next direction.
						if (!maze.get(ArrayUtil.unbox(position)).getWall(dir)) {
//							System.out.println("Found opening. Proceeding.");
							stack.addLast(new ArrObjPair<>(position, index + 1));
							stack.addLast(new ArrObjPair<>(dir.getIncrement(position), 0));
							continue;
						}
						// Otherwise, just look at the next direction.
						else {
//							System.out.println("Found wall. Checking next direction.");
							stack.addLast(new ArrObjPair<>(position, index + 1));
							continue;
						}
					}
					solution = new ArrayList<>();
					for (ArrObjPair<Integer, Integer> pair : stack) {
						Integer[] pos = pair.getFirst();
						int index = pair.getSecond();
						Direction dir = new Direction(index - 1);
						solution.add(new ArrObjPair<>(pos, dir));
						solution.add(new ArrObjPair<>(dir.getIncrement(pos), dir.getOpposite()));
					}
//					System.out.println("Computed solution:");
//					System.out.println(solution);
					System.out.println("Solution is now displayed.");
				}
				else {
					solution = null;
					System.out.println("Solution is no longer displayed.");
				}
				repaint();
			}
		});
		
		repaint();
	}
	
	@Override public void paintComponent(Graphics G) {
		Graphics2D g = (Graphics2D) G;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		MultiDimensionalArray<CellWalls> maze = gen.getMaze();
//		List<int[]> potentialCells = gen.getPotentialCells();
//		MultiDimensionalArray<Boolean> alreadyVisited = gen.getAlreadyVisited();
		int[] entrance = gen.getEntrance();
		int[] exit = gen.getExit();
		
		// Coordinate transformation: [0...cols/rows] -> [0...800]
		int[] xLengths = {
				-gridSize, gridSize,
				0, 0,
				-layerOffsetX, layerOffsetX
		};
		int[] yLengths = {
				0, 0,
				-gridSize, gridSize,
				-layerOffsetY, layerOffsetY
		};
		Color[] colorMapping = new Color[] {
				Color.RED, Color.ORANGE, Color.YELLOW,
				Color.GREEN, Color.BLUE, Color.MAGENTA,
				Color.PINK
		};
		
		// The following code is absolutely horrible, because of Java's lack of
		// support for closures in lambdas. Le sigh.
		
		g.setColor(Color.GRAY);
		// Render x  -> xc
		//        -z -> yc
		//        -y -> zc
		// Iterate backwards if the orientation is swapped.
		// (So that the pieces in the back are drawn first.)
		int X, Y, Z;
		Consumer<int[]> renderLine = (coord) -> {
			int x = coord[0], y = coord[1], z = coord[2];
			for (int d=0; d<6; d++) {
				int dp = d;
				if (swapOrientation) {
					dp = (new int[] {0, 1, 5, 4, 3, 2})[d];
				}
				if (!maze.get(coord).getWall(new Direction(d))) {
					if (swapOrientation) {
						try {
							if (d == 3) g.setColor(colorMapping[((mazeSize-1-y)-1) % 7]);
							else g.setColor(colorMapping[(mazeSize-1-y) % 7]);
						}
						catch (ArrayIndexOutOfBoundsException e) {
							g.setColor(Color.GRAY);
						}
					}
					else {
						try {
							if (d == 4) g.setColor(colorMapping[(z-1) % 7]);
							else g.setColor(colorMapping[z % 7]);
						}
						catch (ArrayIndexOutOfBoundsException e) {
							g.setColor(Color.GRAY);
						}
					}
					if (solution != null && solution.contains(new ArrObjPair<>(ArrayUtil.box(coord), new Direction(d)))) {
						g.setColor(Color.BLACK);
					}
					// [x/y/z]p represents the rendering position. [x/y/z] represents the actual position.
					// We could set yp = -z to render z on what appears to be the y-axis, backwards.
					int xp = x,
							yp = swapOrientation ? maze.getSideLength(2)-1 - z : y,
							zp = swapOrientation ? maze.getSideLength(1)-1 - y : z;
					g.setStroke(new BasicStroke(lineWidth));
					g.drawLine(
							xp * gridSize + zp * layerOffsetX + ovalSize / 2 + windowBuffer / 2,
							yp * gridSize + zp * layerOffsetY + ovalSize / 2 + windowBuffer / 2,
							xp * gridSize + zp * layerOffsetX + xLengths[dp] + ovalSize / 2 + windowBuffer / 2,
							yp * gridSize + zp * layerOffsetY + yLengths[dp] + ovalSize / 2 + windowBuffer / 2
							);
				}
			}
		};
		if (swapOrientation) {
			for (Y=maze.getSideLength(1)-1; Y>-1; Y--) {
				for (X=0; X<maze.getSideLength(0); X++) {
					for (Z=maze.getSideLength(2)-1; Z>-1; Z--) {
						renderLine.accept(new int[] {X, Y, Z});
					}
				}
			}
		}
		else {
			for (Z=0; Z<maze.getSideLength(2); Z++) {
				for (X=0; X<maze.getSideLength(0); X++) {
					for (Y=0; Y<maze.getSideLength(1); Y++) {
						renderLine.accept(new int[] {X, Y, Z});
					}
				}
			}
		}
		
		int xp, yp, zp;
		g.setColor(Color.BLACK);
		X = entrance[0]; Y = entrance[1]; Z = entrance[2];
		xp = X; yp = swapOrientation ? maze.getSideLength(2)-1 - Z : Y; zp = swapOrientation ? maze.getSideLength(1)-1 - Y : Z;
		g.fillOval(
				xp * gridSize + zp * layerOffsetX + windowBuffer / 2,
				yp * gridSize + zp * layerOffsetY + windowBuffer / 2,
				ovalSize, ovalSize);
		if (exit != null) {
			X = exit[0]; Y = exit[1]; Z = exit[2];
			xp = X; yp = swapOrientation ? maze.getSideLength(2)-1 - Z : Y; zp = swapOrientation ? maze.getSideLength(1)-1 - Y : Z;
			g.fillOval(
					xp * gridSize + zp * layerOffsetX + windowBuffer / 2,
					yp * gridSize + zp * layerOffsetY + windowBuffer / 2,
					ovalSize, ovalSize);
		}
	}
	
}
