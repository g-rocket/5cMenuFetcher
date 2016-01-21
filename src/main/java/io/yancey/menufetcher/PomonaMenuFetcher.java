package io.yancey.menufetcher;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;

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
	
	private Element getMenuSpreadsheetInfo() {
		Document doc;
		try {
			doc = Jsoup.connect(getMenuUrl()).get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return doc.getElementById("menu-from-google");
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
	
	private JsonObject getSpreadsheetInfo(LocalDate day, Element menuSpreadsheetInfo) {
		JsonArray spreadsheets = getSpreadsheets(menuSpreadsheetInfo);
		LocalDate nearestMonday = (LocalDate)DayOfWeek.MONDAY.adjustInto(day);
		for(JsonElement spreadsheet: spreadsheets) {
			String spreadsheetDateString = spreadsheet.getAsJsonObject()
					.getAsJsonObject("title")
					.get("$t").getAsString();
			String[] spreadsheetDateElements = spreadsheetDateString.split("-");
			LocalDate spreadsheetDate = LocalDate.of(
					2000 + Integer.parseInt(spreadsheetDateElements[2]),
					Integer.parseInt(spreadsheetDateElements[0]),
					Integer.parseInt(spreadsheetDateElements[1]));
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
		throw new IllegalStateException("no cells feed found");
	}
	
	private JsonObject getSpreadsheetData(JsonObject spreadsheetInfo) {
		String url = getSpreadsheetUrl(spreadsheetInfo);
		try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
			sc.useDelimiter("\\A");
			String spreadsheetString = sc.hasNext()? sc.next(): "";
			return new JsonParser().parse(spreadsheetString).getAsJsonObject();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private int getWidth(JsonObject spreadsheetInfo) {
		return spreadsheetInfo.get("gs$colCount").getAsInt();
	}
	
	private int getHeight(JsonObject spreadsheetInfo) {
		return spreadsheetInfo.get("gs$rowCount").getAsInt();
	}
	
	private String[][] getSpreadsheet(JsonObject spreadsheetInfo) {
		JsonObject spreadsheetData = getSpreadsheetData(spreadsheetInfo);
		String[][] spreadsheet = new String[getWidth(spreadsheetInfo)][getHeight(spreadsheetInfo)];
		//TODO: populate spreadsheet
		return spreadsheet;
	}
	
	@Override
	public List<Meal> getMeals(LocalDate day) {
		Element menuSpreadsheetInfo = getMenuSpreadsheetInfo();
		JsonObject spreadsheetInfo = getSpreadsheetInfo(day, menuSpreadsheetInfo);
		if(spreadsheetInfo == null) {
			// couldn't find any info for requested day
			return null;
		}
		System.out.println(Arrays.deepToString(getSpreadsheet(spreadsheetInfo)));
		//TODO: extract data from spreadsheet
		return null;
	}
	
	public static void main(String[] args) {
		System.out.println(new PomonaMenuFetcher(FRANK_NAME).getMeals(LocalDate.of(2016,1,20)));
	}
}
