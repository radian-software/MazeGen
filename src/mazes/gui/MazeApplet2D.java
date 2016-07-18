package mazes.gui;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JApplet;

@SuppressWarnings("serial")
public final class MazeApplet2D extends JApplet {
	
	private final int windowSize = 800;
	private final int mazeSize = 80;
	
	@Override public void init() {
		System.out.print("Initializing window... ");
		MazePanel2D panel = new MazePanel2D(windowSize, mazeSize);
		setPreferredSize(new Dimension(panel.getWidth(), panel.getHeight()));
		setSize(getPreferredSize());
		getContentPane().add(panel);
		((Frame) getParent().getParent()).setTitle("MazeRender 2D");
	}
	
}