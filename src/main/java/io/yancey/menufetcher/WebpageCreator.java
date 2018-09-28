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

	public static Document createWebpage(LocalDate day, List<Menu> menus) {
		Document template = loadTemplate();
		setupDayList(template, day);
		addMenus(template, menus);
		return template;
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
}
