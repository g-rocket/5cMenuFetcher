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
		menuFetchers.add(new SodexoMenuFetcher("The Hoch", "hoch", SodexoMenuFetcher.HOCH_SITENAME, SodexoMenuFetcher.HOCH_TCM));
		menuFetchers.add(new BonAppetitMenuFetcher("Pitzer", "pitzer", BonAppetitMenuFetcher.PITZER_ID,
				BonAppetitMenuFetcher.PITZER_PUBLIC_MENU_URL_PREFIX, BonAppetitMenuFetcher.PITZER_PUBLIC_MENU_URL_CAFE));
		menuFetchers.add(new PomonaMenuFetcher("Frank", "frank", PomonaMenuFetcher.FRANK_NAME));
		menuFetchers.add(new PomonaMenuFetcher("Frary", "frary", PomonaMenuFetcher.FRARY_NAME));
		menuFetchers.add(new PomonaMenuFetcher("Oldenborg", "oldenborg", PomonaMenuFetcher.OLDENBORG_NAME));
		menuFetchers.add(new SodexoMenuFetcher("Scripps", "scripps", SodexoMenuFetcher.SCRIPPS_SITENAME, SodexoMenuFetcher.SCRIPPS_TCM));
		menuFetchers.add(new BonAppetitMenuFetcher("Collins", "collins", BonAppetitMenuFetcher.COLLINS_ID,
				BonAppetitMenuFetcher.COLLINS_PUBLIC_MENU_URL_PREFIX, BonAppetitMenuFetcher.COLLINS_PUBLIC_MENU_URL_CAFE));
		return Collections.unmodifiableList(menuFetchers);
	}
}
