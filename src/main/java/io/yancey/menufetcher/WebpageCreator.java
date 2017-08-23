package io.yancey.menufetcher;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.io.*;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;

public class WebpageCreator {
	private static final String NOT_FOUND_PAGE_ID = 
			"29b834b1c0e9c7c946c6dc1d7a49c2218be33fefb150e17f2e6bf8a7ee42fec7";
	
	public static Document createBlankpage(LocalDate day) {
		Document template = loadTemplate();
		setupDayList(template, day);
		deleteTables(template);
		addBlankPageFlair(template, day);
		return template;
	}
	
	private static void addBlankPageFlair(Document template, LocalDate day) {
		Element notFound = template.getElementById("header").parent().appendElement("p");
		notFound.text("The menu for "+day+" is not available yet.");
		notFound.attr("id", "not-found");
		// mark as not found page
		notFound.addClass(NOT_FOUND_PAGE_ID);
	}

	private static void deleteTables(Document template) {
		template.getElementById("menu-summary").remove();
		template.getElementById("menu").remove();
	}

	public static Document createWebpage(LocalDate day, List<Menu> menus) {
		Document template = loadTemplate();
		setupDayList(template, day);
		addMenus(template, menus);
		return template;
	}
	
	private static Document loadTemplate() {
		try(InputStream templateFile = WebpageCreator.class.getResourceAsStream("/template.html")) {
			return Jsoup.parse(templateFile, "UTF-8", "");
		} catch (IOException e) {
			throw new RuntimeException("Template not found",e);
		}
	}
	
	private static void addMenus(Document template, List<Menu> menus) {
		//System.out.println(menus);
		addMenuSummary(template, menus);
		addFullMenus(template, menus);
	}

	private static void addMenuSummary(Document template, List<Menu> menus) {
		for(Menu menu: menus) {
			Element nameCell = template.getElementById("menu-summary-title-" + menu.diningHallId);
			nameCell.addClass(menu.diningHallId);
			nameCell.addClass("colored");
			Element nameLink = nameCell.appendElement("a");
			nameLink.attr("id", "menu-summary-"+menu.diningHallId+"-link-to-full");
			nameLink.text(menu.diningHallName);
		}
		boolean hasLunch = false;
		for(Menu menu: menus) {
			for(Meal meal: InterestingItemExtractor.instance.getInterestingItems(menu)) {
				if(meal.name.equalsIgnoreCase("lunch")) {
					hasLunch = true;
				}
				Element cell = template.getElementById("menu-summary-"+meal.name.toLowerCase()+"-"+menu.diningHallId);
				cell.addClass(menu.diningHallId);
				cell.addClass("colored");
				if(!meal.description.isEmpty()) {
					cell.appendText(meal.description);
				}
				Element list = cell.appendElement("ul").addClass("menu-item-list");
				for(Station station: meal.stations) {
					for(MenuItem item: station.menu) {
						item.createElement(list.appendElement("li"));
					}
				}
			}
		}
		if(!hasLunch) template.getElementById("menu-summary-lunch").remove();
	}

	private static void addFullMenus(Document template, List<Menu> menus) {
		Element menuTable = template.getElementById("menu").child(0);
		for(Menu menu: menus) {
			addStationNames(template, menuTable, menu);
		}
		
		List<String> mealTitles = setupMealTitles(template, menus);
		
		for(String mealTitle: mealTitles) {
			addFoodForMeal(mealTitle, menus, menuTable);
		}
	}
	
