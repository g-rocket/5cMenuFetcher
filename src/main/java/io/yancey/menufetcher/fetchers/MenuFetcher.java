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
		// TODO uncomment all commented lines
		List<MenuFetcher> menuFetchers = new ArrayList<>(7);
		menuFetchers.add(new HochMenuFetcher());
		menuFetchers.add(new PitzerMenuFetcher());
		//menuFetchers.add(new FrankMenuFetcher());
		menuFetchers.add(new FraryMenuFetcher());
		//menuFetchers.add(new OldenborgMenuFetcher());
		//menuFetchers.add(new ScrippsMenuFetcher());
		//menuFetchers.add(new CollinsMenuFetcher());
		return Collections.unmodifiableList(menuFetchers);
	}
	
	
	public static List<Menu> fetchAllMenus(List<MenuFetcher> menuFetchers, LocalDate day) {
		List<Menu> menus = new ArrayList<>();
		for(MenuFetcher menuFetcher: menuFetchers) {
			try {
				menus.add(menuFetcher.getMeals(day));
				System.out.print(".");
			} catch(MenuNotAvailableException e) {
				System.err.println("Error fetching "+menuFetcher.getId()+
						" for "+day+": menu not found");
				e.printStackTrace();
			} catch(MalformedMenuException e) {
				System.err.println("Error fetching "+menuFetcher.getId()+
						" for "+day+": invalid data recieved");
				e.printStackTrace();
			} catch(Throwable t) {
				System.err.println("Invalid exception recieved fetching "+
						menuFetcher.getId()+" for "+day+": "+t);
				throw t;
			}
		}
		System.out.println();
		return menus;
	}
}
