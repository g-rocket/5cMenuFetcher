package io.yancey.menufetcher.data;

import java.io.*;
import java.util.*;

import org.jsoup.nodes.*;

import com.google.gson.stream.*;

public class MenuItem {
	public final String name;
	public final String description;
	public final Set<String> tags;
	
	public MenuItem(String name, String description, Set<String> tags) {
		this.name = name;
		this.description = description;
		this.tags = tags;
	}
	
	public String toString() {
		if(description.isEmpty()) {
			return name;
		} else {
			return name + ": \n\t" + description.replaceAll("<br />", "\n\t");
		}
	}
	
	public Element createElement(Element parent) {
		Element itemSpan = parent.appendElement("span");
		itemSpan.text(name);
		if(!description.isEmpty() && !description.equals(name)){
			itemSpan.attr("rel", "tooltip");
			itemSpan.attr("title", description.replaceAll("<br[^>]*>", "\n"));
		}
		return itemSpan;
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
