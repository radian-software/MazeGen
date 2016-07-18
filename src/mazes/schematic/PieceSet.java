package mazes.schematic;

import java.util.List;

public final class PieceSet {
	
	public final List<TetrisWallPiece> tetrisPieces;
	public final List<LayerPiece> layerPieces;
	public final List<SidePiece> sidePieces;
	
	public PieceSet(List<TetrisWallPiece> tetrisPieces2, List<LayerPiece> layerPieces2, List<SidePiece> sidePieces) {
		this.tetrisPieces = tetrisPieces2;
		this.layerPieces = layerPieces2;
		this.sidePieces = sidePieces;
	}
	
}
