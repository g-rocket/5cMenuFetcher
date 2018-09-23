package io.yancey.menufetcher.fetchers;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.google.gson.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;

public abstract class AbstractPomonaMenuFetcher extends AbstractMenuFetcher {
	private final String sitename;
	private static final String urlPrefix = "http://www.pomona.edu/administration/dining/menus/";

	protected Map<String, Document> documentCache = new HashMap<>();
	protected Map<String, JsonElement> jsonCache = new HashMap<>();
	
	public AbstractPomonaMenuFetcher(String name, String id, String sitename) {
		super(name, id);
		this.sitename = sitename;
	}
	
	private String getMenuUrl() {
		return urlPrefix + sitename;
	}
	
	private static Element getMenuSpreadsheetInfo(Document menuInfoPage) {
		return menuInfoPage.getElementById("menu-from-google");
	}
	
	private static String getDocumentId(Element menuSpreadsheetInfo) {
		return menuSpreadsheetInfo.attr("data-google-spreadsheet-id");
	}
	
	private static String getMenuType(Element menuSpreadsheetInfo) {
		// frankFrary or oldenborg
		return menuSpreadsheetInfo.attr("data-menu-type");
	}
	
	// to view spreadsheet, see
	// https://docs.google.com/spreadsheets/d/$spreadsheetId/pubhtml
	private static String getDocumentUrl(Element menuSpreadsheetInfo) {
		return "https://spreadsheets.google.com/feeds/worksheets/" + 
				getDocumentId(menuSpreadsheetInfo) +
				"/public/basic?alt=json";
	}
	
