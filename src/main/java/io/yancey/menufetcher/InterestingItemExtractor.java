package io.yancey.menufetcher;

import java.io.*;
import java.util.*;

import com.google.gson.*;

public class InterestingItemExtractor {
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
			Station newStation = getInterestingPartOfStation(
					getStationByName(baseMeal.stations, stationRule.getKey()),
					stationRule.getValue());
			if(newStation != null) newStations.add(newStation);
		}
		return new Meal(newStations,
				baseMeal.startTime, baseMeal.endTime,
				baseMeal.name, baseMeal.description);
	}
	
	private Station getInterestingPartOfStation(Station baseStation, JsonElement rule) {
		if(rule.isJsonPrimitive()) {
			if(rule.getAsJsonPrimitive().isNumber()) {
				List<MenuItem> newMenuItems = new ArrayList<>();
				int howManyItems = rule.getAsInt();
				if(howManyItems > 0) {
					for(int i = 0; i < howManyItems; i++) {
						newMenuItems.add(baseStation.menu.get(i));
					}
				} else {
					for(int i = -howManyItems; i < baseStation.menu.size(); i++) {
						newMenuItems.add(baseStation.menu.get(i));
					}
				}
				return new Station(baseStation.name, newMenuItems);
			} else if(rule.getAsJsonPrimitive().isString()) {
				if(rule.getAsString().equals("all")) {
					return baseStation;
				} else if(rule.getAsString().equals("single")) {
					StringBuilder newName = new StringBuilder();
					for(MenuItem item: baseStation.menu) {
						newName.append(item.name);
					}
					return new Station(baseStation.name, Collections.singletonList(
							new MenuItem(newName.toString(), "", Collections.emptySet())));
				} else {
					throw new IllegalStateException("Bad rule: "+rule);
				}
			} else {
				throw new IllegalStateException("Bad rule: "+rule);
			}
		} else {
			//TODO: handle objects
			return null;
		}
	}

	private Station getStationByName(List<Station> stations, String stationName) {
		for(Station station: stations) {
			if(station.name.equals(stationName)) {
				return station;
			}
		}
		throw new IllegalArgumentException("Couldn't find "+stationName);
	}
}
