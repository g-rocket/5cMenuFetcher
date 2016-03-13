package io.yancey.menufetcher;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class SodexoMenuFetcher extends AbstractMenuFetcher {
	public static final String HOCH_SITENAME = "hmc";
	public static final String SCRIPPS_SITENAME = "scrippsdining";
	
	private static final List<String> mealNames = Arrays.asList("brk", "lun", "din");
	
	private final String sitename;
	protected Map<String, Document> pageCache = new HashMap<>();
	
	public SodexoMenuFetcher(String name, String id, String sitename) {
		super(name, id);
		this.sitename = sitename;
	}
	
	private String getPortalUrl() {
		return "https://" + sitename + ".sodexomyway.com/dining-choices/index.html?forcedesktop=true";
	}
	
	private static final Pattern datesPattern = Pattern.compile(
			"([0-9][0-9]?)/([0-9][0-9]?)/([0-9]+) - ([0-9][0-9]?)/([0-9][0-9]?)/([0-9]+)");
	public String getMenuUrl(LocalDate day)
			throws MenuNotAvailableException, MalformedMenuException {
		if(!pageCache.containsKey(getPortalUrl())) {
			try {
				pageCache.put(getPortalUrl(), Jsoup.connect(getPortalUrl()).get());
			} catch (IOException e) {
				throw new MenuNotAvailableException("Error fetching portal", e);
			}
		}
		Document portal = pageCache.get(getPortalUrl());
		Elements menus;
		try {
			menus = portal.getElementById("accordion_3543").getElementsByTag("ul").first().children();
		} catch(NullPointerException e) {
			throw new MenuNotAvailableException("Portal doesn't have any menus", e);
		}
		for(Element menuListing: menus) {
			String datesString = menuListing.child(0).ownText();
			Matcher dates = datesPattern.matcher(datesString);
			if(!dates.matches()) {
				//throw new MalformedMenuException("Invalid date range string: " + datesString);
				System.err.println("Invalid date string fetching "+id+": "+datesString);
				continue;
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
	
	public Element getMenu(LocalDate day, Document menuPage) throws MenuNotAvailableException {
		return menuPage.getElementById(day.getDayOfWeek().getDisplayName(
				TextStyle.FULL_STANDALONE, Locale.ENGLISH).toLowerCase());
	}
	
	public String getPublicMenuUrl(String menuUrl, LocalDate day) {
		return menuUrl + "#" + day.getDayOfWeek().toString().toLowerCase();
	}
	
	@Override
	public Menu getMeals(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
		String menuUrl = getMenuUrl(day);
		if(menuUrl == null) {
			// no menu available for requested day
			return new Menu(name, id, getPublicMenuUrl(menuUrl, day), Collections.emptyList());
		}
		Document menuPage;
		try(InputStream portalStream = new URL(menuUrl).openStream()) {
			menuPage = Jsoup.parse(portalStream, "Windows-1252", menuUrl);
		} catch (IOException e) {
			throw new MenuNotAvailableException("Error fetching menu",e);
		}
		Element menu = getMenu(day, menuPage);
		List<Meal> meals = new ArrayList<>(3);
		for(String mealName: mealNames) {
			if(!menu.getElementsByClass(mealName).isEmpty()) {
				meals.add(createMeal(menu.getElementsByClass(mealName),
						day.getDayOfWeek().compareTo(DayOfWeek.FRIDAY) > 0));
			}
		}
		return new Menu(name, id, getPublicMenuUrl(menuUrl, day), meals);
	}
	
	private Meal createMeal(Elements mealItems, boolean isWeekend) {
		String name = mealItems.remove(0).getElementsByClass("mealname").first().ownText();
		name = name.substring(0, 1) + name.substring(1).toLowerCase();
		if(name.equals("Lunch") && isWeekend) {
			name = "Brunch";
		}
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

	public static void main(String[] args) throws MenuNotAvailableException, MalformedMenuException {
		System.out.println(new SodexoMenuFetcher("Scripps", "scripps", SCRIPPS_SITENAME).getMeals(LocalDate.of(2016, 03, 20)));
	}
}
