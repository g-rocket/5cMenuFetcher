package io.yancey.menufetcher;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import com.google.gson.*;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.dininghalls.*;

public class InterestingItemExtractor {
	public static InterestingItemExtractor instance = new InterestingItemExtractor();
	
	private final JsonObject ruleTable;
	
	public InterestingItemExtractor() {
		ruleTable = new JsonParser().parse(new InputStreamReader(
				getClass().getResourceAsStream("/interestingItems.json")))
				.getAsJsonObject();
	}
	
	public List<Meal> getInterestingItems(Menu menu) {
		JsonObject rules = ruleTable.getAsJsonObject(menu.diningHallId);
		List<Meal> meals = new ArrayList<>();
		for(Meal baseMeal: menu.meals) {
			if(!rules.has(baseMeal.name)) continue;
			meals.add(getInterestingPartOfMeal(baseMeal, rules.getAsJsonObject(baseMeal.name)));
		}
		return meals;
	}

	private Meal getInterestingPartOfMeal(Meal baseMeal, JsonObject mealRules) {
		List<Station> newStations = new ArrayList<>();
		for(Map.Entry<String, JsonElement> stationRule: mealRules.entrySet()) {
			Station baseStation = getStationByName(baseMeal.stations, stationRule.getKey());
			if(baseStation == null) continue;
			Station newStation = getInterestingPartOfStation(
					baseStation,
					stationRule.getValue());
			if(newStation != null) newStations.add(newStation);
		}
		if(mealRules.has("")) {
			JsonObject wildcardRule = mealRules.get("").getAsJsonObject();
			Pattern stationPattern = Pattern.compile(wildcardRule.get("regex-match").getAsString());
			for(Station baseStation: baseMeal.stations) {
				if(stationPattern.matcher(baseStation.name).matches()) {
					Station newStation = getInterestingPartOfStation(
							baseStation,
							wildcardRule);
					if(newStation != null) newStations.add(newStation);
				}
			}
		}
		return new Meal(newStations, baseMeal.hours, baseMeal.name, baseMeal.description);
	}
	
	private Station getInterestingPartOfStation(Station baseStation, JsonElement rule) {
		if(rule.isJsonPrimitive()) {
			if(rule.getAsJsonPrimitive().isNumber()) {
				List<MenuItem> newMenuItems = new ArrayList<>();
				int howManyItems = rule.getAsInt();
				if(howManyItems > 0) {
					for(int i = 0; i < howManyItems && i < baseStation.menu.size(); i++) {
						newMenuItems.add(baseStation.menu.get(i));
					}
				} else {
					for(int i = 0; i < -howManyItems && i < baseStation.menu.size(); i++) {
						newMenuItems.add(baseStation.menu.get(baseStation.menu.size() - i - 1));
					}
				}
				return new Station(baseStation.name, newMenuItems);
			} else if(rule.getAsJsonPrimitive().isString()) {
				if(rule.getAsString().equals("all")) {
					return baseStation;
				} else if(rule.getAsString().equals("single")) {
					StringBuilder newName = new StringBuilder();
					for(MenuItem item: baseStation.menu) {
						newName.append(item.name.replaceAll("(^\\h*)|(\\h*$)",""));
						newName.append(", ");
					}
					newName.delete(newName.length() - 2, newName.length());
					return new Station(baseStation.name, Collections.singletonList(
							new MenuItem(newName.toString(), "", Collections.emptySet())));
				} else {
					throw new IllegalStateException("Bad rule: "+rule);
				}
			} else {
				throw new IllegalStateException("Bad rule: "+rule);
			}
		} else {
			JsonObject filter = rule.getAsJsonObject()
					.getAsJsonObject("include-if");
			if(filter == null || shouldIncludeStation(baseStation, filter)) {
				return getInterestingPartOfStation(baseStation,
						rule.getAsJsonObject().get("number"));
			}
			return null;
		}
	}

	private boolean shouldIncludeStation(Station baseStation, JsonObject filter) {
		Map.Entry<String, JsonElement> rule = filter.entrySet().iterator().next();
		switch(rule.getKey()) {
			case "contains":
				return baseStation.toString().contains(rule.getValue().getAsString());
			case "not-contains":
				return !baseStation.toString().contains(rule.getValue().getAsString());
		}
		return false;
	}

	private Station getStationByName(List<Station> stations, String stationName) {
		for(Station station: stations) {
			if(station.name.equalsIgnoreCase(stationName)) {
				return station;
			}
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println(new InterestingItemExtractor().getInterestingItems(new PitzerMenuFetcher().getMeals(LocalDate.now())));
	}
}
