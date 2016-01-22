package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public interface MenuFetcher {
	public List<Meal> getMeals(LocalDate day);
	
	public static List<MenuFetcher> getAllMenuFetchers() {
		return Arrays.asList(
				new PomonaMenuFetcher(PomonaMenuFetcher.FRANK_NAME),
				new PomonaMenuFetcher(PomonaMenuFetcher.FRARY_NAME),
				new PomonaMenuFetcher(PomonaMenuFetcher.OLDENBORG_NAME),
				new BonAppetitMenuFetcher(BonAppetitMenuFetcher.COLLINS_ID),
				new BonAppetitMenuFetcher(BonAppetitMenuFetcher.PITZER_ID),
				new SodexoMenuFetcher(SodexoMenuFetcher.HOCH_SITENAME),
				new SodexoMenuFetcher(SodexoMenuFetcher.SCRIPPS_SITENAME));
	}
}
