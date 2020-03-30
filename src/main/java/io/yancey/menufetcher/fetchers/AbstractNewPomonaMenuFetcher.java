package io.yancey.menufetcher.fetchers;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.google.gson.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;

public abstract class AbstractNewPomonaMenuFetcher extends AbstractMenuFetcher {
	private final String sitename;
	private static final String urlPrefix = "http://www.pomona.edu/administration/dining/menus/";

	protected Map<String, Document> documentCache = new HashMap<>();
	protected Map<String, JsonElement> jsonCache = new HashMap<>();

	public AbstractNewPomonaMenuFetcher(String name, String id, String sitename) {
		super(name, id);
		this.sitename = sitename;
	}
	
	private String getMenuUrl() {
		return urlPrefix + sitename;
	}
	
	private static Element getMenuJsonInfo(Document menuInfoPage) {
		return menuInfoPage.getElementById("dining-menu-from-json");
	}
	
	private static String getJsonUrl(Element menuJsonInfo) throws MalformedMenuException {
		return menuJsonInfo.attr("data-dining-menu-json-url");
	}
	
	private JsonObject getMenuJson(Element jsonInfo)
			throws MalformedMenuException, MenuNotAvailableException {
		String url = getJsonUrl(jsonInfo);
		if(!jsonCache.containsKey(url)) {
			try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
				sc.useDelimiter("\\A");
				String jsonpString = sc.hasNext()? sc.next(): "/**/ menuData({});";
				String jsonString = jsonpString.substring(jsonpString.indexOf("menuData(") + 9, jsonpString.lastIndexOf(")"));
				jsonCache.put(url,
						new JsonParser().parse(jsonString).getAsJsonObject()
						.getAsJsonObject("feed")
						.getAsJsonArray("entry"));
			} catch (MalformedURLException e) {
				throw new MalformedMenuException("Invalid spreadsheet url",e);
			} catch (IOException e) {
				throw new MenuNotAvailableException("Error fetching spreadsheet data",e);
			}
		}
		return jsonCache.get(url).getAsJsonObject();
	}

	private static final Pattern dayRangeRegex = Pattern.compile(
			"(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day(?:\\s*-\\s*(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day)?");
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
		Element jsonInfo = getMenuJsonInfo(menuInfoPage);
		JsonObject menuJson = getMenuJson(jsonInfo);
		if(menuJson == null) {
			// couldn't find any info for requested day
			return new Menu(name, id, getMenuUrl(), Collections.emptyList());
		}
		
		Map<String, LocalTimeRange> hoursTable = getDiningHours(menuInfoPage, day.getDayOfWeek());
		
		if(hoursTable.isEmpty()) {
			// closed for the day
			return new Menu(name, id, getMenuUrl(), Collections.emptyList());
		}

		return new Menu(name, id, getMenuUrl(), parseMeals(menuJson, hoursTable, day));
	}

	private List<Meal> parseMeals(JsonObject eatecExchange, Map<String, LocalTimeRange> hoursTable, LocalDate day) {
		List<Meal> meals = new ArrayList<>();
		
		JsonArray menu = eatecExchange.getAsJsonObject("EatecExchange").getAsJsonArray("menu");
		String expectedDate = day.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		
		for (JsonElement meal: menu) {
			String servDate = meal.getAsJsonObject().get("@servedate").getAsString();
			if (servDate.equals(expectedDate)) meals.add(parseMeal(meal.getAsJsonObject(), hoursTable));
		}
		
		return meals;
	}

	private Meal parseMeal(JsonObject meal, Map<String, LocalTimeRange> hoursTable) {
		String name = meal.get("@mealperiodname").getAsString();
		String description = meal.get("@menubulletin").getAsString();
		return new Meal(null, hoursTable.get(name), name, description);
	}
}
