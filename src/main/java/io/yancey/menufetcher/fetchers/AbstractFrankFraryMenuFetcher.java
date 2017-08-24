package io.yancey.menufetcher.fetchers;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import io.yancey.menufetcher.data.*;

public class AbstractFrankFraryMenuFetcher extends AbstractPomonaMenuFetcher {
	public AbstractFrankFraryMenuFetcher(String name, String id, String sitename) {
		super(name, id, sitename);
	}

	@Override
	protected List<Meal> parseMeals(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, DayOfWeek dayOfWeek) {
		String dayName = dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ROOT);
		int startRow = -1;
		for(int row = 0; row < spreadsheet.length; row++) {
			if(dayName.equals(spreadsheet[row][0])) {
				startRow = row - 1;
				break;
			}
		}
		if(startRow < 0) {
			throw new IllegalArgumentException("Spreadsheet doesn't seem to contain data for " + dayOfWeek);
		}
		List<Meal> meals = new ArrayList<>(3);
		for(int column = 2; column < spreadsheet[startRow].length; column++) {
			if(mealExists(spreadsheet, startRow, column)) {
				meals.add(createMeal(spreadsheet, startRow, column, hoursTable, dayOfWeek));
			}
		}
		return meals;
	}

	private static boolean mealExists(String[][] spreadsheet, int startRow, int column) {
		if(spreadsheet[startRow][column].isEmpty()) return false;
		if(spreadsheet[startRow + 1][column].equalsIgnoreCase("CLOSED")) return false;
		for(int station = 0; !spreadsheet[startRow + station + 2][1].isEmpty(); station++) {
			if(!spreadsheet[startRow + station + 2][column].isEmpty()) return true;
		}
		if(!spreadsheet[startRow + 1][column].isEmpty()) {
			return true;
		}
		return false;
	}
	
	private static Meal createMeal(String[][] spreadsheet, int startRow, int column, Map<String, LocalTimeRange> hoursTable, DayOfWeek dayOfWeek) {
		String name = spreadsheet[startRow][column].trim();
		if(name.equals("Brakfast")) name = "Breakfast"; // fix someone else's typo
		if(name.equals("Breakfast Bar")) name = "Breakfast"; // for consistency
		LocalTimeRange hours = hoursTable.get(name);
		String description = spreadsheet[startRow + 1][column];
		List<Station> stations = new ArrayList<>();
		for(int station = 0; !spreadsheet[startRow + station + 2][1].isEmpty() && !spreadsheet[startRow + station + 2][0].equals("Day"); station++) {
			if(!spreadsheet[startRow + station + 2][column].isEmpty()) {
				stations.add(createStation(spreadsheet, startRow + station + 2, column));
			}
		}
		if(!spreadsheet[startRow + 1][column].isEmpty() && stations.isEmpty()) {
			stations.add(createDefaultBrunchStation(spreadsheet, startRow + 1, column));
		}
		return new Meal(stations, hours, name, description);
	}

	private static Station createDefaultBrunchStation(String[][] spreadsheet, int row, int column) {
		return new Station("Brunch", 
				Arrays.stream(spreadsheet[row][column].split(","))
				.map(itemName -> new MenuItem(itemName.trim(), "", Collections.emptySet()))
				.collect(Collectors.toList()));
	}

	private static Station createStation(String[][] spreadsheet, int row, int column) {
		return new Station(spreadsheet[row][1], 
				Arrays.stream(spreadsheet[row][column].split(","))
				.map(itemName -> new MenuItem(itemName.trim(), "", Collections.emptySet()))
				.collect(Collectors.toList()));
	}

	@Override
	protected boolean isRightType(String menuType) {
		return menuType == "frankFrary";
	}
}
