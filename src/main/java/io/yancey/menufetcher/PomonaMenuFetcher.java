package io.yancey.menufetcher;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import com.google.gson.*;

public class PomonaMenuFetcher implements MenuFetcher {
	private final String name;
	private static final String urlPrefix = "http://www.pomona.edu/administration/dining/menus/";
	
	public static final String FRANK_NAME = "frank";
	public static final String FRARY_NAME = "frary";
	public static final String OLDENBORG_NAME = "oldenborg";
	
	public PomonaMenuFetcher(String name) {
		this.name = name;
	}
	
	private String getMenuUrl() {
		return urlPrefix + name;
	}
	
	private Element getMenuSpreadsheetInfo(Document menuInfoPage) {
		return menuInfoPage.getElementById("menu-from-google");
	}
	
	private String getDocumentId(Element menuSpreadsheetInfo) {
		return menuSpreadsheetInfo.attr("data-google-spreadsheet-id");
	}
	
	private String getMenuType(Element menuSpreadsheetInfo) {
		// frankFrary or oldenborg
		return menuSpreadsheetInfo.attr("data-menu-type");
	}
	
	private String getDocumentUrl(Element menuSpreadsheetInfo) {
		return "https://spreadsheets.google.com/feeds/worksheets/" + 
				getDocumentId(menuSpreadsheetInfo) +
				"/public/basic?alt=json";
	}
	
