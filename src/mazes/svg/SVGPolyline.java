/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.svg;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mazes.schematic.Dimensions;
import util.StreamUtil;

public final class SVGPolyline extends SVGTag implements Iterable<SVGCoordinate> {
	
	private final Deque<SVGCoordinate> points;
	private final double lineWidth;
	
	public SVGPolyline(double lineWidth) {
		this(lineWidth, new ArrayDeque<>());
	}
	public SVGPolyline(double lineWidth, Deque<SVGCoordinate> points) {
		this.points = points;
		this.lineWidth = lineWidth;
	}
	
	public void addFirst(SVGCoordinate point) {
		points.addFirst(point);
	}
	public void addLast(SVGCoordinate point) {
		points.addLast(point);
	}
	public SVGCoordinate getFirst() {
		return points.getFirst();
	}
	public SVGCoordinate getLast() {
		return points.getLast();
	}
	
	// Iterable<SVGCoordinate>
	@Override public Iterator<SVGCoordinate> iterator() {
		return points.iterator();
	}
	// Object
	@Override public String toString() {
		return points.toString();
	}
	// SVGTag
	@Override public String getTag() {
		return "polyline";
	}
	@Override public Map<String, String> getAttributes() {
		Map<String, String> map = new LinkedHashMap<>();
		StringBuilder sb = new StringBuilder();
		Iterator<SVGCoordinate> iterator = points.iterator();
		SVGCoordinate firstPoint = iterator.next();
		sb.append(String.format("%d,%d", firstPoint.svgX(), firstPoint.svgY()));
		for (SVGCoordinate coord : StreamUtil.toIterable(iterator)) {
			sb.append(String.format(" %d,%d", coord.svgX(), coord.svgY()));
		}
		map.put("points", sb.toString());
		map.put("fill", "none");
		map.put("stroke", "black");
		map.put("stroke-width", String.valueOf(lineWidth / Dimensions.cellWidth()));
		return map;
	}
	@Override public List<SVGElement> getChildElements() {
		return null;
	}
	
}
