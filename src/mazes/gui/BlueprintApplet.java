package mazes.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JApplet;

import mazes.gen.GrowingTreeMazeGenerator;
import mazes.schematic.Blueprint;
import mazes.schematic.Colors;
import mazes.schematic.LayerPiece;
import mazes.schematic.PieceSet;
import mazes.schematic.SchematicException;
import mazes.schematic.SchematicGenerator;
import mazes.schematic.SidePiece;
import mazes.schematic.TetrisWallPiece;
import mazes.schematic.XYZLine;
import mazes.util.MazeIO;

@SuppressWarnings("serial")
public final class BlueprintApplet extends JApplet {
	
	private final HashMap<String, List<Blueprint>> pieceLists;
	private final int[] scaleFactors;
	private final boolean[] reverseDirection;
	private final int windowBuffer;
	
	private static final Colors tetrisColors = new Colors(
			Color.BLACK, Color.GRAY);
	private static final Colors layerOutlineColors = new Colors(
			Color.GRAY, Color.LIGHT_GRAY);
	private static final Colors perforationColors = new Colors(
			Color.GREEN, new Color(192, 255, 192));
	private static final Colors slotColors = new Colors(
			Color.BLUE, new Color(192, 192, 255));
	private static final Colors minorErrorColors = new Colors(
			Color.ORANGE, new Color(255, 200, 128));
	private static final Colors majorErrorColors = new Colors(
			Color.RED, new Color(255, 128, 128));
	private static final Colors sideOutlineColors = new Colors(
			Color.GRAY, Color.LIGHT_GRAY);
	private static final Colors sidePerforationColors = new Colors(
			Color.CYAN, new Color(192, 255, 255));
	
	private static final int resizeDelayMS = -1;
	private static final boolean dimUnselected = false;
	
