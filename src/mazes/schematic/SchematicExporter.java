/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.List;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.svg.SVGDocument;
import mazes.util.MazeIO;
import mazes.util.MultiDimensionalArray;

public final class SchematicExporter {
	
	public static void main(String[] args) {
		System.out.print("Enter maze name: ");
		String mazeName = MazeIO.scanner.nextLine();
		boolean valid = true;
		GrowingTreeMazeGenerator gen = MazeIO.loadMaze(mazeName + ".chmz", false);
		if (gen == null) {
			gen = MazeIO.loadMaze(mazeName + ".maze", true);
			valid = false;
		}
		if (gen == null) {
			return;
		}
		MultiDimensionalArray<CellWalls> maze = gen.getMaze();
		try {
			PieceSet pieceSet = SchematicGenerator.generatePieces(maze, true, !valid);
			SchematicSet schematicSet = SchematicGenerator.generateSchematics(maze.getSideLengths(), pieceSet, true, !valid);
			SchematicChecker.checkSchematics(maze, schematicSet, true, !valid);
			LaserCutSchematicSet laserCutSchematicSet = SchematicGenerator.generateLaserCutSchematics(schematicSet, false, true);
			System.out.print("Determining SVG piece layout... ");
			List<SVGDocument> documents = laserCutSchematicSet.toSVGDocuments();
			System.out.print("Done.\nSaving SVG files... ");
			if (MazeIO.saveSVG(documents, maze.getSideLengths(), mazeName, true)) {
				System.out.println("Done.");
			}
		}
		catch (SchematicException e) {
			System.out.println("Schematic generation failed: " + e.getMessage());
			System.exit(0);
		}
		
		if (!valid) System.out.println("[WARNING] Maze may be invalid. Do not try to build it, please.");
	}
	
}
