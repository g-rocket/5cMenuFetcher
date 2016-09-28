package io.yancey.menufetcher.fetchers;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.dininghalls.*;

import java.time.*;
import java.util.*;

public interface MenuFetcher {
	public Menu getMeals(LocalDate day)
			throws MenuNotAvailableException, MalformedMenuException;
	
	public String getName();
	public String getId();
	
	public static List<MenuFetcher> getAllMenuFetchers() {
		List<MenuFetcher> menuFetchers = new ArrayList<>(7);
		menuFetchers.add(new HochMenuFetcher());
		menuFetchers.add(new PitzerMenuFetcher());
		menuFetchers.add(new FrankMenuFetcher());
		menuFetchers.add(new FraryMenuFetcher());
		menuFetchers.add(new OldenborgMenuFetcher());
		menuFetchers.add(new ScrippsMenuFetcher());
		menuFetchers.add(new CollinsMenuFetcher());
		return Collections.unmodifiableList(menuFetchers);
	}
}
