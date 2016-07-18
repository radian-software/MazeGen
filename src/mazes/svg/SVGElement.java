package mazes.svg;

import java.util.List;

public interface SVGElement {
	
	List<String> toSVGCode();
	default boolean isInline() {
		return false;
	}
	
}
