package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public interface MenuFetcher {
	public Menu getMeals(LocalDate day)
			throws MenuNotAvailableException, MalformedMenuException;
	
	public String getName();
	public String getId();
	
	public static List<MenuFetcher> getAllMenuFetchers() {
		List<MenuFetcher> menuFetchers = new ArrayList<>(7);
		menuFetchers.add(new SodexoMenuFetcher("The Hoch", "hoch", SodexoMenuFetcher.HOCH_SITENAME));
		menuFetchers.add(new BonAppetitMenuFetcher("Pitzer", "pitzer", BonAppetitMenuFetcher.PITZER_ID));
		menuFetchers.add(new PomonaMenuFetcher("Frank", "frank", PomonaMenuFetcher.FRANK_NAME));
		menuFetchers.add(new PomonaMenuFetcher("Frary", "frary", PomonaMenuFetcher.FRARY_NAME));
		menuFetchers.add(new PomonaMenuFetcher("Oldenborg", "oldenborg", PomonaMenuFetcher.OLDENBORG_NAME));
		menuFetchers.add(new SodexoMenuFetcher("Scripps", "scripps", SodexoMenuFetcher.SCRIPPS_SITENAME));
		menuFetchers.add(new BonAppetitMenuFetcher("Collins", "collins", BonAppetitMenuFetcher.COLLINS_ID));
		return Collections.unmodifiableList(menuFetchers);
	}
}
