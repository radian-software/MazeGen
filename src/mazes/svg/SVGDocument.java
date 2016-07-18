package mazes.svg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mazes.schematic.Coordinate;
import mazes.schematic.Dimensions;
import mazes.schematic.LaserCutPath;
import mazes.schematic.LaserCutSchematic;
import mazes.schematic.Piece;
import mazes.schematic.SchematicCell;
import mazes.schematic.SchematicCellGrid;
import util.MathUtil;
import util.Quadruplet;
import util.StreamUtil;
import util.Triplet;

public final class SVGDocument extends SVGTag {
	
	private final SVGCoordinate size; public SVGCoordinate size() { return size; }
	private final Set<SVGCoordinatePair> lines;
	private final List<Quadruplet<Piece, SVGCoordinate, String, SchematicCellGrid>> documentation;
	private final List<SVGText> labels;
	private double lineWidth;
	public void setLineWidth(double lineWidth) {
		this.lineWidth = lineWidth;
	}
	
	public SVGDocument(SVGCoordinate size) {
		this.size = size;
		this.lines = new LinkedHashSet<>();
		this.documentation = new ArrayList<>();
		this.labels = new ArrayList<>();
	}
	
	public void addSchematic(Piece piece, SchematicCellGrid schematic, LaserCutSchematic laserCutSchematic, int x, int y) {
		String docString = String.format(
				"Lower-left corner at (%d, %d), or (%s in, %s in), or (%s in, %s in)%n"
				+ "%s%n"
				+ "%s",
				x, y,
				MathUtil.toEighthsDecimal(x), MathUtil.toEighthsDecimal(y),
				MathUtil.toEighthsFraction(x), MathUtil.toEighthsFraction(y),
				piece, schematic);
		documentation.add(new Quadruplet<>(piece, new SVGCoordinate(x, y), docString, schematic));
		
		LaserCutSchematic adjustedLaserCutSchematic = laserCutSchematic.moveBy(new Coordinate(x, y));
		for (LaserCutPath path : adjustedLaserCutSchematic) {
			StreamUtil.groupTwo(
					path,
					(p1, p2) -> new SVGCoordinatePair(p1, p2),
					true).forEach(lines::add);
		}
	}
	public void addLabels(List<SVGDocument> documents, int[] mazeSize) {
		List<Piece> keyOrderPieces = documents.stream().flatMap(
				document -> document
					.getDocumentation()
					.stream()
					.<Piece>map(Triplet::getFirst))
				.collect(Collectors.toList());
		List<Piece> assemblyOrderPieces = Piece.orderPieces(keyOrderPieces, mazeSize);
		List<Quadruplet<Piece, SVGCoordinate, String, SchematicCellGrid>> documentationCopy = new ArrayList<>(documentation);
		documentationCopy.sort((q1, q2) -> assemblyOrderPieces.indexOf(q1.getFirst()) - assemblyOrderPieces.indexOf(q2.getFirst()));
		for (Quadruplet<Piece, SVGCoordinate, String, SchematicCellGrid> quad : documentationCopy) {
			// Place a text label corresponding to the assembly order of the piece.
			// For a disjoint piece, place multiple labels.
			Piece piece = quad.getFirst();
			SVGCoordinate offset = quad.getSecond();
			SchematicCellGrid schematic = quad.getFourth();
			Collection<Set<SVGCoordinate>> schematicSections = new ArrayList<>();
			boolean[][] visited = new boolean[schematic.width][schematic.height];
			for (int X=0; X<schematic.width; X++) {
				for (int Y=0; Y<schematic.height; Y++) {
					if (!visited[X][Y] && schematic.getCell(X, Y) == SchematicCell.SOLID) {
						Set<SVGCoordinate> schematicSection = new HashSet<>();
						Deque<SVGCoordinate> stack = new ArrayDeque<>();
						stack.addLast(new SVGCoordinate(X, Y));
						while (!stack.isEmpty()) {
							SVGCoordinate current = stack.removeLast();
							int x = current.x, y = current.y;
							if (x >= 0 && y >= 0 && x < schematic.width && y < schematic.height && !visited[x][y] && schematic.getCell(x, y) == SchematicCell.SOLID) {
								schematicSection.add(current);
								visited[x][y] = true;
								stack.addLast(new SVGCoordinate(x-1, y));
								stack.addLast(new SVGCoordinate(x+1, y));
								stack.addLast(new SVGCoordinate(x, y-1));
								stack.addLast(new SVGCoordinate(x, y+1));
							}
						}
						schematicSections.add(schematicSection);
					}
				}
			}
			for (Set<SVGCoordinate> schematicSection : schematicSections) {
				boolean addedLabel =  false;
				for (SVGCoordinate coord : schematicSection) {
					int mod = Dimensions.tileSize() + 1;
					if (MathUtil.mod(coord.x, mod) == 2 && MathUtil.mod(coord.y, mod) == 2) {
						labels.add(new SVGText(new SVGCoordinate(offset.x + coord.x, offset.y + coord.y), String.valueOf(assemblyOrderPieces.indexOf(piece) + 1)));
						addedLabel = true;
						break;
					}
				}
				if (!addedLabel) throw new AssertionError();
			}
		}
	}
	
