package mazes.svg;

import java.util.Arrays;
import java.util.List;

public final class SVGRawCode implements SVGElement {
	
	private final String code;
	
	public SVGRawCode(String code) {
		this.code = code;
	}
	
	@Override public List<String> toSVGCode() {
		return Arrays.asList(code);
	}
	
}
