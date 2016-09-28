package io.yancey.menufetcher.fetchers;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.dininghalls.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.parser.*;

import com.google.gson.*;

public class BonAppetitMenuFetcher extends AbstractMenuFetcher {
	private final int cafeId;
	private final String publicMenuUrlPrefix, publicMenuUrlCafe;
	
	public BonAppetitMenuFetcher(String name, String id, int cafeId,
			String publicMenuUrlPrefix, String publicMenuUrlCafe) {
		super(name, id);
		this.cafeId = cafeId;
		this.publicMenuUrlPrefix = publicMenuUrlPrefix;
		this.publicMenuUrlCafe = publicMenuUrlCafe;
	}
	
	private String getJsonMenuUrl(LocalDate day) {
		return String.format("http://legacy.cafebonappetit.com/api/2/menus?format=json&cafe=%d&date=%s",
				cafeId, day.format(DateTimeFormatter.ISO_LOCAL_DATE));
	}
	
	private JsonElement getMenuJson(LocalDate day)
			throws MenuNotAvailableException, MalformedMenuException {
		String url = getJsonMenuUrl(day);
		try(Scanner sc = new Scanner(new URL(url).openStream(), "UTF-8")) {
			sc.useDelimiter("\\A");
			String menuString = sc.hasNext()? sc.next(): "";
			return new JsonParser().parse(menuString);
		} catch (MalformedURLException e) {
			throw new MalformedMenuException("Invalid json url", e);
		} catch (IOException e) {
			throw new MenuNotAvailableException("Error fetching json",e);
		}
	}

	@Override
	public Menu getMeals(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
		JsonObject menuData = getMenuJson(day).getAsJsonObject();
		JsonArray mealsDataParts = menuData
				.getAsJsonArray("days")
				.get(0).getAsJsonObject()
				.getAsJsonObject("cafes")
				.getAsJsonObject(Integer.toString(cafeId))
				.getAsJsonArray("dayparts");
		JsonObject itemsData = menuData
				.getAsJsonObject("items");
		JsonArray mealsData;
		if(mealsDataParts.size() > 0 && mealsDataParts.get(0).getAsJsonArray().size() > 0) {
			mealsData = mealsDataParts
					.get(0).getAsJsonArray();
		} else if(isInCurrentWeek(day)) {
			System.err.printf("%s missing dayparts: trying RSS feed\n", name);
			mealsData = getMealsDataFromRSS(day, itemsData);
			if(mealsData == null) {
				System.err.println("fetching RSS failed");
				return new Menu(name, id, getMenuUrl(day), Collections.emptyList());
			}
		} else {
			System.err.printf("%s missing dayparts; RSS not available\n", name);
			return new Menu(name, id, getMenuUrl(day), Collections.emptyList());
		}
		List<Meal> meals = new ArrayList<>(3);
		for(JsonElement mealData: mealsData) {
			meals.add(createMeal(mealData.getAsJsonObject(), itemsData));
		}
		return new Menu(name, id, getMenuUrl(day), meals);
	}
	
	private JsonArray getMealsDataFromRSS(LocalDate day, JsonObject itemsData) {
		Document rssFeed;
		try {
			rssFeed = Jsoup.connect(getRssUrl()).timeout(10*1000).get();
		} catch (IOException e) {
			System.err.println("error loading RSS");
			return null;
		}
		
		for(Element item: rssFeed.getElementsByTag("item")) {
			String dateString = Parser.unescapeEntities(item.getElementsByTag("title").get(0).text(), false);
			LocalDate itemDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
			if(itemDate.equals(day)) {
				String itemText = Parser.unescapeEntities(item.getElementsByTag("description").get(0).text(), false);
				return formatAsMealsData(itemText, itemsData);
			}
		}
		
		System.err.println("no items matched");
		return null;
	}

	private static final Pattern mealTitleRegex = Pattern.compile("<h3>([^<]+)</h3>");
	private JsonArray formatAsMealsData(String feedItemText, JsonObject itemsData) {
		JsonArray mealsData = new JsonArray();
		Matcher mealTitleMatcher = mealTitleRegex.matcher(feedItemText);
		if(!mealTitleMatcher.find()) return null;
		while(!mealTitleMatcher.hitEnd()) {
			String mealTitle = mealTitleMatcher.group(1);
			int mealStart = mealTitleMatcher.end();
			int mealEnd;
			if(mealTitleMatcher.find()) {
				mealEnd = mealTitleMatcher.start();
			} else {
				mealEnd = feedItemText.length();
			}
			
			JsonObject mealData = createMealDataFromRss(mealTitle, 
					feedItemText.substring(mealStart, mealEnd), itemsData);
			if(mealData != null) {
				mealsData.add(mealData);
			} else {
				System.err.printf("error adding %s\n", mealTitle);
			}
		}
		return mealsData;
	}

