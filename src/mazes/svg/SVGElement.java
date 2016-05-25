/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.svg;

import java.util.List;

public interface SVGElement {
	
	List<String> toSVGCode();
	default boolean isInline() {
		return false;
	}
	
}
