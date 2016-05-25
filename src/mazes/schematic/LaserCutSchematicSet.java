/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.ArrayList;
import java.util.List;

import mazes.svg.SVGCoordinate;
import mazes.svg.SVGDocument;
import util.StreamUtil;
import util.Triplet;

public final class LaserCutSchematicSet {
	
	public final List<TetrisWallPiece> tetrisPieces;
	public final List<LayerPiece> layerPieces;
	public final List<SidePiece> sidePieces;
	public final List<SchematicCellGrid> tetrisSchematics, layerSchematics, sideSchematics;
	public final List<LaserCutSchematic> tetrisLaserCutSchematics, layerLaserCutSchematics, sideLaserCutSchematics;
	
	public LaserCutSchematicSet(SchematicSet schematicSet,
			List<LaserCutSchematic> tetrisLaserCutSchematics,
			List<LaserCutSchematic> layerLaserCutSchematics,
			List<LaserCutSchematic> sideLaserCutSchematics) {
		this.tetrisPieces = schematicSet.tetrisPieces;
		this.layerPieces = schematicSet.layerPieces;
		this.sidePieces = schematicSet.sidePieces;
		this.tetrisSchematics = schematicSet.tetrisSchematics;
		this.layerSchematics = schematicSet.layerSchematics;
		this.sideSchematics = schematicSet.sideSchematics;
		this.tetrisLaserCutSchematics = tetrisLaserCutSchematics;
		this.layerLaserCutSchematics = layerLaserCutSchematics;
		this.sideLaserCutSchematics = sideLaserCutSchematics;
	}
	
	public List<SVGDocument> toSVGDocuments() {
		int documentWidth = Dimensions.documentWidth(), documentHeight = Dimensions.documentHeight();
		List<SVGDocument> documents = new ArrayList<>();
		List<boolean[][]> spaceTakens = new ArrayList<>();
		for (Triplet<Piece, SchematicCellGrid, LaserCutSchematic> triplet : StreamUtil.zip(
				StreamUtil.concat(layerPieces, sidePieces, tetrisPieces),
				StreamUtil.concat(layerSchematics, sideSchematics, tetrisSchematics),
				StreamUtil.concat(layerLaserCutSchematics, sideLaserCutSchematics, tetrisLaserCutSchematics),
				(piece, schematic, laserCutSchematic) -> new Triplet<>(piece, schematic, laserCutSchematic))) {
			// Get information about the piece we are to place.
			Piece piece = triplet.getFirst();
			SchematicCellGrid schematic = triplet.getSecond();
			LaserCutSchematic laserCutSchematic = triplet.getThird();
			if (schematic.width > documentWidth - Dimensions.margins() || schematic.height > documentHeight - Dimensions.margins()) throw new IllegalArgumentException("piece does not fit on material sheet");
			// See if we can place the piece on an existing document.
			boolean couldPlace = false;
			for (int i=0; i<documents.size(); i++) {
				SVGDocument document = documents.get(i);
				boolean[][] spaceTaken = spaceTakens.get(i);
				if (addToDocumentIfPossible(piece, schematic, laserCutSchematic, document, spaceTaken)) {
					couldPlace = true;
					break;
				}
			}
			if (!couldPlace) {
				SVGDocument document = new SVGDocument(new SVGCoordinate(documentWidth, documentHeight));
				boolean[][] spaceTaken = new boolean[documentWidth][documentHeight];
				if (!addToDocumentIfPossible(piece, schematic, laserCutSchematic, document, spaceTaken))
					throw new AssertionError();
				documents.add(document);
				spaceTakens.add(spaceTaken);
			}
		}
		return documents;
	}
	private boolean addToDocumentIfPossible(Piece piece, SchematicCellGrid schematic, LaserCutSchematic laserCutSchematic, SVGDocument document, boolean[][] spaceTaken) {
		int documentWidth = document.size().x, documentHeight = document.size().y;
		int margin = Dimensions.margin();
		int margins = Dimensions.margins();
		// Pick a location for the upper-left corner of the piece.
		for (int X = margin; X <= documentWidth - schematic.width - margins; X++) {
			for (int Y = margin; Y <= documentHeight - schematic.height - margins; Y++) {
				boolean isValid = true;
				checkingValidityOfPiecePlacement:
					for (int x=0; x<schematic.width; x++) {
						for (int y=0; y<schematic.height; y++) {
							if (schematic.getCell(x, y) == SchematicCell.SOLID && spaceTaken[X + x][Y + y]) {
								isValid = false;
								break checkingValidityOfPiecePlacement;
							}
						}
					}
				if (isValid) {
					// Note the space that this piece takes up.
					for (int y=0; y<schematic.height; y++) {
						for (int x=0; x<schematic.width; x++) {
							if (schematic.getCell(x, y) == SchematicCell.SOLID) {
								spaceTaken[X + x][Y + y] = true;
							}
						}
					}
					// Place the piece.
					document.addSchematic(piece, schematic, laserCutSchematic, X, Y);
					return true;
				}
			}
		}
		return false;
	}
	
}
