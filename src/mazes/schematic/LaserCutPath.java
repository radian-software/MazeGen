/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class LaserCutPath implements Iterable<Coordinate> {
	
	// This class represents a single closed path. Multiple closed paths will form a LaserCutSchematic.
	
	private final List<Coordinate> points;
	
	public LaserCutPath() {
		this.points = new ArrayList<Coordinate>();
	}
	
	public void addPoint(Coordinate point) {
		points.add(Objects.requireNonNull(point));
	}
	
	// This method assumes that any two sequential points have either
	// the same x-coordinate, or the same y-coordinate, but not both
	public void eliminateMidpoints() {
		List<Coordinate> condensedPoints = new ArrayList<>();
		
		boolean collinearOnY;
		// Determine which way the first line segment is
		if (points.get(0).x == points.get(1).x) collinearOnY = false;
		else if (points.get(0).y == points.get(1).y) collinearOnY = true;
		else throw new AssertionError();
		
		// Start iterating through
		int index = 1;
		Coordinate lastPoint = points.get(0), currentPoint;
		do { // while (index < points.size() + 2)
			currentPoint = points.get(index % points.size());
			// Check which way the next line segment is
			boolean nowColinearOnY;
			if (currentPoint.x == lastPoint.x) nowColinearOnY = false;
			else if (currentPoint.y == lastPoint.y) nowColinearOnY = true;
			else throw new AssertionError();
			// If the line segments are not in the same direction...
			if (collinearOnY != nowColinearOnY) {
				// ... we have found a corner.
				condensedPoints.add(lastPoint);
				// Note that the first point added will be the first corner with an index greater than zero.
				// Also, the line segments connecting point 0 to the first corner found must be collinear.
			}
			// Update variables for the next go
			lastPoint = currentPoint;
			collinearOnY = nowColinearOnY;
			index += 1;
		}
		while (index < points.size() + 2);
		
		points.clear();
		points.addAll(condensedPoints); // final variable, oh well. It's not that inefficient...
	}
	
	// Iterable<Coordinate>
	@Override public Iterator<Coordinate> iterator() {
		return points.iterator();
	}
	// Object
	@Override public String toString() {
		return points.toString();
	}
	
}
