package io.yancey.menufetcher.fetchers;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import io.yancey.menufetcher.data.*;

public class AbstractOldenborgMenuFetcher extends AbstractPomonaMenuFetcher {
	public AbstractOldenborgMenuFetcher(String name, String id, String sitename) {
		super(name, id, sitename);
	}

	@Override
	protected List<Meal> parseMeals(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, DayOfWeek dayOfWeek) {
		if(!mealExists(spreadsheet, dayOfWeek)) return Collections.emptyList();
		return Collections.singletonList(createMeal(spreadsheet, hoursTable, dayOfWeek.getValue()));
	}

	private static boolean mealExists(String[][] spreadsheet, DayOfWeek dayOfWeek) {
		if(dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) return false;
		int column = dayOfWeek.getValue();
		if(spreadsheet[2][column].equalsIgnoreCase("CLOSED")) return false;
		for(int row = 3; !spreadsheet[row][column].isEmpty(); row++) {
			if(!spreadsheet[row][column].isEmpty()) return true;
		}
		return false;
	}

	private static Meal createMeal(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, int column) {
		String name = hoursTable.keySet().iterator().next();
		LocalTimeRange hours = hoursTable.get(name);
		String description = spreadsheet[2][column];
		List<Station> stations = new ArrayList<>();
		for(int station = 0; !spreadsheet[station + 3][column].isEmpty(); station++) {
			if(!spreadsheet[station + 3][column].isEmpty()) {
				stations.add(createStation(spreadsheet, station + 3, column));
			}
		}
		return new Meal(stations, hours, name, description);
	}

	private static Station createStation(String[][] spreadsheet, int row, int column) {
		return new Station(spreadsheet[row][0], 
				Arrays.stream(spreadsheet[row][column].split(","))
				.map(itemName -> new MenuItem(itemName.trim(), "", Collections.emptySet()))
				.collect(Collectors.toList()));
	}

	@Override
	protected boolean isRightType(String menuType) {
		return menuType == "oldenborg";
	}
}
