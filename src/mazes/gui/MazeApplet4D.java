/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.gui;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JApplet;

@SuppressWarnings("serial")
public final class MazeApplet4D extends JApplet {
	
	private final int gridSize = 60;
	private final int layerOffsetX = 45;
	private final int layerOffsetY = 27;
	private final int metalayerOffsetX = 27;
	private final int metalayerOffsetY = 45;
	private final int windowBuffer = 100;
	private final int mazeSize = 3;
	private final int lineWidth = 2;
	
	@Override public void init() {
		System.out.print("Initializing window... ");
		MazePanel4D panel = new MazePanel4D(gridSize, layerOffsetX, layerOffsetY, metalayerOffsetX, metalayerOffsetY, mazeSize, windowBuffer, lineWidth);
		setPreferredSize(new Dimension(panel.getWidth(), panel.getHeight()));
		setSize(getPreferredSize());
		getContentPane().add(panel);
		((Frame) getParent().getParent()).setTitle("MazeRender 4D");
	}
	
}
