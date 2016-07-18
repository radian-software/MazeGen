package mazes.schematic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import mazes.util.Direction;

public interface Piece {
	
	static List<Piece> orderPieces(Collection<Piece> original, int[] mazeSize) {
		List<Piece> pieces = new ArrayList<>();
		Consumer<Predicate<Piece>> addMatching = predicate -> {
			original.stream().filter(predicate).forEach(pieces::add);
		};
		for (int z=-1; z<mazeSize[2]; z++) {
			final int Z = z;
			// Add a layer piece and all the tetris pieces that start on that layer.
			addMatching.accept(piece -> {
				return piece instanceof LayerPiece && ((LayerPiece) piece).z == Z;
			});
			for (int y=0; y<mazeSize[1]-1; y++) {
				final int Y = y;
				addMatching.accept(piece -> {
					return piece instanceof TetrisWallPiece &&
							((TetrisWallPiece) piece).isXZ() &&
							((TetrisWallPiece) piece).getMinimumZCoordinate() - 1 == Z &&
							((TetrisWallPiece) piece).getNormalOffset() == Y;
				});
			}
			for (int x=0; x<mazeSize[0]-1; x++) {
				final int X = x;
				addMatching.accept(piece -> {
					return piece instanceof TetrisWallPiece &&
							((TetrisWallPiece) piece).isYZ() &&
							((TetrisWallPiece) piece).getMinimumZCoordinate() - 1 == Z &&
							((TetrisWallPiece) piece).getNormalOffset() == X;
				});
			}
		}
		addMatching.accept(piece -> {
			return piece instanceof SidePiece;
		});
		if (pieces.size() != original.size() || !new HashSet<>(pieces).equals(new HashSet<>(original))) {
			throw new AssertionError();
		}
		return pieces;
	}
	
	int[] getSchematicGridCorner();
	Direction getXDirection();
	Direction getYDirection();
	Direction getZDirection();
	String getType();
	
}
