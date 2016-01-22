package io.yancey.menufetcher;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import com.google.common.base.*;

public class SodexoMenuFetcher implements MenuFetcher {
	private final String sitename;
	
	public static final String HOCH_SITENAME = "hmc";
	public static final String SCRIPPS_SITENAME = "scrippsdining";
	
	private static final List<String> mealNames = Arrays.asList("brk", "lun", "din");
	
	public SodexoMenuFetcher(String sitename) {
		this.sitename = sitename;
	}
	
	private String getPortalUrl() {
		return "https://" + sitename + ".sodexomyway.com/dining-choices/index.html?forcedesktop=true";
	}
	
	private static final Pattern datesPattern = Pattern.compile("([0-9][0-9]?)/([0-9][0-9]?)/([0-9]+) - ([0-9][0-9]?)/([0-9][0-9]?)/([0-9]+)");
	private String getMenuUrl(LocalDate day) {
		Document portal;
		try {
			portal = Jsoup.connect(getPortalUrl()).get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Elements menus = portal.getElementById("accordion_3543").getElementsByTag("ul").first().children();
		for(Element menuListing: menus) {
			String datesString = menuListing.child(0).ownText();
			Matcher dates = datesPattern.matcher(datesString);
			if(!dates.matches()) {
				throw new IllegalStateException("Invalid date range string: " + datesString);
			}
			LocalDate startDate = LocalDate.of(
					Integer.parseInt(dates.group(3)),
					Integer.parseInt(dates.group(1)),
					Integer.parseInt(dates.group(2)));
			LocalDate endDate = LocalDate.of(
					Integer.parseInt(dates.group(6)),
					Integer.parseInt(dates.group(4)),
					Integer.parseInt(dates.group(5)));
			if(!day.isBefore(startDate) && !day.isAfter(endDate)) {
				return menuListing.child(0).absUrl("href") + "?forcedesktop=true";
			}
		}
		// requested date not found
		return null;
	}
	
	public Element getMenu(LocalDate day, Document menuPage) {
		Elements menus = menuPage.getElementsByClass("dayinner");
		return menus.get(day.getDayOfWeek().getValue() - 1).child(0);
	}
	
	@Override
	public List<Meal> getMeals(LocalDate day) {
		String menuUrl = getMenuUrl(day);
		if(menuUrl == null) {
			// no menu available for requested day
			return Collections.emptyList();
		}
		Document menuPage;
		try(InputStream portalStream = new URL(menuUrl).openStream()) {
			menuPage = Jsoup.parse(portalStream, "Windows-1252", menuUrl);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Element menu = getMenu(day, menuPage);
		List<Meal> meals = new ArrayList<>(3);
		for(String mealName: mealNames) {
			if(!menu.getElementsByClass(mealName).isEmpty()) {
				meals.add(createMeal(menu.getElementsByClass(mealName)));
			}
		}
		return meals;
	}
	
	private Meal createMeal(Elements mealItems) {
		String name = mealItems.remove(0).getElementsByClass("mealname").first().ownText();
		List<Station> stations = new ArrayList<>();
		ListIterator<Element> mealItemIter = mealItems.listIterator();
		while(mealItemIter.hasNext()) {
			stations.add(createStation(mealItemIter));
		}
		return new Meal(stations, null, null, name, "");
	}

	private Station createStation(ListIterator<Element> mealItemIter) {
		List<MenuItem> items = new ArrayList<>();
		String name = null;
		while(mealItemIter.hasNext()) {
			Element mealItem = mealItemIter.next();
			//System.out.println(mealItem);
			if(mealItem.getElementsByClass("station").isEmpty()) {
				if(name != null) {
					break;
				} else {
					continue;
				}
			}
			if(name == null) {
				name = mealItem.getElementsByClass("station").first().ownText().substring(1);
			}
			items.add(createMenuItem(mealItem));
		}
		return new Station(name, items);
	}

	private MenuItem createMenuItem(Element itemInfo) {
		itemInfo.getElementsByClass("station").remove();
		String name = itemInfo.text();
		Set<String> tags = new HashSet<>();
		for(Element tag: itemInfo.getElementsByClass("icon")) {
			tags.add(tag.attr("alt"));
		}
		return new MenuItem(name, "", tags);
	}

	public static void main(String[] args) {
		System.out.println(new SodexoMenuFetcher(HOCH_SITENAME).getMeals(LocalDate.of(2016, 01, 22)));
	}
}
