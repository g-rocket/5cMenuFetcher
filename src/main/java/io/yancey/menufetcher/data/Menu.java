package io.yancey.menufetcher.data;

import java.util.*;

public class Menu {
	public final String diningHallName;
	public final String diningHallId;
	public final String publicUrl;
	public final List<Meal> meals;
	
	public Menu(String diningHallName, String diningHallId, String publicUrl, List<Meal> meals) {
		this.diningHallName = diningHallName;
		this.diningHallId = diningHallId;
		this.publicUrl = publicUrl;
		this.meals = meals;
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
