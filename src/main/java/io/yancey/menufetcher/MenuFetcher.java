package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public interface MenuFetcher {
	public List<Meal> getMeals(LocalDate day);
	
	public static Map<String,MenuFetcher> getAllMenuFetchers() {
		Map<String, MenuFetcher> menuFetchers = new LinkedHashMap<>();
		menuFetchers.put("The Hoch", new SodexoMenuFetcher(SodexoMenuFetcher.HOCH_SITENAME));
		menuFetchers.put("Pitzer",   new BonAppetitMenuFetcher(BonAppetitMenuFetcher.PITZER_ID));
		menuFetchers.put("Frank",    new PomonaMenuFetcher(PomonaMenuFetcher.FRANK_NAME));
		menuFetchers.put("Frary",    new PomonaMenuFetcher(PomonaMenuFetcher.FRARY_NAME));
		menuFetchers.put("Oldenborg",new PomonaMenuFetcher(PomonaMenuFetcher.OLDENBORG_NAME));
		menuFetchers.put("Scripps",  new SodexoMenuFetcher(SodexoMenuFetcher.SCRIPPS_SITENAME));
		menuFetchers.put("Collins",  new BonAppetitMenuFetcher(BonAppetitMenuFetcher.COLLINS_ID));
		return Collections.unmodifiableMap(menuFetchers);
	}
}
