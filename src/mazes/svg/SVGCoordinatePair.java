/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.svg;

import mazes.schematic.Coordinate;
import util.Pair;

public final class SVGCoordinatePair extends Pair<SVGCoordinate, SVGCoordinate> {
	
	public SVGCoordinatePair(SVGCoordinate first, SVGCoordinate second) {
		super(first, second, true);
	}
	public SVGCoordinatePair(Coordinate first, Coordinate second) {
		this(new SVGCoordinate(first), new SVGCoordinate(second));
	}
	
}