	public List<Quadruplet<Piece, SVGCoordinate, String, SchematicCellGrid>> getDocumentation() {
		return documentation;
	}
	
	// SVGTag
	@Override public String getTag() {
		return "svg";
	}
	@Override public Map<String, String> getAttributes() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("version", "1.1");
		map.put("width", String.valueOf(size.x * Dimensions.cellWidth()) + "in");
		map.put("height", String.valueOf(size.y * Dimensions.cellWidth()) + "in");
		map.put("viewBox", String.format("0 0 %d %d", size.x, size.y));
		map.put("xmlns", "http://www.w3.org/2000/svg");
		return map;
	}
	@Override public List<SVGElement> getChildElements() {
		if (lineWidth == 0) throw new IllegalStateException("lineWidth has not been set");
		List<SVGPolyline> polylines = new ArrayList<>();
		Set<SVGCoordinatePair> linesCopy = new LinkedHashSet<>(lines);
		// While there are lines remaining, stick them in polylines.
		while (linesCopy.size() > 0) {
			Iterator<SVGCoordinatePair> iterator = linesCopy.iterator();
			SVGCoordinatePair line;
			// Iterate through the lines, looking for one that can be added to an existing polyline.
			while (iterator.hasNext()) {
				line = iterator.next();
				for (SVGPolyline polyline : polylines) {
					boolean setAdded = true;
					if (line.getFirst().equals(polyline.getFirst())) {
						polyline.addFirst(line.getSecond());
					}
					else if (line.getFirst().equals(polyline.getLast())) {
						polyline.addLast(line.getSecond());
					}
					else if (line.getSecond().equals(polyline.getFirst())) {
						polyline.addFirst(line.getFirst());
					}
					else if (line.getSecond().equals(polyline.getLast())) {
						polyline.addLast(line.getFirst());
					}
					else {
						setAdded = false;
					}
					if (setAdded) {
						// If we find one, remove it from the set and start iterating from the beginning.
						linesCopy.remove(line);
						iterator = linesCopy.iterator();
						break;
					}
				}
			}
			if (linesCopy.size() > 0) {
				// If we run out of lines (i.e., none could be added to an existing polyline),
				// create a new polyline for the first (an arbitrary choice) line.
				// Remove it from the set. Then continue.
				line = linesCopy.iterator().next();
				SVGPolyline newPolyline = new SVGPolyline(lineWidth);
				newPolyline.addLast(line.getFirst());
				newPolyline.addLast(line.getSecond());
				polylines.add(newPolyline);
				linesCopy.remove(line);
			}
		}
		return StreamUtil.concat(polylines.stream(), labels.stream()).collect(Collectors.toList());
	}
	public List<String> getHeader() {
		return new ArrayList<>();
	}
	
	@Override public List<String> toSVGCode() {
		List<String> imageCode = super.toSVGCode();
		imageCode.addAll(0, getHeader());
		return imageCode;
	}
	
}