	private static void addFoodForMeal(String mealTitle, List<Menu> menus, Element menuTable) {
		for(Menu menu: menus) {
			Meal thisMeal = null;
			for(Meal meal: menu.meals) {
				if(meal.name.equalsIgnoreCase(mealTitle)) {
					thisMeal = meal;
				}
			}
			if(thisMeal != null) {
				for(Station station: thisMeal.stations) {
					String stationId = getStationIdFromName(station.name);
					int duplicateId = 0;
					String cellId = "menu-cell-" + menu.diningHallId + "-" + stationId + "-" + mealTitle + "-";
					while(menuTable.getElementById(cellId + duplicateId) != null) duplicateId++;
					cellId += duplicateId;
					Element cell = menuTable
							.getElementById("menu-row-" + menu.diningHallId + "-" + stationId + "-" + duplicateId)
							.appendElement("td");
					cell.attr("id",cellId);
					cell.addClass("menu-cell");
					cell.addClass(mealTitle);
					Element list = cell.appendElement("ul").addClass("menu-item-list");
					for(MenuItem item: station.menu) {
						item.createElement(list.appendElement("li"));
					}
				}
			}
			for(Element stationElement: menuTable.getElementsByClass("menu-row-" + menu.diningHallId)) {
				if(stationElement.getElementsByClass(mealTitle).size() == 0) {
					Element spacer = stationElement.appendElement("td");
					spacer.attr("id", stationElement.id() + "-" + mealTitle);
					spacer.addClass("menu-cell");
					spacer.addClass("menu-spacer");
				}
			}
		}
	}

	private static List<String> setupMealTitles(Document template, List<Menu> menus) {
		Set<String> unusedMealTitles = new HashSet<>(Arrays.asList("breakfast","lunch","brunch","dinner"));
		for(Menu menu: menus) {
			for(Meal meal: menu.meals) {
				unusedMealTitles.remove(meal.name.toLowerCase());
			}
		}
		for(String mealTitle: unusedMealTitles) {
			template.getElementById("menu-header-meals-" + mealTitle).remove();
		}
		List<String> mealTitles = new ArrayList<>(Arrays.asList("breakfast","lunch","brunch","dinner"));
		mealTitles.removeAll(unusedMealTitles);
		return mealTitles;
	}
	
	private static void addStationNames(Document template, Element menuTable, Menu menu) {
		Multiset<String> stationNames = LinkedHashMultiset.create();
		for(Meal meal: menu.meals) {
			Multiset<String> stationNamesForMeal = LinkedHashMultiset.create();
			for(Station station: meal.stations) {
				stationNamesForMeal.add(station.name);
			}
			for(Multiset.Entry<String> e: stationNamesForMeal.entrySet()) {
				stationNames.setCount(e.getElement(), Math.max(stationNames.count(e.getElement()), e.getCount()));
			}
		}
		boolean isFirstStation = true;
		boolean isOddRow = true;
		for(String station: stationNames) {
			String stationId = getStationIdFromName(station);
			int duplicateId = 0;
			String rowId = "menu-row-" + menu.diningHallId + "-" + stationId + "-";
			while(menuTable.getElementById(rowId + duplicateId) != null) duplicateId++;
			rowId += duplicateId;
			Element stationRow = menuTable.appendElement("tr");
			stationRow.attr("id","menu-row-"+menu.diningHallId+"-"+stationId+"-"+duplicateId);
			stationRow.addClass("menu-row-"+menu.diningHallId);
			stationRow.addClass(menu.diningHallId);
			stationRow.addClass("colored");
			if(isOddRow) {
				stationRow.addClass("menu-row-odd");
			}
			isOddRow = !isOddRow;
			if(isFirstStation) {
				addDiningHallName(template, stationRow, stationNames.size(), menu);
				isFirstStation = false;
			}
			Element stationName = stationRow.appendElement("td");
			stationName.attr("id", 
					"menu-title-" + menu.diningHallId + "-station-" + stationId + "-" + duplicateId);
			stationName.addClass("menu-cell");
			stationName.text(station);
			
		}
	}
	
	private static final DateTimeFormatter hmFormat = DateTimeFormatter.ofPattern("h:mm");
	private static void addDiningHallName(Document template, Element stationRow, int height, Menu menu) {
		Element summaryLinkToFull = template.getElementById("menu-summary-"+menu.diningHallId+"-link-to-full");
		summaryLinkToFull.attr("href", "#"+stationRow.id());
		Element diningHallName = stationRow.appendElement("td");
		diningHallName.attr("id", "menu-title-" + menu.diningHallId);
		diningHallName.addClass("menu-cell");
		diningHallName.addClass("menu-title");
		Element nameLink = diningHallName.appendElement("a");
		nameLink.attr("href", menu.publicUrl);
		nameLink.text(menu.diningHallName);
		Element times = diningHallName.appendElement("div");
		for(Meal meal: menu.meals) {
			if(meal.hours == null) continue;
			times.appendText(meal.name+":");
			times.appendElement("br");
			times.appendText(meal.hours.startTime.format(hmFormat) + "\u00a0-\u00a0" +
			                 meal.hours.endTime.format(hmFormat));
			times.appendElement("br");
		}
		diningHallName.attr("rowspan",Integer.toString(height));
	}

