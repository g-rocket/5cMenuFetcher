package io.yancey.menufetcher;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import com.google.gson.*;

public class BonAppetitMenuFetcher implements MenuFetcher {
	private final int cafeId;
	public static final int PITZER_ID = 219;
	public static final int COLLINS_ID = 50;
	
	public BonAppetitMenuFetcher(int cafeId) {
		this.cafeId = cafeId;
	}
	
	private String getMenuUrl(LocalDate day) {
		return String.format("http://legacy.cafebonappetit.com/api/2/menus?format=json&cafe=%d&date=%s",
				cafeId, day.format(DateTimeFormatter.ISO_LOCAL_DATE));
	}
	
	private JsonElement getMenuJson(LocalDate day) {
		String url = getMenuUrl(day);
		try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
			sc.useDelimiter("\\A");
			String menuString = sc.hasNext()? sc.next(): "";
			return new JsonParser().parse(menuString);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Meal> getMeals(LocalDate day) {
		JsonObject menuData = getMenuJson(day).getAsJsonObject();
		JsonArray mealsData = menuData
				.getAsJsonArray("days")
				.get(0).getAsJsonObject()
				.getAsJsonObject("cafes")
				.getAsJsonObject(Integer.toString(cafeId))
				.getAsJsonArray("dayparts")
				.get(0).getAsJsonArray();
		JsonObject itemsData = menuData
				.getAsJsonObject("items");
		List<Meal> meals = new ArrayList<>(3);
		for(JsonElement mealData: mealsData) {
			meals.add(createMeal(mealData.getAsJsonObject(), itemsData));
		}
		return meals;
	}
	
	private Meal createMeal(JsonObject mealData, JsonObject itemsData) {
		List<Station> stations = new ArrayList<>();
		for(JsonElement stationData: mealData.getAsJsonArray("stations")) {
			stations.add(createStation(stationData.getAsJsonObject(), itemsData));
		}
		return new Meal(stations,
				LocalTime.parse(mealData.get("starttime").getAsString()),
				LocalTime.parse(mealData.get("endtime").getAsString()),
				mealData.get("label").getAsString());
	}

	private Station createStation(JsonObject stationData, JsonObject itemsData) {
		List<MenuItem> items = new ArrayList<>();
		for(JsonElement itemId: stationData.getAsJsonArray("items")) {
			items.add(createMenuItem(itemsData.getAsJsonObject(itemId.getAsString())));
		}
		return new Station(stationData.get("label").getAsString(), items);
	}

	private MenuItem createMenuItem(JsonObject itemData) {
		return new MenuItem(itemData.get("label").getAsString(),
				itemData.get("description").getAsString());
	}

	public static void main(String[] args) {
		System.out.println(new BonAppetitMenuFetcher(COLLINS_ID).getMeals(java.time.LocalDate.of(2016, 01, 21)));
	}

}
