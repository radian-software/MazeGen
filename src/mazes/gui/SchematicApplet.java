package mazes.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JApplet;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.schematic.Blueprint;
import mazes.schematic.Colors;
import mazes.schematic.LaserCutSchematicSet;
import mazes.schematic.PieceSet;
import mazes.schematic.SchematicChecker;
import mazes.schematic.SchematicException;
import mazes.schematic.SchematicGenerator;
import mazes.schematic.SchematicSet;
import mazes.util.MazeIO;
import mazes.util.MultiDimensionalArray;

@SuppressWarnings("serial")
public final class SchematicApplet extends JApplet {
	
	private final HashMap<String, List<Blueprint>> blueprintLists;
	private final int[] scaleFactors;
	private final boolean[] reverseDirection;
	private final int windowBuffer;
	
	private final Colors colors;
	
	private final int resizeDelayMS;
	private final boolean dimUnselected;
	
	public SchematicApplet() {
		System.out.print("Enter maze name: ");
		String mazeName = MazeIO.scanner.nextLine();
		boolean valid = true;
		GrowingTreeMazeGenerator mazeGenerator = MazeIO.loadMaze(mazeName + ".chmz", true); // temporary change
		if (mazeGenerator == null) {
			mazeGenerator = MazeIO.loadMaze(mazeName + ".maze", true);
			valid = false;
		}
		if (mazeGenerator == null) {
			System.exit(0);
			throw new AssertionError();
		}
		MultiDimensionalArray<CellWalls> maze = mazeGenerator.getMaze();
		int[] mazeSize = maze.getSideLengths();
		
		this.colors = new Colors(Color.BLACK, Color.GRAY);
		
		LaserCutSchematicSet laserCutSchematicSet;
		try {
			PieceSet pieceSet = SchematicGenerator.generatePieces(maze, true, !valid);
			SchematicSet schematicSet = SchematicGenerator.generateSchematics(mazeSize, pieceSet, true, !valid);
			SchematicChecker.checkSchematics(maze, schematicSet, true, !valid);
			laserCutSchematicSet = SchematicGenerator.generateLaserCutSchematics(schematicSet, true, true);
			SchematicGenerator.countPoorlySupportedPieces(pieceSet.tetrisPieces, pieceSet.layerPieces);
		}
		catch (SchematicException e) {
			System.out.println("Schematic generation failed with error:");
			e.printStackTrace(System.out);
			System.exit(0);
			throw new AssertionError();
		}
		
		List<Blueprint> tetrisBlueprints = new ArrayList<>();
		for (int i=0; i<laserCutSchematicSet.tetrisPieces.size(); i++) {
			tetrisBlueprints.add(laserCutSchematicSet.tetrisLaserCutSchematics.get(i).getBlueprint(laserCutSchematicSet.tetrisPieces.get(i), colors));
		}
		List<Blueprint> layerBlueprints = new ArrayList<>();
		for (int i=0; i<laserCutSchematicSet.layerPieces.size(); i++) {
			layerBlueprints.add(laserCutSchematicSet.layerLaserCutSchematics.get(i).getBlueprint(laserCutSchematicSet.layerPieces.get(i), colors));
		}
		List<Blueprint> sideBlueprints = new ArrayList<>();
		for (int i=0; i<laserCutSchematicSet.sidePieces.size(); i++) {
			sideBlueprints.add(laserCutSchematicSet.sideLaserCutSchematics.get(i).getBlueprint(laserCutSchematicSet.sidePieces.get(i), colors));
		}
		
		HashMap<String, List<Blueprint>> blueprintLists = new HashMap<>();
		blueprintLists.put("R", tetrisBlueprints);
		blueprintLists.put("T", tetrisBlueprints);
		blueprintLists.put("Y", tetrisBlueprints);
		blueprintLists.put("J", layerBlueprints);
		blueprintLists.put("K", layerBlueprints);
		blueprintLists.put("L", layerBlueprints);
		blueprintLists.put("A", sideBlueprints);
		blueprintLists.put("S", sideBlueprints);
		blueprintLists.put("D", sideBlueprints);
		
		this.blueprintLists = blueprintLists;
		this.scaleFactors = new int[] {10, 0, -8, -5, 0, -10};
		this.reverseDirection = new boolean[] {false, false, false};
		this.windowBuffer = 100;
		this.resizeDelayMS = -1;
		this.dimUnselected = true;
		
		if (!valid) System.out.println("[WARNING] Maze may be invalid. Do not try to build it, please.");
	}
	
	@Override public void init() {
		BlueprintPanel panel = new BlueprintPanel(blueprintLists, scaleFactors, reverseDirection, windowBuffer, this, resizeDelayMS, dimUnselected, "GVBN");
		setPreferredSize(new Dimension(panel.getWidth(), panel.getHeight()));
		setSize(getPreferredSize());
		getContentPane().add(panel);
		((Frame) getParent().getParent()).setTitle("BlueprintRender 3D");
	}
	
}
