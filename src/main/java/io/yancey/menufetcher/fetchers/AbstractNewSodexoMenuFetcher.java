package io.yancey.menufetcher.fetchers;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.google.gson.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;

public abstract class AbstractNewSodexoMenuFetcher extends AbstractMenuFetcher {
	private final int menuId;
	private final int locationId;
	private final String sitename;
	private final String dininghallname;
	
	private HashMap<LocalDate, JsonObject> jsonCache = new HashMap<>();
	
	private String menuUrl(LocalDate day) {
		String startDateString = day.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
		return String.format("https://menus.sodexomyway.com/BiteMenu/MenuOnly?menuId=%d&locationId=%d&startdate=%s",
				menuId, locationId, startDateString);
	}
	
	private String hoursUrl() {
		return String.format("https://%s.sodexomyway.com/dining-near-me/hours", sitename);
	}
	
	private String whereAmIUrl() {
		return String.format("http://%s.sodexomyway.com/dining-near-me/%s", sitename, dininghallname);
	}
	
	private String publicMenuUrl(LocalDate day) {
		String startDateString = day.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
		return String.format("https://menus.sodexomyway.com/BiteMenu/Menu?menuId=%d&locationId=%d&whereami=%s&startdate=%s",
				menuId, locationId, whereAmIUrl(), startDateString);
	}
	
	private void fetchMenu(LocalDate day) throws MenuNotAvailableException {
		Document menuPage;
		try(Scanner sc = new Scanner(new URL(menuUrl(day)).openStream())) {
			menuPage = Jsoup.parseBodyFragment(sc.useDelimiter("\\A").next());
		} catch (IOException e) {
			throw new MenuNotAvailableException("Error fetching menu data", e);
		}
		String text = menuPage.getElementById("nutData").html();
		JsonArray menus = new JsonParser().parse(text).getAsJsonArray();
		for (JsonElement e: menus) {
			JsonObject menu = e.getAsJsonObject();
			LocalDate menuDay = LocalDate.parse(menu.get("date").getAsString(), DateTimeFormatter.ISO_DATE_TIME);
			jsonCache.put(menuDay, menu);
		}
	}
	
	private JsonObject getMenuJson(LocalDate day) throws MenuNotAvailableException {
		if (!jsonCache.containsKey(day)) {
			fetchMenu(day);
		}
		return jsonCache.get(day);
	}
	
	public AbstractNewSodexoMenuFetcher(String name, String id, String sitename, int menuId, int locationId, String dininghallname) {
		super(name, id);
		this.sitename = sitename;
		this.menuId = menuId;
		this.locationId = locationId;
		this.dininghallname = dininghallname;
	}
	
	@Override
	public Menu getMeals(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
		JsonObject menuJson = getMenuJson(day);
		ArrayList<Meal> meals = new ArrayList<>(3);
		for (JsonElement e: menuJson.getAsJsonArray("dayParts")) {
			meals.add(parseMeal(e.getAsJsonObject()));
		}
		return new Menu(name, id, publicMenuUrl(day), meals);
	}

	private static Meal parseMeal(JsonObject mealJson) throws MalformedMenuException {
		LocalTimeRange timeRange = null;
		ArrayList<Station> stations = new ArrayList<>();
		for(JsonElement e: mealJson.getAsJsonArray("courses")) {
			JsonObject stationJson = e.getAsJsonObject();
			stations.add(parseStation(stationJson));
			for (JsonElement e2: stationJson.getAsJsonArray("menuItems")) {
				JsonObject item = e2.getAsJsonObject();
				LocalTime itemStartTime = LocalTime.parse(item.get("startTime").getAsString(), DateTimeFormatter.ISO_DATE_TIME);
				LocalTime itemEndTime = LocalTime.parse(item.get("endTime").getAsString(), DateTimeFormatter.ISO_DATE_TIME);
				LocalTimeRange itemTimeRange = new LocalTimeRange(itemStartTime, itemEndTime);
				if (timeRange == null) {
					timeRange = itemTimeRange;
				} else {
					if (!timeRange.equals(itemTimeRange)) {
						System.err.printf("Error: start times do not match; expected %s but got %s\n",
								timeRange.toString(), itemTimeRange.toString());
					}
				}
			}
		}
		return new Meal(stations, timeRange, mealJson.get("dayPartName").getAsString(), "");
	}

	private static Station parseStation(JsonObject stationJson) {
		List<MenuItem> items = new ArrayList<MenuItem>();
		for (JsonElement e: stationJson.getAsJsonArray("menuItems")) {
			JsonObject itemJson = e.getAsJsonObject();
			items.add(parseItem(itemJson));
		}
		return new Station(stationJson.get("courseName").getAsString(), items);
	}

	private static MenuItem parseItem(JsonObject itemJson) {
		String name = itemJson.get("formalName").getAsString();
		String description = itemJson.get("description").getAsString();
		return new MenuItem(name, description, Collections.emptySet());
	}
}
