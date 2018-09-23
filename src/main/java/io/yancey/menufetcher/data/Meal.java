package io.yancey.menufetcher.data;

import java.io.*;
import java.util.*;

import com.google.gson.stream.*;

public class Meal {
  public final List<Station> stations;
  public final LocalTimeRange hours;
  public final String name;
  public final String description;

  public Meal(List<Station> stations,
      LocalTimeRange hours,
      String name, String description) {
    this.stations = stations;
    this.hours = hours;
    this.name = name;
    this.description = description;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(name.toString());
    if(!description.isEmpty()) {
      sb.append(": \n");
      sb.append(description);
    }
    if(hours != null) {
      sb.append("\n");
      sb.append(hours.startTime);
      sb.append(" - ");
      sb.append(hours.endTime);
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
        (hours == null? 0: hours.startTime.hashCode() ^ hours.endTime.hashCode());
  }

  public void toJson(JsonWriter writer) throws IOException {
    writer.beginObject();
    writer.name("name").value(name);
    writer.name("description").value(description);
    if(hours != null) {
      writer.name("startTime").value(hours.startTime.toString());
      writer.name("endTime").value(hours.endTime.toString());
    }
    writer.name("stations").beginArray();
    for(Station station: stations) station.toJson(writer);
    writer.endArray();
    writer.endObject();
  }
}