	private JsonArray getSpreadsheets(Element menuSpreadsheetInfo)
			throws MalformedMenuException, MenuNotAvailableException {
		String url = getDocumentUrl(menuSpreadsheetInfo);
		if(!jsonCache.containsKey(url)) {
			try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
				sc.useDelimiter("\\A");
				String spreadsheetsString = sc.hasNext()? sc.next(): "";
				jsonCache.put(url,
						new JsonParser().parse(spreadsheetsString).getAsJsonObject()
						.getAsJsonObject("feed")
						.getAsJsonArray("entry"));
			} catch (MalformedURLException e) {
				throw new MalformedMenuException("Invalid spreadsheets url", e);
			} catch (IOException e) {
				throw new MenuNotAvailableException("Error fetching spreadsheets",e);
			}
		}
		return jsonCache.get(url).getAsJsonArray();
	}
	
	private static final Pattern spreadsheetDateStringRegex = 
			Pattern.compile("([0-9][0-9]?)-([0-9][0-9]?)-([0-9][0-9])");
	private JsonObject getSpreadsheetInfo(LocalDate day, Element menuSpreadsheetInfo)
			throws MalformedMenuException, MenuNotAvailableException {
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
			LocalDate spreadsheetDate;
			try {
				spreadsheetDate = LocalDate.of(
						2000 + Integer.parseInt(m.group(3)),
						Integer.parseInt(m.group(1)),
						Integer.parseInt(m.group(2)));
			} catch(DateTimeException e){
				System.err.println("Invalid date string: " + spreadsheetDateString);
				continue;
			}
			if(nearestMonday.equals(spreadsheetDate)) {
				return spreadsheet.getAsJsonObject();
			}
		}
		// date not found
		return null;
	}
	
	private static String getSpreadsheetUrl(JsonObject spreadsheetInfo) {
		for(JsonElement linkData: spreadsheetInfo.getAsJsonArray("link")) {
			if(linkData.getAsJsonObject().get("rel").getAsString()
					.equals("http://schemas.google.com/spreadsheets/2006#cellsfeed")) {
				return linkData.getAsJsonObject().get("href").getAsString() + "?alt=json";
			}
		}
		throw new IllegalArgumentException("no cells feed found");
	}
	
	private JsonArray getSpreadsheetData(JsonObject spreadsheetInfo)
			throws MalformedMenuException, MenuNotAvailableException {
		String url = getSpreadsheetUrl(spreadsheetInfo);
		if(!jsonCache.containsKey(url)) {
			try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
				sc.useDelimiter("\\A");
				String spreadsheetString = sc.hasNext()? sc.next(): "";
				jsonCache.put(url,
						new JsonParser().parse(spreadsheetString).getAsJsonObject()
						.getAsJsonObject("feed")
						.getAsJsonArray("entry"));
			} catch (MalformedURLException e) {
				throw new MalformedMenuException("Invalid spreadsheet url",e);
			} catch (IOException e) {
				throw new MenuNotAvailableException("Error fetching spreadsheet data",e);
			}
		}
		return jsonCache.get(url).getAsJsonArray();
	}
	
	private static int getCols(JsonObject spreadsheetInfo) {
		return spreadsheetInfo.getAsJsonObject("gs$colCount")
				.get("$t").getAsInt();
	}
	
	private static int getRows(JsonObject spreadsheetInfo) {
		return spreadsheetInfo.getAsJsonObject("gs$rowCount")
				.get("$t").getAsInt();
	}
	
	private static int getRow(String[] position) {
		return Integer.parseInt(position[1]) - 1;
	}
	
	private static int getCol(String[] position) {
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
	private static String[] getPosition(JsonObject cellData) {
		String position = cellData.getAsJsonObject("title")
				.get("$t").getAsString();
		Matcher m = positionRegex.matcher(position);
		if(!m.find()) {
			throw new IllegalArgumentException("cell title doesn't match expected format");
		}
		return new String[]{m.group(1), m.group(2)};
	}
	
	private static String getContents(JsonObject cellData) {
		return cellData.getAsJsonObject("content")
				.get("$t").getAsString();
	}
	
	private String[][] getSpreadsheet(JsonObject spreadsheetInfo)
			throws MalformedMenuException, MenuNotAvailableException {
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
			"([1-9][0-9]?)(?::([0-9][0-9]))? (a|p)\\.m\\. - " +
			"([1-9][0-9]?)(?::([0-9][0-9]))? (a|p)\\.m\\.");
	private static Map<String, LocalTimeRange> getDiningHours(Document menuInfoPage, DayOfWeek dayOfWeek) throws MalformedMenuException {
		for(Element hoursElement: menuInfoPage.getElementsByClass("dining-hours-top")) {
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
								m.group(3) == null? 0: Integer.parseInt(m.group(3)));
						LocalTime endTime = LocalTime.of(
								Integer.parseInt(m.group(5))%12 + (m.group(7).equals("p")? 12: 0),
								m.group(6) == null? 0: Integer.parseInt(m.group(6)));
						times.put(name, new LocalTimeRange(startTime, endTime));
						if(name.equals("Continental Breakfast")) {
							// work around frary bug on weekends
							times.put("Breakfast", new LocalTimeRange(startTime, endTime));
						}
					}
					return times;
				}
			}
		}
		throw new MalformedMenuException("The hours for the specified day could not be found");
	}
	
	@Override
	public Menu getMeals(LocalDate day) throws MalformedMenuException, MenuNotAvailableException {
		if(!documentCache.containsKey(getMenuUrl())) {
			try {
				documentCache.put(getMenuUrl(), Jsoup.connect(getMenuUrl()).timeout(10*1000).get());
			} catch (IOException e) {
				throw new MenuNotAvailableException("Error fetching menu info",e);
			}
		}
		Document menuInfoPage = documentCache.get(getMenuUrl());
		Element menuSpreadsheetInfo = getMenuSpreadsheetInfo(menuInfoPage);
		JsonObject spreadsheetInfo = getSpreadsheetInfo(day, menuSpreadsheetInfo);
		if(spreadsheetInfo == null) {
			// couldn't find any info for requested day
			return new Menu(name, id, getMenuUrl(), Collections.emptyList());
		}
		String[][] spreadsheet = getSpreadsheet(spreadsheetInfo);
		
		String menuType = getMenuType(menuSpreadsheetInfo);
		if(!isRightType(menuType)) {
			throw new MalformedMenuException("Wrong menu type: "+menuType);
		}
		
		Map<String, LocalTimeRange> hoursTable = getDiningHours(menuInfoPage, day.getDayOfWeek());
		
		if(hoursTable.isEmpty()) {
			// closed for the day
			return new Menu(name, id, getMenuUrl(), Collections.emptyList());
		}

		return new Menu(name, id, getMenuUrl(), parseMeals(spreadsheet, hoursTable, day.getDayOfWeek()));
	}

	protected abstract boolean isRightType(String menuType);
	protected abstract List<Meal> parseMeals(String[][] spreadsheet, Map<String, LocalTimeRange> hoursTable, DayOfWeek dayOfWeek) throws MalformedMenuException;
}
