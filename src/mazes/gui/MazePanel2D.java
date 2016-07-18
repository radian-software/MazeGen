package mazes.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.util.Direction;
import mazes.util.MultiDimensionalArray;

@SuppressWarnings("serial")
public final class MazePanel2D extends JPanel {
	
	private int windowSize;
	private int sideLength;
	
	private GrowingTreeMazeGenerator gen;
	private boolean hasStartedAnimation = false;
	private final long seed = System.currentTimeMillis();
	
	public MazePanel2D(int windowSize, int sideLength) {
		super();
		setPreferredSize(new Dimension(windowSize + 2, windowSize + 2));
		setSize(getPreferredSize());
		setVisible(true);
		
		this.windowSize = windowSize;
		this.sideLength = sideLength;
		
		AbstractAction updateMazeGeneration = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				if (!hasStartedAnimation) {
					resetMazeGen();
					hasStartedAnimation = true;
				}
				else {
					gen.performIteration();
				}
				repaint();
			}
		};
		Timer timer = new Timer(1, updateMazeGeneration);
		getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "SPACE");
		getActionMap().put("SPACE", updateMazeGeneration);
		getInputMap().put(KeyStroke.getKeyStroke("A"), "A");
		// Use closures to keep reference to timer
		getActionMap().put("A", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				if (timer.isRunning()) {
					timer.stop();
				}
				else {
					timer.start();
				}
			}
		});
		
		resetMazeGen();
		System.out.print("Generating " + (sideLength + "x" + sideLength) + " maze... ");
		while (!gen.isFinished()) {
			gen.performIteration();
		}
		System.out.println("Done.");
		repaint();
	}
	
	public void resetMazeGen() {
		gen = GrowingTreeMazeGenerator.generate2DMaze(sideLength, 0.5f, seed);
	}
	
	@Override public void paintComponent(Graphics G) {
		Graphics2D g = (Graphics2D) G;
//		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		MultiDimensionalArray<CellWalls> maze = gen.getMaze();
		List<int[]> potentialCells = gen.getPotentialCells();
		MultiDimensionalArray<Boolean> alreadyVisited = gen.getAlreadyVisited();
		int[] entrance = gen.getEntrance();
		int[] exit = gen.getExit();
		
		// Coordinate transformation: [0...cols/rows] -> [0...800]
		double xTransform = windowSize / maze.getSideLength(0);
		double yTransform = windowSize / maze.getSideLength(1);
		int[] xOffsets = {
				0, 1, // +x
				0, 0
		};
		int[] yOffsets = {
				0, 0,
				0, 1 // +y
		};
		int[] xLengths = {
				0, 0,
				1, 1
		};
		int[] yLengths = {
				1, 1,
				0, 0
		};
		for (int x=0; x<maze.getSideLength(0); x++) {
			for (int y=0; y<maze.getSideLength(1); y++) {
				int[] coord = new int[] {x, y};
				g.setColor(Color.BLACK);
				for (int d=0; d<4; d++) {
					if (maze.get(coord).getWall(new Direction(d))) {
						g.drawLine(
								(int)((x + xOffsets[d]) * xTransform),
								(int)((y + yOffsets[d]) * yTransform),
								(int)((x + xOffsets[d] + xLengths[d]) * xTransform),
								(int)((y + yOffsets[d] + yLengths[d]) * yTransform)
								);
					}
				}
				// not in potentialCells & not alreadyVisited -> GRAY
				// in potentialCells & alreadyVisited -> PINK
				// not in potentialCells & alreadyVisited -> WHITE
				boolean isPotentialCell = false;
				for (int[] potentialCell : potentialCells) {
					if (Arrays.equals(potentialCell, coord)) {
						isPotentialCell = true;
						break;
					}
				}
				boolean hasBeenVisited = alreadyVisited.get(coord);
				if (!isPotentialCell && !hasBeenVisited) {
					g.setColor(Color.GRAY);
					g.fillRect((int)(x * xTransform) + 1, (int)(y * yTransform) + 1,
							(int) xTransform, (int) yTransform);
				}
				if (isPotentialCell && hasBeenVisited) {
					g.setColor(Color.PINK);
					g.fillRect((int)(x * xTransform) + 1, (int)(y * yTransform) + 1,
							(int) xTransform, (int) yTransform);
				}
				// Background fill is white, so we don't have to do anything here
			}
		}
		int x, y;
		g.setColor(Color.GREEN);
		x = entrance[0]; y = entrance[1];
		g.fillRect((int)(x * xTransform) + 1, (int)(y * yTransform) + 1,
				(int) xTransform - 1, (int) yTransform - 1);
		if (exit != null) {
			g.setColor(Color.RED);
			x = exit[0]; y = exit[1];
			g.fillRect((int)(x * xTransform) + 1, (int)(y * yTransform) + 1,
					(int) xTransform - 1, (int) yTransform - 1);
		}
	}
	
}
