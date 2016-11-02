package io.yancey.menufetcher.data;

import java.io.*;
import java.time.*;
import java.util.*;

import com.google.gson.stream.*;

public class Meal {
	public final List<Station> stations;
	public final LocalTime startTime;
	public final LocalTime endTime;
	public final String name;
	public final String description;
	
	public Meal(List<Station> stations, 
			LocalTime startTime, LocalTime endTime,
			String name, String description) {
		this.stations = stations;
		this.startTime = startTime;
		this.endTime = endTime;
		this.name = name;
		this.description = description;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(name.toString());
		if(!description.isEmpty()) {
			sb.append(": \n");
			sb.append(description);
		}
		if(startTime != null && endTime != null) {
			sb.append("\n");
			sb.append(startTime);
			sb.append(" - ");
			sb.append(endTime);
		}
		sb.append("\n================\n\n");
		for(Station station: stations) {
			sb.append(station);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public boolean equals(Object o) {
		return o instanceof Meal &&
				((Meal)o).name == name &&
				((Meal)o).stations.equals(stations);
	}
	
	public int hashCode() {
		return name.hashCode() ^ stations.hashCode() ^
				startTime.hashCode() ^ endTime.hashCode();
	}
	
	public void toJson(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("name").value(name);
		writer.name("description").value(description);
		if(startTime != null) writer.name("startTime").value(startTime.toString());
		if(endTime != null) writer.name("endTime").value(endTime.toString());
		writer.name("stations").beginArray();
		for(Station station: stations) station.toJson(writer);
		writer.endArray();
		writer.endObject();
	}
}
