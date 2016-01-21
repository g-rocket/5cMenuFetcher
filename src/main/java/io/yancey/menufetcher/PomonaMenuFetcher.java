package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public class PomonaMenuFetcher implements MenuFetcher {
	private final String name;
	private static final String urlPrefix = "http://www.pomona.edu/administration/dining/menus/";
	
	public PomonaMenuFetcher(String name) {
		this.name = name;
	}
	
	private String getMenuUrl(LocalDate day) {
		return urlPrefix + name;
	}
	
	@Override
	public List<Meal> getMeals(LocalDate day) {
		return null;
	}
}
