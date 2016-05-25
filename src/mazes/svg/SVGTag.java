/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.svg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SVGTag implements SVGElement {
	
	public abstract String getTag();
	public abstract Map<String, String> getAttributes();
	public abstract List<SVGElement> getChildElements();
	
	@Override public List<String> toSVGCode() {
		List<String> code = new ArrayList<>();
		String tagLine = "<" + getTag();
		for (Map.Entry<String, String> attribute : getAttributes().entrySet()) {
			tagLine += " " + attribute.getKey() + " = \"" + attribute.getValue() + "\"";
		}
		List<SVGElement> childElements = getChildElements();
		if (childElements == null) {
			tagLine += " />";
			code.add(tagLine);
			return code;
		}
		else {
			if (isInline()) {
				tagLine += ">";
				if (childElements.size() != 1) {
					throw new IllegalStateException();
				}
				tagLine += childElements.get(0).toSVGCode().stream().collect(Collectors.joining("\n"));
				tagLine += "</" + getTag() + ">";
				code.add(tagLine);
				return code;
			}
			else {
				tagLine += ">";
				code.add(tagLine);
				for (SVGElement childElement : childElements) {
					List<String> childElementCode = childElement.toSVGCode();
					for (String codeLine : childElementCode) {
						code.add("\t" + codeLine);
					}
				}
				code.add("</" + getTag() + ">");
				return code;
			}
		}
	}
	
}
