package io.yancey.menufetcher.fetchers;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.google.gson.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;

public abstract class AbstractNewSodexoMenuFetcher extends AbstractMenuFetcher {
	private final int menuId;
	private final int locationId;
	private final String sitename;
	
	private HashMap<LocalDate, JsonObject> jsonCache = new HashMap<>();
	
	private String menuUrl(LocalDate day) {
		String startDateString = day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		return String.format("https://menus.sodexomyway.com/BiteMenu/MenuOnly?menuId=%d&locationId=%d&startdate=%s",
				menuId, locationId, startDateString);
	}
	
	private String hoursUrl() {
		return String.format("https://%s.sodexomyway.com/dining-near-me/hours", sitename);
	}
	
	private void fetchMenu(LocalDate day) throws MenuNotAvailableException {
		Document menuPage;
		try {
			menuPage = Jsoup.connect(menuUrl(day)).get();
		} catch (IOException e) {
			throw new MenuNotAvailableException("Error fetching menu data", e);
		}
		JsonElement menuItems = new JsonParser().parse(menuPage.getElementById("nutData").text());
		System.out.println(menuItems.getAsJsonArray().toString());
	}
	
	private JsonObject getMenuJson(LocalDate day) throws MenuNotAvailableException {
		if (!jsonCache.containsKey(day)) {
			fetchMenu(day);
		}
		return jsonCache.get(day);
	}
	
	public AbstractNewSodexoMenuFetcher(String name, String id, String sitename, int menuId, int locationId) {
		super(name, id);
		this.sitename = sitename;
		this.menuId = menuId;
		this.locationId = locationId;
	}
	
	@Override
	public Menu getMeals(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
		throw new MenuNotAvailableException("Sodexo is broken");
	}
}
