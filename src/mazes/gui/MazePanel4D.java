/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.util.Direction;
import mazes.util.MultiDimensionalArray;

@SuppressWarnings("serial")
public final class MazePanel4D extends JPanel {
	
	private int gridSize;
	private int layerOffsetX, layerOffsetY;
	private int metalayerOffsetX, metalayerOffsetY;
	private int mazeSize;
	private int ovalSize;
	private int windowBuffer;
	private int windowSize;
	private final int lineWidth;
	
	private GrowingTreeMazeGenerator gen;
	private boolean hasStartedAnimation = false;
	private final long seed = System.currentTimeMillis();
	
	public MazePanel4D(int gridSize, int layerOffsetX, int layerOffsetY, int metalayerOffsetX, int metalayerOffsetY, int mazeSize, int windowBuffer, int lineWidth) {
		super();
		this.gridSize = gridSize;
		this.layerOffsetX = layerOffsetX;
		this.layerOffsetY = layerOffsetY;
		this.metalayerOffsetX = metalayerOffsetX;
		this.metalayerOffsetY = metalayerOffsetY;
		this.mazeSize = mazeSize;
		this.ovalSize = gridSize / 3;
		this.windowBuffer = windowBuffer;
		this.windowSize = (gridSize + Math.max(layerOffsetX, layerOffsetY) + Math.max(metalayerOffsetX, metalayerOffsetY)) * (mazeSize - 1) + ovalSize + windowBuffer;
		this.lineWidth = lineWidth;
		
		setPreferredSize(new Dimension(windowSize, windowSize));
		setSize(getPreferredSize());
		setVisible(true);
		
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
		Timer timer = new Timer(10, updateMazeGeneration);
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
		System.out.print("Generating " + (mazeSize + "x" + mazeSize + "x" + mazeSize + "x" + mazeSize) + " maze... ");
		while (!gen.isFinished()) {
			gen.performIteration();
		}
		System.out.println("Done.");
		repaint();
	}
	
	public void resetMazeGen() {
		gen = GrowingTreeMazeGenerator.generate4DMaze(mazeSize, 0.5f, seed);
	}
	
	@Override public void paintComponent(Graphics G) {
		Graphics2D g = (Graphics2D) G;
//		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
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
				-layerOffsetX, layerOffsetX,
				-metalayerOffsetX, metalayerOffsetX
		};
		int[] yLengths = {
				0, 0,
				-gridSize, gridSize,
				-layerOffsetY, layerOffsetY,
				-metalayerOffsetY, metalayerOffsetY
		};
		Color[] colorMapping = new Color[] {
				Color.RED, Color.ORANGE, Color.YELLOW,
				Color.GREEN, Color.BLUE, Color.MAGENTA,
				Color.PINK
		};
		g.setColor(Color.GRAY);
		for (int w=0; w<maze.getSideLength(3); w++) {
			for (int z=0; z<maze.getSideLength(2); z++) {
				for (int x=0; x<maze.getSideLength(0); x++) {
					for (int y=0; y<maze.getSideLength(1); y++) {
						int[] coord = new int[] {x, y, z, w};
						for (int d=0; d<8; d++) {
							if (!maze.get(coord).getWall(new Direction(d))) {
								try {
									if (d == 4 || d == 6) g.setColor(colorMapping[(z+w-1) % 7]);
									else g.setColor(colorMapping[(z+w) % 7]);
								}
								catch (ArrayIndexOutOfBoundsException e) {
									g.setColor(Color.BLACK);
								}
								g.setStroke(new BasicStroke(lineWidth));
								g.drawLine(
										x * gridSize + z * layerOffsetX + w * metalayerOffsetX + ovalSize / 2 + windowBuffer / 2,
										y * gridSize + z * layerOffsetY + w * metalayerOffsetY + ovalSize / 2 + windowBuffer / 2,
										x * gridSize + z * layerOffsetX + w * metalayerOffsetX + xLengths[d] + ovalSize / 2 + windowBuffer / 2,
										y * gridSize + z * layerOffsetY + w * metalayerOffsetY + yLengths[d] + ovalSize / 2 + windowBuffer / 2
										);
							}
						}
					}
				}
			}
		}
		
		int x, y, z, w;
		g.setColor(Color.BLACK);
		x = entrance[0]; y = entrance[1]; z = entrance[2]; w = entrance[3];
		g.fillOval(
				x * gridSize + z * layerOffsetX + w * metalayerOffsetX + windowBuffer / 2,
				y * gridSize + z * layerOffsetY + w * metalayerOffsetY + windowBuffer / 2,
				ovalSize, ovalSize);
		if (exit != null) {
			x = exit[0]; y = exit[1]; z = exit[2]; w = exit[3];
			g.fillOval(
					x * gridSize + z * layerOffsetX + w * metalayerOffsetX + windowBuffer / 2,
					y * gridSize + z * layerOffsetY + w * metalayerOffsetY + windowBuffer / 2,
					ovalSize, ovalSize);
		}
	}
	
}
