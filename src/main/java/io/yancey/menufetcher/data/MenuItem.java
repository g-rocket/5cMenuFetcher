package io.yancey.menufetcher.data;

import java.io.*;
import java.util.*;

import com.google.gson.stream.*;

public class MenuItem {
	public final String name;
	public final String description;
	public final Set<String> tags;
	
	public MenuItem(String name, String description, Set<String> tags) {
		this.name = name;
		this.description = description;
		this.tags = Collections.unmodifiableSet(tags);
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
				name.equals(((MenuItem)o).name) &&
				description.equals(((MenuItem)o).description) &&
				tags.equals(((MenuItem)o).tags);
	}
	
	public int hashCode() {
		return name.hashCode() ^ description.hashCode() ^ tags.hashCode();
	}
	
	public void toJson(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("name").value(name);
		writer.name("description").value(description);
		writer.name("tags").beginArray();
		for(String tag: tags) writer.value(tag);
		writer.endArray();
		writer.endObject();
	}
}