	public BlueprintApplet() {
		System.out.print("Enter maze name: ");
		String mazeName = MazeIO.scanner.nextLine();
		PieceSet pieceMap;
		boolean valid = true;
		try {
			GrowingTreeMazeGenerator gen = MazeIO.loadMaze(mazeName + ".chmz", false);
			if (gen == null) {
				gen = MazeIO.loadMaze(mazeName + ".maze", true);
				valid = false;
			}
			if (gen == null) {
				System.exit(0);
				throw new AssertionError();
			}
			pieceMap = SchematicGenerator.generatePieces(gen.getMaze(), true, !valid); // if not valid, override exceptions
		}
		catch (SchematicException e) {
			System.out.println("Blueprint generation failed with exception");
			e.printStackTrace(System.out);
			System.exit(0);
			throw new AssertionError();
		}
		
		List<TetrisWallPiece> tetrisPieces = pieceMap.tetrisPieces;
		List<LayerPiece> layerPieces = pieceMap.layerPieces;
		List<SidePiece> sidePieces = pieceMap.sidePieces;
		
		System.out.println("Generating TetrisWallPiece blueprints...");
		List<Blueprint> tetrisBlueprints = new ArrayList<>();
		tetrisPieces.forEach(tetrisPiece -> tetrisBlueprints.add(tetrisPiece.getBlueprint(tetrisColors)));
		
		System.out.println("Generating normal LayerPiece blueprints...");
		List<Blueprint> normalLayerBlueprints = new ArrayList<>();
		layerPieces.forEach(layerPiece -> normalLayerBlueprints.add(layerPiece.getBlueprint(layerOutlineColors, perforationColors, slotColors)));
		
		System.out.println("Generating outline LayerPiece blueprints...");
		List<Blueprint> layerOutlineBlueprints = new ArrayList<>();
		layerPieces.forEach(layerPiece -> layerOutlineBlueprints.add(layerPiece.getOutlinesBlueprint(layerOutlineColors)));
		
		System.out.println("Generating perforation LayerPiece blueprints...");
		List<Blueprint> perforationBlueprints = new ArrayList<>();
		layerPieces.forEach(layerPiece -> perforationBlueprints.add(layerPiece.getPerforationsBlueprint(perforationColors, slotColors)));
		
		System.out.println("Generating highlighted perforation LayerPiece blueprints...");
		List<Blueprint> perforationErrorBlueprints = new ArrayList<>();
		layerPieces.forEach(layerPiece -> perforationErrorBlueprints.add(layerPiece.getErrorHighlightedPerforations(perforationColors, slotColors, minorErrorColors, majorErrorColors)));
		
		System.out.println("Generating highlighted LayerPiece blueprints...");
		List<Blueprint> layerErrorBlueprints = new ArrayList<>();
		layerPieces.forEach(layerPiece -> layerErrorBlueprints.add(layerPiece.getErrorHighlightedBlueprint(layerOutlineColors, perforationColors, slotColors, minorErrorColors, majorErrorColors)));
		
		System.out.println("Generating composite TetrisWallPiece blueprints...");
		// Tetris piece outline + perforations and slots _on the plane of the piece_, with errors highlighted
		List<Blueprint> tetrisErrorBlueprints = new ArrayList<>();
		tetrisPieces.forEach(tetrisPiece -> {
			Blueprint tetrisBlueprint = tetrisPiece.getBlueprint(tetrisColors);
			int minimumZ = tetrisPiece.getMinimumZCoordinate() - 1;
			int maximumZ = tetrisPiece.getMaximumZCoordinate();
			for (Blueprint errorBlueprint : perforationErrorBlueprints) {
				for (XYZLine line : errorBlueprint) {
					if (line.z1 < minimumZ || line.z1 > maximumZ ||
							line.z2 < minimumZ || line.z2 > maximumZ)
						continue; // a few redundant checks, but it's clearer this way
					if (tetrisPiece.getHorizontalNormalDimension() == 0 &&
							tetrisPiece.getNormalOffset() == line.x1 &&
							tetrisPiece.getNormalOffset() == line.x2 ||
						tetrisPiece.getHorizontalNormalDimension() == 1 &&
							tetrisPiece.getNormalOffset() == line.y1 &&
							tetrisPiece.getNormalOffset() == line.y2) {
						tetrisBlueprint.add(line);
					}
				}
			}
			tetrisErrorBlueprints.add(tetrisBlueprint);
		});
		
		System.out.println("Generating SidePiece blueprints...");
		List<Blueprint> sidePieceBlueprints = new ArrayList<>();
		sidePieces.forEach(sidePiece -> sidePieceBlueprints.add(sidePiece.getBlueprint(sideOutlineColors, sidePerforationColors)));
		
		System.out.println("Finishing...");
		HashMap<String, List<Blueprint>> pieceLists = new HashMap<>();
		// Yay for understanding covariance!
		pieceLists.put("T", tetrisBlueprints);
		pieceLists.put("L", normalLayerBlueprints);
		pieceLists.put("O", layerOutlineBlueprints);
		pieceLists.put("P", perforationBlueprints);
		pieceLists.put("E", perforationErrorBlueprints);
		pieceLists.put("K", layerErrorBlueprints);
		pieceLists.put("C", tetrisErrorBlueprints);
		pieceLists.put("S", sidePieceBlueprints);
//		int[] scaleFactors = {10, 0, -8, -5, 0, -10};
//		int[] scaleFactors = {20, 0, -15, -9, 0, -20};
		int[] scaleFactors = {40, 0, -30, -18, 0, -40};
		boolean[] reverseDirection = {false, false, false};
		int windowBuffer = 100;
		
		this.pieceLists = pieceLists;
		this.scaleFactors = scaleFactors;
		this.reverseDirection = reverseDirection;
		this.windowBuffer = windowBuffer;
		
		if (!valid) System.out.println("[WARNING] Maze may be invalid. Do not try to build it, please.");
	}
	
	@Override public void init() {
		BlueprintPanel panel = new BlueprintPanel(pieceLists, scaleFactors, reverseDirection, windowBuffer, this, resizeDelayMS, dimUnselected, "GVBN");
		setPreferredSize(new Dimension(panel.getWidth(), panel.getHeight()));
		setSize(getPreferredSize());
		getContentPane().add(panel);
		((Frame) getParent().getParent()).setTitle("BlueprintRender 3D");
	}
	
}