	private static String getStationIdFromName(String stationName) {
		return stationName.toLowerCase().replaceAll("\\s+", "-");
	}

	private static void setupDayList(Document template, LocalDate day) {
		for(int dayNumber = 1; dayNumber <= 7; dayNumber++) {
			DayOfWeek dayOfWeek = DayOfWeek.of(dayNumber);
			Element tableItem = template.getElementById("day-list-day-"+dayNumber);
			LocalDate tagDay = (LocalDate)dayOfWeek.adjustInto(day);
			if(tagDay.equals(day)) {
				tableItem.addClass("day-list-item-selected");
			}
			Element link = tableItem.appendElement("a");
			link.appendElement("u").text(dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.US));
			link.appendElement("br");
			link.appendText(tagDay.toString());
			link.attr("href", tagDay.toString() + ".html");
		}
		template.getElementById("day-list-back-link").attr("href",
				((LocalDate)DayOfWeek.MONDAY.adjustInto(day)).minusDays(1) + ".html");
		template.getElementById("day-list-fwd-link").attr("href",
				((LocalDate)DayOfWeek.SUNDAY.adjustInto(day)).plusDays(1) + ".html");
	}
	
	public static void createAndSaveBlankpage(String folder, LocalDate day, 
			boolean replaceBlank, boolean replaceAll) {
		File fp = new File(folder, day.toString() + ".html");
		if(fp.exists() && !replaceAll) {
			if(!replaceBlank) return;
			try {
				if(!Files.toString(fp, Charsets.UTF_8).contains(NOT_FOUND_PAGE_ID)) return;
			} catch (IOException e) {
				return;
			}
		}
		try(FileWriter w = new FileWriter(fp)) {
			w.write(WebpageCreator.createBlankpage(day).toString());
		} catch (IOException e) {
			throw new RuntimeException("Error saving webpage",e);
		}
	}
	
	public static void createAndSaveWebpage(String folder, LocalDate day, List<Menu> menus) {
		try(FileWriter w = new FileWriter(new File(folder, day.toString() + ".html"))) {
			w.write(WebpageCreator.createWebpage(day, menus).toString());
		} catch (IOException e) {
			throw new RuntimeException("Error saving webpage",e);
		}
	}

	public static void createAndSaveWebpage(String folder, LocalDate day) {
		createAndSaveWebpage(folder, day, 
				MenuFetcher.fetchAllMenus(MenuFetcher.getAllMenuFetchers(), day));
	}
	
	public static void createIndex(String folder, LocalDate day) throws IOException {
		Files.copy(new File(folder, day.toString() + ".html"), new File(folder, "index.html"));
	}
	
	public static void main(String[] args) {
		//createAndSaveBlankpage(".", LocalDate.now(), true, true);
		//if(true) return;
		switch(args.length) {
		case 0:
			createAndSaveWebpage(".", LocalDate.of(2017, 8, 18));
			return;
		case 1:
			List<MenuFetcher> menuFetchers = MenuFetcher.getAllMenuFetchers();
			for(LocalDate day = LocalDate.now(); day.isBefore(LocalDate.now().plusDays(7)); day = day.plusDays(1)) {
				try {
					createAndSaveWebpage(args[0], day, MenuFetcher.fetchAllMenus(menuFetchers, day));
				} catch(Throwable t) {
					System.err.println("Failed to create webpage for day "+day);
					t.printStackTrace();
				}
			}
			try {
				createIndex(args[0], LocalDate.now());
			} catch (IOException e) {
				System.err.println("Failed to create index");
				e.printStackTrace();
			}
			return;
		default:
			for(int i = 1; i < args.length; i++) {
				createAndSaveWebpage(args[0], LocalDate.parse(args[i]));
			}
			return;
		}
	}
}
