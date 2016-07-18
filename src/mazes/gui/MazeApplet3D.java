package mazes.gui;

import java.awt.Frame;

import javax.swing.JApplet;

@SuppressWarnings("serial")
public final class MazeApplet3D extends JApplet {
	
//	private final int gridSize = 10;
//	private final int layerOffsetX = 8;
//	private final int layerOffsetY = 5;
	
//	private final int gridSize = 20;
//	private final int layerOffsetX = 15;
//	private final int layerOffsetY = 9;
	
//	private final int gridSize = 40;
//	private final int layerOffsetX = 30;
//	private final int layerOffsetY = 18;
	
	private int gridSize = 57;
	private int layerOffsetX = 47;
	private int layerOffsetY = 27;
	
	private int windowBuffer = 100;
	private byte mazeSize = 6;
	private int lineWidth = 2;
	
	@Override public void init() {
		MazePanel3D panel = new MazePanel3D(mazeSize, gridSize, layerOffsetX, layerOffsetY, windowBuffer, lineWidth);
		add(panel);
		validate();
		try {
			((Frame) getParent().getParent()).setTitle("MazeRender 3D");
		}
		catch (NullPointerException e) {}
	}
	
}
