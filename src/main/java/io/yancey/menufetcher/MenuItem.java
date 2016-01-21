package io.yancey.menufetcher;

public class MenuItem {
	public final String name;
	public final String description;
	
	public MenuItem(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	public String toString() {
		if(description.isEmpty()) {
			return name;
		} else {
			return name + ": \n\t" + description.replaceAll("<br />", "\n\t");
		}
	}
	
	public boolean equals(Object o) {
		return o instanceof MenuItem &&
				((MenuItem)o).name.equals(name) &&
				((MenuItem)o).description.equals(description);
	}
	
	public int hashCode() {
		return name.hashCode() ^ description.hashCode();
	}
}
