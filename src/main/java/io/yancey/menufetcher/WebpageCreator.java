package io.yancey.menufetcher;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

public class WebpageCreator {
	
	public static Document createWebpage(LocalDate day) {
		Document template;
		try(InputStream templateFile = new Object().getClass().getResourceAsStream("/template.html")) {
			template = Jsoup.parse(templateFile, "UTF-8", "");
		} catch (IOException e) {
			throw new RuntimeException("Template not found",e);
		}
		setupDayList(template, day);
		addMenus(template, day);
		return template;
	}
	
	private static void addMenus(Document template, LocalDate day) {
		List<Menu> menus = new ArrayList<>();
		for(MenuFetcher menuFetcher: MenuFetcher.getAllMenuFetchers()) {
			try {
				menus.add(menuFetcher.getMeals(day));
				System.out.print(".");
			} catch(MenuNotAvailableException e) {
				System.err.println("Error fetching "+menuFetcher.getId()+
						" for "+day+": menu not found");
				e.printStackTrace();
			} catch(MalformedMenuException e) {
				System.err.println("Error fetching "+menuFetcher.getId()+
						" for "+day+": invalid data recieved");
				e.printStackTrace();
			} catch(Throwable t) {
				System.err.println("Invalid exception recieved fetching "+
						menuFetcher.getId()+" for "+day+": "+t);
				throw t;
			}
		}
		System.out.println();
		//System.out.println(menus);
		addMenuSummary(template, menus);
		addFullMenus(template, menus);
	}

	private static void addMenuSummary(Document template, List<Menu> menus) {
		for(Menu menu: menus) {
			Element nameCell = template.getElementById("menu-summary-title-" + menu.diningHallId);
			Element nameLink = nameCell.appendElement("a");
			nameLink.attr("href", menu.publicUrl);
			nameLink.text(menu.diningHallName);
		}
		boolean hasLunch = false;
		for(Menu menu: menus) {
			for(Meal meal: InterestingItemExtractor.instance.getInterestingItems(menu)) {
				if(meal.name.equalsIgnoreCase("lunch")) {
					hasLunch = true;
				}
				Element cell = template.getElementById("menu-summary-"+meal.name.toLowerCase()+"-"+menu.diningHallId);
				if(!meal.description.isEmpty()) {
					cell.appendText(meal.description);
				}
				Element list = cell.appendElement("ul").addClass("menu-item-list");
				for(Station station: meal.stations) {
					for(MenuItem item: station.menu) {
						list.appendElement("li").appendText(item.toString());
					}
				}
			}
		}
		if(!hasLunch) template.getElementById("menu-summary-lunch").remove();
	}

	private static void addFullMenus(Document template, List<Menu> menus) {
		// TODO Auto-generated method stub
		System.err.println("addFullMenus not implemented yet");
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
			link.text(dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.US));
			link.appendElement("br");
			link.appendText(tagDay.toString());
			link.attr("href", tagDay.toString() + ".html");
		}
		template.getElementById("day-list-back-link").attr("href",
				((LocalDate)DayOfWeek.MONDAY.adjustInto(day)).minusDays(1) + ".html");
		template.getElementById("day-list-fwd-link").attr("href",
				((LocalDate)DayOfWeek.SUNDAY.adjustInto(day)).plusDays(1) + ".html");
	}
	
	public static void createAndSaveWebpage(String folder, LocalDate day) {
		try(FileWriter w = new FileWriter(new File(folder, day.toString() + ".html"))) {
			w.write(WebpageCreator.createWebpage(day).toString());
		} catch (IOException e) {
			throw new RuntimeException("Error saving webpage",e);
		}
	}
	
	public static void main(String[] args) {
		switch(args.length) {
		case 0:
			createAndSaveWebpage(".", LocalDate.now());
			return;
		case 1:
			for(LocalDate day = LocalDate.now(); day.isBefore(LocalDate.now().plusDays(7)); day = day.plusDays(1)) {
				try {
					createAndSaveWebpage(args[0], day);
				} catch(Throwable t) {
					System.err.println("Failed to create webpage for day "+day);
					t.printStackTrace();
				}
			}
			try(FileWriter index = new FileWriter(new File(args[0], "index.html"))) {
				index.write("<html><head><meta http-equiv=\"refresh\" content=\"0; URL=" + LocalDate.now().toString() + ".html\"></head><body>Redirecting you to <a href=\"" + LocalDate.now().toString() + ".html\">the current menu</a></body></html>");
			} catch (IOException e) {
				e.printStackTrace();
			}
		case 2:
			createAndSaveWebpage(args[0], LocalDate.parse(args[1]));
		default:
			throw new IllegalArgumentException("Too many arguments; perhaps you forgot to quote the filename");
		}
	}
}
