/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.List;

public final class SchematicSet {
	
	public final List<TetrisWallPiece> tetrisPieces;
	public final List<LayerPiece> layerPieces;
	public final List<SidePiece> sidePieces;
	public final List<SchematicCellGrid> tetrisSchematics, layerSchematics, sideSchematics;
	
	public SchematicSet(List<TetrisWallPiece> tetrisPieces,
			List<LayerPiece> layerPieces,
			List<SidePiece> sidePieces,
			List<SchematicCellGrid> tetrisSchematics,
			List<SchematicCellGrid> layerSchematics,
			List<SchematicCellGrid> sideSchematics) {
		this.tetrisPieces = tetrisPieces;
		this.layerPieces = layerPieces;
		this.sidePieces = sidePieces;
		this.tetrisSchematics = tetrisSchematics;
		this.layerSchematics = layerSchematics;
		this.sideSchematics = sideSchematics;
	}
	
}
