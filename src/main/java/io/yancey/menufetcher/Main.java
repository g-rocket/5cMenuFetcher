package io.yancey.menufetcher;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import joptsimple.*;

public class Main {
	public static void main(String[] stringArgs) throws IOException {
		OptionParser parser = new OptionParser();
		OptionSpec<File> basedirOpt = parser.acceptsAll(
				Arrays.asList("basedir", "f"), 
				"The base directory for the webpages and `api` folder")
				.withRequiredArg().ofType(File.class)
				.defaultsTo(new File("."));
		OptionSpec<LocalDate> dateListOpt = parser.acceptsAll(
				Arrays.asList("dates", "date", "d"),
				"A list of dates to generate")
				.withRequiredArg()
				.withValuesSeparatedBy(',')
				.withValuesConvertedBy(new LocalDateValueConverter());
		OptionSpec<LocalDate> startDateOpt = parser.acceptsAll(
				Arrays.asList("from", "startDate", "s"),
				"The starting date to generate")
				.availableUnless(dateListOpt)
				.withRequiredArg()
				.withValuesConvertedBy(new LocalDateValueConverter())
				.defaultsTo(LocalDate.now());
		OptionSpec<LocalDate> endDateOpt = parser.acceptsAll(
				Arrays.asList("to", "endDate", "e"),
				"The ending date to generate")
				.availableUnless(dateListOpt)
				.withRequiredArg()
				.withValuesConvertedBy(new LocalDateValueConverter());
		OptionSpec<Integer> numDaysOpt = parser.acceptsAll(
				Arrays.asList("numDays", "n"),
				"How many days to generate")
				.availableUnless(dateListOpt, endDateOpt)
				.withRequiredArg()
				.ofType(Integer.class)
				.defaultsTo(1);
		OptionSpec<Void> webOpt = parser.acceptsAll(
				Arrays.asList("web", "w"),
				"Generate the webpage");
		OptionSpec<LocalDate> indexOpt = parser.acceptsAll(
				Arrays.asList("index", "i"),
				"Generate the index, optionally at a given date")
				.availableIf(webOpt)
				.withOptionalArg()
				.withValuesConvertedBy(new LocalDateValueConverter())
				.defaultsTo(LocalDate.now());
		OptionSpec<Void> apiOpt = parser.acceptsAll(
				Arrays.asList("api", "a"),
				"Generate the api");
		OptionSpec<Void> helpOpt = parser.acceptsAll(
				Arrays.asList("help", "h", "?"),
				"Get help")
				.forHelp();
		
		OptionSet args;
		try {
			args = parser.parse(stringArgs);
		} catch(OptionException e) {
			System.err.println("Error parsing arguments: " + e);
			System.err.println();
			parser.printHelpOn(System.err);
			return;
		}
		
		if(args.has(helpOpt)) {
			try {
				parser.printHelpOn(System.out);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return;
		}
		
		List<LocalDate> dates = getDates(args, dateListOpt, startDateOpt, endDateOpt, numDaysOpt);
		String baseDir = args.valueOf(basedirOpt).getAbsolutePath();
		if(args.has(basedirOpt)) Files.createDirectories(args.valueOf(basedirOpt).toPath());
		
		generateStuff(args, dates, baseDir, webOpt, indexOpt, apiOpt);
	}

	private static void generateStuff(OptionSet args, List<LocalDate> dates, String baseDir,
			OptionSpec<Void> webOpt, OptionSpec<LocalDate> indexOpt, OptionSpec<Void> apiOpt) {
		List<MenuFetcher> menuFetchers = MenuFetcher.getAllMenuFetchers();
		for(LocalDate day: dates) {
			List<Menu> menus = MenuFetcher.fetchAllMenus(menuFetchers, day);
			
			if(args.has(webOpt)) {
				WebpageCreator.createAndSaveWebpage(baseDir, day, menus);
				if(args.has(indexOpt)) {
					try {
						WebpageCreator.createIndex(baseDir, args.valueOf(indexOpt));
					} catch (IOException e) {
						System.err.println("error creating index:");
						e.printStackTrace();
					}
				}
			}
			
			if(args.has(apiOpt)) {
				try {
					ApiCreator.createAPI(baseDir, day, menus);
				} catch (IOException e) {
					System.err.println("error creating api:");
					e.printStackTrace();
				}
			}
		}
	}

	private static List<LocalDate> getDates(OptionSet args, 
			OptionSpec<LocalDate> dateListOpt, 
			OptionSpec<LocalDate> startDateOpt, OptionSpec<LocalDate> endDateOpt,
			OptionSpec<Integer> numDaysOpt) {
		if(args.has(dateListOpt)) {
			return args.valuesOf(dateListOpt);
		} else {
			List<LocalDate> dates = new ArrayList<>();
			LocalDate startDate;
			if(args.has(startDateOpt)) {
				startDate = args.valueOf(startDateOpt);
			} else {
				startDate = LocalDate.now();
			}
			if(args.has(endDateOpt)) {
				for(LocalDate day = args.valueOf(startDateOpt); 
						!day.isAfter(args.valueOf(endDateOpt)); 
						day = day.plusDays(1)) {
					dates.add(day);
				}
			} else {
				LocalDate day = args.valueOf(startDateOpt);
				for(int i = 0; i < args.valueOf(numDaysOpt); i++, day = day.plusDays(1)) {
					dates.add(day);
				}
			}
			return dates;
		}
	}
}