	private static final Pattern itemRegex = Pattern.compile("<h4>\\s*\\[([^]]+)\\]\\s*([^<]+)</h4>");
	private JsonObject createMealDataFromRss(String mealTitle, String mealDataString, JsonObject itemsData) {
		Map<String, JsonArray> stationsMap = new HashMap<>();
		Matcher itemMatcher = itemRegex.matcher(mealDataString);
		
		while(itemMatcher.find()) {
			String stationName = itemMatcher.group(1);
			String itemName = itemMatcher.group(2);
			
			if(!stationsMap.containsKey(stationName)) {
				stationsMap.put(stationName, new JsonArray());
			}
			String itemId = guessItemId(itemName, itemsData);
			if(itemId != null) {
				stationsMap.get(stationName).add(itemId);
			} else {
				System.err.println("error getting id for "+itemName);
			}
		}
		
		JsonArray stations = new JsonArray();
		for(Map.Entry<String, JsonArray> stationData: stationsMap.entrySet()) {
			JsonObject station = new JsonObject();
			station.addProperty("label", stationData.getKey());
			station.add("items", stationData.getValue());
			stations.add(station);
		}
		
		JsonObject mealData = new JsonObject();
		mealData.addProperty("label", mealTitle);
		mealData.add("stations", stations);
		
		//TODO: fake these better
		mealData.addProperty("starttime", "00:00");
		mealData.addProperty("endtime", "00:00");
		
		return mealData;
	}

	private String guessItemId(String itemName, JsonObject itemsData) {
		itemName = itemName.trim();
		if(itemName.endsWith("&nbsp;")) itemName = itemName.substring(0, itemName.length() - 6);
		if(itemName.endsWith("\u00a0")) itemName = itemName.substring(0, itemName.length() - 1);
		for(Map.Entry<String, JsonElement> itemData: itemsData.entrySet()) {
			if(itemData.getValue().getAsJsonObject().get("label").getAsString().equalsIgnoreCase(itemName)) {
				return itemData.getKey();
			}
		}
		return null;
	}

	private String getRssUrl() {
		return "http://legacy.cafebonappetit.com/rss/menu/" + cafeId;
	}

	private static boolean isInCurrentWeek(LocalDate day) {
		LocalDate today = LocalDate.now(ZoneId.of("America/Los_Angeles"));
		LocalDate startOfWeek = (LocalDate)DayOfWeek.MONDAY.adjustInto(today);
		LocalDate endOfWeek = (LocalDate)DayOfWeek.SUNDAY.adjustInto(today);
		return !today.isBefore(startOfWeek) && !today.isAfter(endOfWeek);
	}

	private Meal createMeal(JsonObject mealData, JsonObject itemsData) {
		List<Station> stations = new ArrayList<>();
		for(JsonElement stationData: mealData.getAsJsonArray("stations")) {
			stations.add(createStation(stationData.getAsJsonObject(), itemsData));
		}
		return new Meal(stations,
				LocalTime.parse(mealData.get("starttime").getAsString()),
				LocalTime.parse(mealData.get("endtime").getAsString()),
				mealData.get("label").getAsString(), "");
	}

	private Station createStation(JsonObject stationData, JsonObject itemsData) {
		List<MenuItem> items = new ArrayList<>();
		for(JsonElement itemId: stationData.getAsJsonArray("items")) {
			items.add(createMenuItem(itemsData.getAsJsonObject(itemId.getAsString())));
		}
		return new Station(stationData.get("label").getAsString(), items);
	}

	private MenuItem createMenuItem(JsonObject itemData) {
		Set<String> tags;
		if(itemData.get("cor_icon").isJsonObject()) {
			JsonObject tagArray = itemData.getAsJsonObject("cor_icon");
			tags = tagArray.entrySet().parallelStream()
					.map((e) -> e.getValue().getAsString())
					.collect(Collectors.toSet());
		} else {
			tags = Collections.emptySet();
		}
		return new MenuItem(itemData.get("label").getAsString(),
				itemData.get("description").getAsString(), tags);
	}

	public static void main(String[] args) throws MenuNotAvailableException, MalformedMenuException {
		System.out.println(new PitzerMenuFetcher().getMeals(java.time.LocalDate.of(2016, 8, 28)));
	}
	
	private String getMenuUrl(LocalDate day) {
		return "http://" + publicMenuUrlPrefix + ".cafebonappetit.com/cafe/" + publicMenuUrlCafe + "/" + day.toString();
	}

}