	private JsonArray getSpreadsheets(Element menuSpreadsheetInfo) {
		String url = getDocumentUrl(menuSpreadsheetInfo);
		try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
			sc.useDelimiter("\\A");
			String spreadsheetsString = sc.hasNext()? sc.next(): "";
			return new JsonParser().parse(spreadsheetsString).getAsJsonObject()
					.getAsJsonObject("feed")
					.getAsJsonArray("entry");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final Pattern spreadsheetDateStringRegex = 
			Pattern.compile("([0-9][0-9]?)-([0-9][0-9]?)-([0-9][0-9])");
	private JsonObject getSpreadsheetInfo(LocalDate day, Element menuSpreadsheetInfo) {
		JsonArray spreadsheets = getSpreadsheets(menuSpreadsheetInfo);
		LocalDate nearestMonday = (LocalDate)DayOfWeek.MONDAY.adjustInto(day);
		for(JsonElement spreadsheet: spreadsheets) {
			String spreadsheetDateString = spreadsheet.getAsJsonObject()
					.getAsJsonObject("title")
					.get("$t").getAsString();
			Matcher m = spreadsheetDateStringRegex.matcher(spreadsheetDateString);
			if(!m.find()) {
				System.err.println("Invalid format for date string: " + spreadsheetDateString);
				continue;
			}
			LocalDate spreadsheetDate = LocalDate.of(
					2000 + Integer.parseInt(m.group(3)),
					Integer.parseInt(m.group(1)),
					Integer.parseInt(m.group(2)));
			if(nearestMonday.equals(spreadsheetDate)) {
				return spreadsheet.getAsJsonObject();
			}
		}
		// date not found
		return null;
	}
	
	private String getSpreadsheetUrl(JsonObject spreadsheetInfo) {
		for(JsonElement linkData: spreadsheetInfo.getAsJsonArray("link")) {
			if(linkData.getAsJsonObject().get("rel").getAsString()
					.equals("http://schemas.google.com/spreadsheets/2006#cellsfeed")) {
				return linkData.getAsJsonObject().get("href").getAsString() + "?alt=json";
			}
		}
		throw new IllegalArgumentException("no cells feed found");
	}
	
	private JsonArray getSpreadsheetData(JsonObject spreadsheetInfo) {
		String url = getSpreadsheetUrl(spreadsheetInfo);
		try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
			sc.useDelimiter("\\A");
			String spreadsheetString = sc.hasNext()? sc.next(): "";
			return new JsonParser().parse(spreadsheetString).getAsJsonObject()
					.getAsJsonObject("feed")
					.getAsJsonArray("entry");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private int getCols(JsonObject spreadsheetInfo) {
		return spreadsheetInfo.getAsJsonObject("gs$colCount")
				.get("$t").getAsInt();
	}
	
	private int getRows(JsonObject spreadsheetInfo) {
		return spreadsheetInfo.getAsJsonObject("gs$rowCount")
				.get("$t").getAsInt();
	}
	
	private int getRow(String[] position) {
		return Integer.parseInt(position[1]) - 1;
	}
	
	private int getCol(String[] position) {
		// convert AC type heading to number (29)
		// val must be int[] so it can be modified in lambda
		int[] val = {0};
		position[0].codePoints().forEachOrdered(c -> {
			val[0] *= 26;
			val[0] += Character.getNumericValue(c) - 9;
		});
		return val[0] - 1;
	}
	
	private static final Pattern positionRegex = Pattern.compile("^([A-Z]+)([1-9][0-9]*)$");
	private String[] getPosition(JsonObject cellData) {
		String position = cellData.getAsJsonObject("title")
				.get("$t").getAsString();
		Matcher m = positionRegex.matcher(position);
		if(!m.find()) {
			throw new IllegalArgumentException("cell title doesn't match expected format");
		}
		return new String[]{m.group(1), m.group(2)};
	}
	
	private String getContents(JsonObject cellData) {
		return cellData.getAsJsonObject("content")
				.get("$t").getAsString();
	}
	
	private String[][] getSpreadsheet(JsonObject spreadsheetInfo) {
		JsonArray spreadsheetData = getSpreadsheetData(spreadsheetInfo);
		String[][] spreadsheet = new String[getRows(spreadsheetInfo)][getCols(spreadsheetInfo)];
		for(JsonElement cellData: spreadsheetData) {
			String[] position = getPosition(cellData.getAsJsonObject());
			spreadsheet[getRow(position)][getCol(position)] = getContents(cellData.getAsJsonObject());
		}
		for(int row = 0; row < spreadsheet.length; row++) {
			for(int col = 0; col < spreadsheet[row].length; col++) {
				if(spreadsheet[row][col] == null) {
					spreadsheet[row][col] = "";
				}
			}
		}
		return spreadsheet;
	}

	private static final Pattern dayRangeRegex = Pattern.compile(
			"(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day(?:-(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day)?");
	private static final Pattern mealTimePattern = Pattern.compile(
			"([A-Z][a-z]*(?: [A-Z][a-z]*)*): ?" +
			"([1-9][0-9]?):([0-9][0-9]) (a|p)\\.m\\. - " +
			"([1-9][0-9]?):([0-9][0-9]) (a|p)\\.m\\.");
	private Map<String, LocalTimeRange> getDiningHours(Document menuInfoPage, DayOfWeek dayOfWeek) {
		Element hoursElement = menuInfoPage.getElementsByClass("dining-hours-top").first();
		for(Element column: hoursElement.children()) {
			if(!column.className().startsWith("dining-days-col-")) continue;
			String dayRangeString = column.getElementsByClass("dining-days").first().ownText();
			Matcher dayRangeMatcher = dayRangeRegex.matcher(dayRangeString);
			if(!dayRangeMatcher.find()) {
				System.err.println("Invalid day range: "+dayRangeString);
				continue;
			}
			DayOfWeek startDay = DayOfWeek.valueOf((dayRangeMatcher.group(1)+"day").toUpperCase());
			DayOfWeek endDay;
			if(dayRangeMatcher.group(2) != null) {
				endDay = DayOfWeek.valueOf((dayRangeMatcher.group(2)+"day").toUpperCase());
			} else {
				endDay = startDay;
			}
			if(dayOfWeek.compareTo(startDay) >= 0 && dayOfWeek.compareTo(endDay) <= 0) {
				Element hoursForDay = column.getElementsByClass("dining-hours").first();
				Map<String, LocalTimeRange> times = new HashMap<>();
				for(Matcher m = mealTimePattern.matcher(hoursForDay.text()); m.find();) {
					String name = m.group(1);
					LocalTime startTime = LocalTime.of(
							Integer.parseInt(m.group(2))%12 + (m.group(4).equals("p")? 12: 0),
							Integer.parseInt(m.group(3)));
					LocalTime endTime = LocalTime.of(
							Integer.parseInt(m.group(5))%12 + (m.group(7).equals("p")? 12: 0),
							Integer.parseInt(m.group(6)));
					times.put(name, new LocalTimeRange(startTime, endTime));
				}
				return times;
			}
		}
		System.out.println(hoursElement);
		throw new IllegalArgumentException("The hours for the specified day could not be found");
	}
	
	@Override
	public List<Meal> getMeals(LocalDate day) {
		Document menuInfoPage;
		try {
			menuInfoPage = Jsoup.connect(getMenuUrl()).get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Element menuSpreadsheetInfo = getMenuSpreadsheetInfo(menuInfoPage);
		JsonObject spreadsheetInfo = getSpreadsheetInfo(day, menuSpreadsheetInfo);
		if(spreadsheetInfo == null) {
			// couldn't find any info for requested day
			return Collections.emptyList();
		}
		String[][] spreadsheet = getSpreadsheet(spreadsheetInfo);
		
		Map<String, LocalTimeRange> hoursTable = getDiningHours(menuInfoPage, day.getDayOfWeek());
		
		String menuType = getMenuType(menuSpreadsheetInfo);
		if(menuType.equals("frankFrary")) {
			return frankFraryParseMeals(spreadsheet, hoursTable, day.getDayOfWeek());
		} else if(menuType.equals("oldenborg")) {
			return oldenborgParseMeals(spreadsheet, hoursTable, day.getDayOfWeek());
		} else {
			throw new IllegalStateException("menu returned invalid type: " + menuType);
		}
	}

	private List<Meal> frankFraryParseMeals(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, DayOfWeek dayOfWeek) {
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
			if(frankFraryMealExists(spreadsheet, startRow, column)) {
				meals.add(frankFraryCreateMeal(spreadsheet, startRow, column, hoursTable));
			}
		}
		return meals;
	}

	private boolean frankFraryMealExists(String[][] spreadsheet, int startRow, int column) {
		if(spreadsheet[startRow][column].isEmpty()) return false;
		if(spreadsheet[startRow + 1][column].equalsIgnoreCase("CLOSED")) return false;
		for(int station = 0; !spreadsheet[startRow + station + 2][1].isEmpty(); station++) {
			if(!spreadsheet[startRow + station + 2][column].isEmpty()) return true;
		}
		return false;
	}
	
	private Meal frankFraryCreateMeal(String[][] spreadsheet, int startRow, int column, Map<String, LocalTimeRange> hoursTable) {
		String name = spreadsheet[startRow][column].trim();
		LocalTimeRange hours = hoursTable.get(name);
		String description = spreadsheet[startRow + 1][column];
		List<Station> stations = new ArrayList<>();
		for(int station = 0; !spreadsheet[startRow + station + 2][1].isEmpty(); station++) {
			if(!spreadsheet[startRow + station + 2][column].isEmpty()) {
				stations.add(frankFraryCreateStation(spreadsheet, startRow + station + 2, column));
			}
		}
		return new Meal(stations, hours.startTime, hours.endTime, name, description);
	}

	private Station frankFraryCreateStation(String[][] spreadsheet, int row, int column) {
		return new Station(spreadsheet[row][1], 
				Arrays.stream(spreadsheet[row][column].split(","))
				.map(itemName -> new MenuItem(itemName.trim(), "", Collections.emptySet()))
				.collect(Collectors.toList()));
	}

	private List<Meal> oldenborgParseMeals(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, DayOfWeek dayOfWeek) {
		if(!oldenborgMealExists(spreadsheet, dayOfWeek)) return Collections.emptyList();
		return Collections.singletonList(oldenborgCreateMeal(spreadsheet, hoursTable, dayOfWeek.getValue()));
	}

	private boolean oldenborgMealExists(String[][] spreadsheet, DayOfWeek dayOfWeek) {
		if(dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) return false;
		int column = dayOfWeek.getValue();
		if(spreadsheet[2][column].equalsIgnoreCase("CLOSED")) return false;
		for(int row = 3; !spreadsheet[row][column].isEmpty(); row++) {
			if(!spreadsheet[row][column].isEmpty()) return true;
		}
		return false;
	}

	private Meal oldenborgCreateMeal(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, int column) {
		String name = hoursTable.keySet().iterator().next();
		LocalTimeRange hours = hoursTable.get(name);
		String description = spreadsheet[2][column];
		List<Station> stations = new ArrayList<>();
		for(int station = 0; !spreadsheet[station + 3][column].isEmpty(); station++) {
			if(!spreadsheet[station + 3][column].isEmpty()) {
				stations.add(oldenborgCreateStation(spreadsheet, station + 3, column));
			}
		}
		return new Meal(stations, hours.startTime, hours.endTime, name, description);
	}

	private Station oldenborgCreateStation(String[][] spreadsheet, int row, int column) {
		return new Station(spreadsheet[row][0], 
				Arrays.stream(spreadsheet[row][column].split(","))
				.map(itemName -> new MenuItem(itemName.trim(), "", Collections.emptySet()))
				.collect(Collectors.toList()));
	}

	public static void main(String[] args) {
		System.out.println(new PomonaMenuFetcher(FRARY_NAME).getMeals(LocalDate.of(2016,1,24)));
		//System.out.println(new PomonaMenuFetcher(OLDENBORG_NAME).getMeals(LocalDate.of(2016,1,20)));
	}
}
