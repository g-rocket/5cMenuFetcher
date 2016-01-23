package io.yancey.menufetcher;

import java.util.*;

public class Menu {
	public final String diningHallName;
	public final String diningHallId;
	public final List<Meal> meals;
	
	public Menu(String diningHallName, String diningHallId, List<Meal> meals) {
		this.diningHallName = diningHallName;
		this.diningHallId = diningHallId;
		this.meals = Collections.unmodifiableList(meals);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(diningHallName);
		sb.append(" (");
		sb.append(diningHallId);
		sb.append("):\n");
		for(Meal m: meals) {
			sb.append(m);
		}
		return sb.toString();
	}
}
