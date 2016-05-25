/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.svg;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mazes.schematic.Dimensions;

public final class SVGText extends SVGTag {
	
	private SVGCoordinate loc;
	private String text;
	
	public SVGText(SVGCoordinate loc, String text) {
		this.loc = loc;
		this.text = text;
	}
	
	// SVGElement
	@Override public boolean isInline() {
		return true;
	}
	// SVGTag
	@Override public String getTag() {
		return "text";
	}
	@Override public Map<String, String> getAttributes() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("x", String.valueOf(loc.svgX()));
		map.put("y", String.valueOf(loc.svgY()));
		map.put("fill", "black");
		map.put("font-size", String.valueOf(Dimensions.fontSize()));
		map.put("font-family", "tinos, sans-serif");
		return map;
	}
	@Override public List<SVGElement> getChildElements() {
		return Arrays.asList(new SVGRawCode(text));
	}
	
}
