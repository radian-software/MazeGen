/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.svg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SVGComment implements SVGElement {
	
	private final List<String> content;
	
	public SVGComment(List<String> content) {
		this.content = content;
	}
	public SVGComment(String content) {
		this.content = Arrays.asList(content.split("\\n"));
	}
	
	@Override public List<String> toSVGCode() {
		List<String> code = new ArrayList<>();
		code.add("<!--");
		code.addAll(content);
		code.add("-->");
		return code;
	}
	
}
