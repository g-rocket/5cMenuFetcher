package io.yancey.menufetcher;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import com.google.gson.*;

public class BonAppetitMenuFetcher extends AbstractMenuFetcher {
	private final int cafeId;
	private final String publicMenuUrlPrefix, publicMenuUrlCafe;
	public static final int PITZER_ID = 219;
	public static final int COLLINS_ID = 50;
	public static final String PITZER_PUBLIC_MENU_URL_PREFIX = "pitzer";
	public static final String PITZER_PUBLIC_MENU_URL_CAFE = "mcconnell-bistro";
	public static final String COLLINS_PUBLIC_MENU_URL_PREFIX = "collins-cmc";
	public static final String COLLINS_PUBLIC_MENU_URL_CAFE = "collins";
	
	public BonAppetitMenuFetcher(String name, String id, int cafeId,
			String publicMenuUrlPrefix, String publicMenuUrlCafe) {
		super(name, id);
		this.cafeId = cafeId;
		this.publicMenuUrlPrefix = publicMenuUrlPrefix;
		this.publicMenuUrlCafe = publicMenuUrlCafe;
	}
	
	private String getJsonMenuUrl(LocalDate day) {
		return String.format("http://legacy.cafebonappetit.com/api/2/menus?format=json&cafe=%d&date=%s",
				cafeId, day.format(DateTimeFormatter.ISO_LOCAL_DATE));
	}
	
	private JsonElement getMenuJson(LocalDate day)
			throws MenuNotAvailableException, MalformedMenuException {
		String url = getJsonMenuUrl(day);
		try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
			sc.useDelimiter("\\A");
			String menuString = sc.hasNext()? sc.next(): "";
			return new JsonParser().parse(menuString);
		} catch (MalformedURLException e) {
			throw new MalformedMenuException("Invalid json url", e);
		} catch (IOException e) {
			throw new MenuNotAvailableException("Error fetching json",e);
		}
	}

	@Override
	public Menu getMeals(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
		JsonObject menuData = getMenuJson(day).getAsJsonObject();
		JsonArray mealsDataParts = menuData
				.getAsJsonArray("days")
				.get(0).getAsJsonObject()
				.getAsJsonObject("cafes")
				.getAsJsonObject(Integer.toString(cafeId))
				.getAsJsonArray("dayparts");
		if(mealsDataParts.size() == 0) {
			return new Menu(name, id, getMenuUrl(day), Collections.emptyList());
		}
		JsonArray mealsData = mealsDataParts
				.get(0).getAsJsonArray();
		JsonObject itemsData = menuData
				.getAsJsonObject("items");
		List<Meal> meals = new ArrayList<>(3);
		for(JsonElement mealData: mealsData) {
			meals.add(createMeal(mealData.getAsJsonObject(), itemsData));
		}
		return new Menu(name, id, getMenuUrl(day), meals);
	}
	
	private Meal createMeal(JsonObject mealData, JsonObject itemsData) {
		List<Station> stations = new ArrayList<>();
		for(JsonElement stationData: mealData.getAsJsonArray("stations")) {
			stations.add(createStation(stationData.getAsJsonObject(), itemsData));
		}
		return new Meal(stations,
				LocalTime.parse(mealData.get("starttime").getAsString()),
				LocalTime.parse(mealData.get("endtime").getAsString()),
				mealData.get("label").getAsString(), "");
	}

	private Station createStation(JsonObject stationData, JsonObject itemsData) {
		List<MenuItem> items = new ArrayList<>();
		for(JsonElement itemId: stationData.getAsJsonArray("items")) {
			items.add(createMenuItem(itemsData.getAsJsonObject(itemId.getAsString())));
		}
		return new Station(stationData.get("label").getAsString(), items);
	}

	private MenuItem createMenuItem(JsonObject itemData) {
		Set<String> tags;
		if(itemData.get("cor_icon").isJsonObject()) {
			JsonObject tagArray = itemData.getAsJsonObject("cor_icon");
			tags = tagArray.entrySet().parallelStream()
					.map((e) -> e.getValue().getAsString())
					.collect(Collectors.toSet());
		} else {
			tags = Collections.emptySet();
		}
		return new MenuItem(itemData.get("label").getAsString(),
				itemData.get("description").getAsString(), tags);
	}

	public static void main(String[] args) throws MenuNotAvailableException, MalformedMenuException {
		System.out.println(new BonAppetitMenuFetcher("Collins", "collins", COLLINS_ID,
				COLLINS_PUBLIC_MENU_URL_CAFE, COLLINS_PUBLIC_MENU_URL_CAFE).getMeals(java.time.LocalDate.of(2016, 01, 21)));
	}
	
	private String getMenuUrl(LocalDate day) {
		return "http://" + publicMenuUrlPrefix + ".cafebonappetit.com/cafe/" + publicMenuUrlCafe + "/" + day.toString();
	}

}
