package io.yancey.menufetcher;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.io.*;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;

import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.commons.lang.WordUtils;

public class WebpageCreator {
	private static final String NOT_FOUND_PAGE_ID = "29b834b1c0e9c7c946c6dc1d7a49c2218be33fefb150e17f2e6bf8a7ee42fec7";

	private static void addStationNames(Document template, Element menuTable, Menu menu) {
		Multiset<String> stationNames = LinkedHashMultiset.create();
		for (Meal meal : menu.meals) {
			Multiset<String> stationNamesForMeal = LinkedHashMultiset.create();
			for (Station station : meal.stations) {
				stationNamesForMeal.add(station.name);
			}
			for (Multiset.Entry<String> e : stationNamesForMeal.entrySet()) {
				stationNames.setCount(e.getElement(), Math.max(stationNames.count(e.getElement()), e.getCount()));
			}
		}
	}

	/*
	 * public static Document createBlankpage(LocalDate day) { Document template =
	 * loadTemplate(); setupDayList(template, day); deleteTables(template);
	 * addBlankPageFlair(template, day); return template;
	 * 
	 * return Document() }
	 */

	/**
	 * Create the entire webpage for a day, and returns the generated HTML as a
	 * string
	 * 
	 * @param day
	 *            The day which menus contains data for
	 * 
	 * @param menus
	 *            a list containing the meal information for each dining hall
	 * 
	 * @return the generated HTML as a string
	 */
	public static String createWebpage(LocalDate day, List<Menu> menus) {
		Velocity.init();
		VelocityContext context = new VelocityContext();
		context.put("menus", menus);
		context.put("test", "seems to be working");

		// generate an ordered set of meals which will represent the names (and order)
		// of each meal offered today
		List<Meal> allMeals = new ArrayList<>();
		for (Menu menu : menus) {
			for (Meal meal : menu.meals) {
				allMeals.add(meal);
			}
		}
		Collections.sort(allMeals);

		// now convert to strings

		Function<Meal, String> getMealTitle = new Function<Meal, String>() {
			@Override
			public String apply(Meal meal) {
				// normalizes string names to title case, so when we filter out duplicate meals,
				// we ignore case ("lunch", "Lunch", and "LUNch" all become "Lunch")
				return WordUtils.capitalizeFully(meal.name);
			}
		};

		List<String> allMealsStrings = Lists.transform(allMeals, getMealTitle);
		// and remove duplicates
		List<String> distinctMeals = allMealsStrings.stream().distinct().collect(Collectors.toList());

		context.put("allMeals", distinctMeals);

		// now that we have set up context, we can generate the menu

		Template template = null;

		try {
			template = Velocity.getTemplate("src/main/resources/template/index.vhtml");
		} catch (ResourceNotFoundException rnfe) {
			// couldn't find the template
		} catch (ParseErrorException pee) {
			// syntax error: problem parsing the template
		} catch (MethodInvocationException mie) {
			// something invoked in the template
			// threw an exception
		}

		StringWriter sw = new StringWriter();
		template.merge(context, sw);

		return sw.toString();
	}

	/*
	 * public static void createAndSaveBlankpage(String folder, LocalDate day,
	 * boolean replaceBlank, boolean replaceAll) { File fp = new File(folder,
	 * day.toString() + ".html"); if(fp.exists() && !replaceAll) { if(!replaceBlank)
	 * return; try { if(!Files.toString(fp,
	 * Charsets.UTF_8).contains(NOT_FOUND_PAGE_ID)) return; } catch (IOException e)
	 * { return; } } try(FileWriter w = new FileWriter(fp)) {
	 * w.write(WebpageCreator.createBlankpage(day).toString()); } catch (IOException
	 * e) { throw new RuntimeException("Error saving webpage",e); } }
	 */

	public static void createAndSaveWebpage(String folder, LocalDate day, List<Menu> menus) {
		try (FileWriter w = new FileWriter(new File(folder, day.toString() + ".html"))) {
			w.write(WebpageCreator.createWebpage(day, menus));
		} catch (IOException e) {
			throw new RuntimeException("Error saving webpage", e);
		}
	}

	public static void createAndSaveWebpage(String folder, LocalDate day) {
		createAndSaveWebpage(folder, day, MenuFetcher.fetchAllMenus(MenuFetcher.getAllMenuFetchers(), day));
	}

	public static void createIndex(String folder, LocalDate day) throws IOException {
		Files.copy(new File(folder, day.toString() + ".html"), new File(folder, "index.html"));
	}
}
