package io.yancey.menufetcher;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import com.google.gson.*;

public class ApiCreator {
	private static String toId(String name) {
		return name.toLowerCase().replaceAll("\\s+", "-");
	}
	
	public static void writeJson(Path apiPath, String apiNode, JsonElement json) throws IOException {
		Files.write(apiPath.resolve(apiNode), json.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	public static void createAPI(String folder, LocalDate day, List<Menu> menus) throws IOException {
		Path apiFolder = Paths.get(folder, "api", "v1");
		Files.createDirectories(apiFolder);
		createMenus(apiFolder, menus, day);
	}
	
	public static JsonElement createMenus(Path apiFolder, List<Menu> menus, LocalDate day) throws IOException {
		Path dayFolder = apiFolder.resolve(day.toString());
		Files.createDirectories(dayFolder);
		JsonArray menusJson = new JsonArray();
		JsonArray menusAllJson = new JsonArray();
		for(Menu menu: menus) {
			menusJson.add(createMenu(menu));
			JsonObject menuAllJson = createMenu(menu);
			JsonElement mealsJson = createMeals(dayFolder, menu);
			menuAllJson.add("meals", mealsJson);
			menusAllJson.add(menuAllJson);
		}
		writeJson(dayFolder, "dining-halls", menusJson);
		writeJson(dayFolder, "all", menusAllJson);
		
		return menusAllJson;
	}
	
	public static JsonElement createMeals(Path dayFolder, Menu menu) throws IOException {
		Path menuFolder = dayFolder.resolve(menu.diningHallId);
		Files.createDirectories(menuFolder);
		JsonArray mealsJson = new JsonArray();
		JsonArray mealsAllJson = new JsonArray();
		for(Meal meal: menu.meals) {
			mealsJson.add(createMeal(meal));
			JsonObject mealAllJson = createMeal(meal);
			JsonElement stationsJson = createStations(menuFolder, meal);
			mealAllJson.add("stations", stationsJson);
			mealsAllJson.add(mealAllJson);
		}
		writeJson(menuFolder, "meals", mealsJson);
		writeJson(menuFolder, "all", mealsAllJson);
		
		return mealsAllJson;
	}
	
	public static JsonElement createStations(Path menuFolder, Meal meal) throws IOException {
		Path mealFolder = menuFolder.resolve(toId(meal.name));
		Files.createDirectories(mealFolder);
		JsonArray stationsJson = new JsonArray();
		JsonArray stationsAllJson = new JsonArray();
		for(Station station: meal.stations) {
			stationsJson.add(createStation(station));
			JsonObject stationAllJson = createStation(station);
			JsonElement itemsJson = createItems(mealFolder, station);
			stationAllJson.add("items", itemsJson);
			stationsAllJson.add(stationAllJson);
		}
		writeJson(mealFolder, "stations", stationsJson);
		writeJson(mealFolder, "all", stationsAllJson);
		
		return stationsAllJson;
	}
	
	public static JsonElement createItems(Path mealFolder, Station station) throws IOException {
		Path stationFolder = mealFolder.resolve(toId(station.name));
		Files.createDirectories(stationFolder);
		JsonArray itemsJson = new JsonArray();
		for(MenuItem item: station.menu) {
			itemsJson.add(createMenuItem(item));
		}
		writeJson(stationFolder, "items", itemsJson);
		writeJson(stationFolder, "all", itemsJson);
		
		return itemsJson;
	}
	
	private static JsonObject createMenu(Menu menu) {
		JsonObject json = new JsonObject();
		json.addProperty("id", menu.diningHallId);
		json.addProperty("name", menu.diningHallName);
		json.addProperty("url", menu.publicUrl);
		return json;
	}
	
	private static JsonObject createMeal(Meal meal) {
		JsonObject json = new JsonObject();
		json.addProperty("id", toId(meal.name));
		json.addProperty("name", meal.name);
		json.addProperty("description", meal.description);
		if(meal.hours != null) {
			json.addProperty("startTime", meal.hours.startTime.toString());
			json.addProperty("endTime", meal.hours.endTime.toString());
		}
		return json;
	}
	
	private static JsonObject createStation(Station station) {
		JsonObject json = new JsonObject();
		json.addProperty("id", toId(station.name));
		json.addProperty("name", station.name);
		return json;
	}
	
	private static JsonObject createMenuItem(MenuItem item) {
		JsonObject json = new JsonObject();
		json.addProperty("name", item.name);
		json.addProperty("description", item.description);
		JsonArray tags = new JsonArray();
		for(String tag: item.tags) tags.add(tag);
		json.add("tags", tags);
		return json;
	}

	public static void createAPI(String folder, LocalDate day, Collection<MenuFetcher> menuFetchers) throws IOException {
		createAPI(folder, day, MenuFetcher.fetchAllMenus((List<MenuFetcher>) menuFetchers, day));
	}

	public static void createAPI(String folder, LocalDate day) throws IOException {
		createAPI(folder, day, MenuFetcher.getAllMenuFetchers());
	}
	
	public static void main(String[] args) {
		switch(args.length) {
		case 0:
				try {
					createAPI(".", LocalDate.now());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			return;
		case 1:
			List<MenuFetcher> menuFetchers = MenuFetcher.getAllMenuFetchers();
			for(LocalDate day = LocalDate.now(); day.isBefore(LocalDate.now().plusDays(7)); day = day.plusDays(1)) {
				try {
					createAPI(args[0], day, menuFetchers);
				} catch(Throwable t) {
					System.err.println("Failed to create API for day " + day);
					t.printStackTrace();
				}
			}
			return;
		default:
			for(int i = 1; i < args.length; i++) {
				try {
					createAPI(args[0], LocalDate.parse(args[i]));
				} catch (Throwable t) {
					System.err.println("Failed to create API for " + args[i]);
					t.printStackTrace();
				}
			}
			return;
		}
	}
}
