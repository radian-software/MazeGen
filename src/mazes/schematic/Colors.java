/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.schematic;

import java.awt.Color;

public final class Colors {
	
	public final Color active, inactive;
	
	public Colors(Color active, Color inactive) {
		this.active = active;
		this.inactive = inactive;
	}

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((active == null) ? 0 : active.hashCode());
		result = prime * result + ((inactive == null) ? 0 : inactive.hashCode());
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Colors other = (Colors) obj;
		if (active == null) {
			if (other.active != null)
				return false;
		} else if (!active.equals(other.active))
			return false;
		if (inactive == null) {
			if (other.inactive != null)
				return false;
		} else if (!inactive.equals(other.inactive))
			return false;
		return true;
	}
	@Override public String toString() {
		return active.toString().substring(14) + "/" + inactive.toString().substring(14);
	}
	
}
