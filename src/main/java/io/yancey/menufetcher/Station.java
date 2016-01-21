package io.yancey.menufetcher;

import java.util.*;

public class Station {
	public final String name;
	public final List<MenuItem> menu;
	
	public Station(String name, List<MenuItem> menu) {
		this.name = name;
		this.menu = Collections.unmodifiableList(menu);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(name);
		sb.append("\n\n");
		for(MenuItem item: menu) {
			sb.append(item);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public boolean equals(Object o) {
		return o instanceof Station &&
				((Station)o).name.equals(name) &&
				((Station)o).menu.equals(menu);
	}
	
	public int hashCode() {
		return name.hashCode() ^ menu.hashCode();
	}
}
