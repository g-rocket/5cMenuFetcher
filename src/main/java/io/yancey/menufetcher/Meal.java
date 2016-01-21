package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public class Meal {
	public final List<Station> stations;
	public final LocalTime startTime;
	public final LocalTime endTime;
	public final String name;
	
	public Meal(List<Station> stations, 
			LocalTime startTime, LocalTime endTime,
			String name) {
		this.stations = Collections.unmodifiableList(stations);
		this.startTime = startTime;
		this.endTime = endTime;
		this.name = name;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(name.toString());
		sb.append("\n");
		sb.append(startTime);
		sb.append(" - ");
		sb.append(endTime);
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
}
