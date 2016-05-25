/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mazes.gen.CellWalls;
import mazes.gen.GrowingTreeMazeGenerator;
import mazes.svg.SVGDocument;
import mazes.util.MazeIO;
import mazes.util.MultiDimensionalArray;

public class BulkSchematicExporter {
	
	public static void main(String[] args) {
		List<String> mazeNames = IntStream.range(0, 10).mapToObj(num -> String.format("maze5_%03d", num)).collect(Collectors.toList());
		for (String mazeName : mazeNames) {
			GrowingTreeMazeGenerator gen = MazeIO.loadMaze(mazeName + ".chmz", true);
			if (gen == null) {
				return;
			}
			MultiDimensionalArray<CellWalls> maze = gen.getMaze();
			try {
				PieceSet pieceSet = SchematicGenerator.generatePieces(maze, false);
				SchematicSet schematicSet = SchematicGenerator.generateSchematics(maze.getSideLengths(), pieceSet, false);
				SchematicChecker.checkSchematics(maze, schematicSet, false);
				LaserCutSchematicSet laserCutSchematicSet = SchematicGenerator.generateLaserCutSchematics(schematicSet, false, false);
				List<SVGDocument> documents = laserCutSchematicSet.toSVGDocuments();
				if (MazeIO.saveSVG(documents, maze.getSideLengths(), mazeName, true)) {
					System.out.printf("Finished %s.%n", mazeName);
				}
				else {
					return;
				}
			}
			catch (SchematicException e) {
				System.out.println("Schematic generation failed: " + e.getMessage());
				return;
			}
		}
	}
	
}
