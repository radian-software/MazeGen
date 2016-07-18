package mazes.schematic;


public final class Dimensions {
	
	private static final int tileSize = 6; // number of cells across the interior of a maze passageway
	private static final int margin = 2; // number of cells allowed on each side of the SVG document
	private static final double cellWidth = 0.125; // side length of a cell, in inches
	private static final double laserLineWidth = 0.001; // SVG line thickness, in inches
	private static final double debugLineWidth = 0.01;
	// Epilog laser: 18 in by 12 in
	private static final int documentWidth = 8 * 24; // document width in cells (documentWidthInInches * 1/cellWidth)
	private static final int documentHeight = 8 * 19; // document height in cells (documentHeightInInches * 1/cellWidth)
	private static final int fontSize = 4; // font size for piece number labels
	
	public static int tileSize() {
		return Dimensions.tileSize;
	}
	public static int margin() {
		return margin;
	}
	public static int margins() {
		return margin() * 2;
	}
	public static double cellWidth() {
		return cellWidth;
	}
	public static double laserLineWidth() {
		return laserLineWidth;
	}
	public static double debugLineWidth() {
		return debugLineWidth;
	}
	public static int documentWidth() {
		return documentWidth;
	}
	public static int documentHeight() {
		return documentHeight;
	}
	public static int fontSize() {
		return fontSize;
	}
	
	public static int tileToCell(int tile) {
		return tile * (tileSize + 1);
	}
	public static int cellToTile(int cell) {
		return cell / (tileSize + 1);
	}
	
}
