package io.yancey.menufetcher;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;
import io.yancey.menufetcher.fetchers.dininghalls.*;
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
		OptionSpec<LocalDateRange> nomenuOpt = parser.acceptsAll(
				Arrays.asList("nomenu", "x"),
				"Generate menu-not-available pages")
				.withRequiredArg()
				.withValuesConvertedBy(new ValueConverter<LocalDateRange>(){
					@Override
					public LocalDateRange convert(String value) {
						String[] parts = value.split(":");
						if(parts.length != 2) {
							throw new IllegalArgumentException("Invalid dateRange");
						}
						return new LocalDateRange(
								LocalDate.parse(parts[0].trim()),
								LocalDate.parse(parts[1].trim()));
					}

					@Override
					public Class<? extends LocalDateRange> valueType() {
						return LocalDateRange.class;
					}

					@Override
					public String valuePattern() {
						return "yyyy-MM-dd:yyyy-MM-dd";
					}
					
				});
		OptionSpec<String> nomenuReplaceOpt = parser.acceptsAll(
				Arrays.asList("replace", "r"),
				"Replace certain pages with menu-not-available pages")
				.availableIf(nomenuOpt)
				.withRequiredArg()
				.defaultsTo("none")
				.withValuesConvertedBy(new ValueConverter<String>() {
					@Override
					public String convert(String value) {
						if(!value.equals("none") && !value.equals("blank") && !value.equals("all")) {
							throw new IllegalArgumentException();
						}
						return value;
					}

					@Override
					public Class<? extends String> valueType() {
						return String.class;
					}

					@Override
					public String valuePattern() {
						return "none, blank, all";
					}
				});
		OptionSpec<Void> interactiveOpt = parser.acceptsAll(
				Arrays.asList("interactive", "z"),
				"Interactively fetch only the requested data and format or display it (for debugging)")
				.availableUnless(webOpt, apiOpt, nomenuOpt);
		
		OptionSet args;
		try {
			args = parser.parse(stringArgs);
		} catch(OptionException e) {
			System.err.println("Error parsing arguments: " + e);
			System.err.println();
			parser.printHelpOn(System.err);
			return;
		}
		
		if(!(args.has(apiOpt) || args.has(webOpt) || args.has(nomenuOpt) || args.has(interactiveOpt))) {
			System.err.println("Please specify something to output (--web or --api or --nomenu or --interactive)");
			System.err.println();
			try {
				parser.printHelpOn(System.err);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
		if(args.has(baseDir)) Files.createDirectories(args.valueOf(basedirOpt).toPath());
		
		if(args.has(nomenuOpt)) {
			for(LocalDate day: args.valueOf(nomenuOpt)) {
				WebpageCreator.createAndSaveBlankpage(baseDir, day, 
						args.valueOf(nomenuReplaceOpt).equals("blank"), 
						args.valueOf(nomenuReplaceOpt).equals("all"));
			}
		}
		
		if(args.has(webOpt) || args.has(apiOpt)) {
			generateStuff(args, dates, baseDir, webOpt, apiOpt);
		}
		
		if(args.has(indexOpt)) {
			try {
				WebpageCreator.createIndex(baseDir, args.valueOf(indexOpt));
			} catch (IOException e) {
				System.err.println("error creating index:");
				e.printStackTrace();
			}
		}
		
		if(args.has(interactiveOpt)) {
			runInteractiveLoop();
		}
	}

	private static void runInteractiveLoop() {
		Scanner sc = new Scanner(System.in);
		while(true) {
			System.out.print("> ");
			String cmdLine = sc.nextLine();
			if(cmdLine.equals("exit")) return;
			String[] cmdSplit = cmdLine.split(" ");
			String cmd;
			MenuFetcher mf = null;
			LocalDate day = LocalDate.now();
			int index = 0;
			String directory = ".";
			if(getMF(cmdSplit[0]) != null) {
				cmd = "print";
			} else {
				cmd = cmdSplit[index++];
			}
			if(index < cmdSplit.length) {
				mf = getMF(cmdSplit[index++]);
			}
			if(mf == null) index--;
			if(index < cmdSplit.length) {
				try {
					day = LocalDate.parse(cmdSplit[index++]);
				} catch(DateTimeParseException e) {
					// it's not a date; try another type of argument?
					index--;
				}
			}
			if(index < cmdSplit.length) {
				directory = cmdSplit[index++];
			}
			Menu menu = null;
			if(mf != null) {
				try {
					menu = mf.getMeals(day);
				} catch (MenuNotAvailableException | MalformedMenuException e) {
					System.err.println("Error fetching menu for "+mf.getId()+" on "+day+":");
					e.printStackTrace(System.err);
					continue;
				}
			}
			doCommand(cmd, menu, day, directory);
		}
	}
	
	private static void doCommand(String cmd, Menu menu, LocalDate day, String directory) {
		switch(cmd.toLowerCase()) {
			case "print":
				System.out.println(menu);
				break;
			case "a":
			case "api":
				try {
					if(menu == null) {
						ApiCreator.createAPI(directory, day);
					} else {
						ApiCreator.createAPI(directory, day, Arrays.asList(menu));
					}
				} catch (IOException e) {
					System.err.println("Error generating API:");
					e.printStackTrace(System.err);
				}
				break;
			case "w":
			case "web":
				try {
					if(menu == null) {
						WebpageCreator.createAndSaveWebpage(directory, day);
					} else {
						WebpageCreator.createAndSaveWebpage(directory, day, Arrays.asList(menu));
					}
				} catch(RuntimeException e) {
					System.err.println("Error generating webpage:");
					e.printStackTrace(System.err);
				}
				break;
			case "i":
			case "index":
				try {
					WebpageCreator.createIndex(directory, day);
				} catch (IOException e) {
					System.err.println("Error generating index:");
					e.printStackTrace(System.err);
				}
				break;
			default:
				System.err.println("Invalid command "+cmd);
		}
		System.err.flush();
	}

	private static MenuFetcher getMF(String name) {
		switch(name) {
			case "o":
			case "oldenborg": return new OldenborgMenuFetcher();
			case "fk":
			case "frank": return new FrankMenuFetcher();
			case "fy":
			case "frary": return new FraryMenuFetcher();
			case "h":
			case "hoch": return new HochMenuFetcher();
			case "p":
			case "mc":
			case "pitzer":
			case "mcconnel": return new PitzerMenuFetcher();
			case "c":
			case "cmc":
			case "collins": return new CollinsMenuFetcher();
			case "s":
			case "ml":
			case "scripps":
			case "mallot": return new ScrippsMenuFetcher();
			default: return null;
		}
	}

	private static void generateStuff(OptionSet args, List<LocalDate> dates, String baseDir,
			OptionSpec<Void> webOpt, OptionSpec<Void> apiOpt) {
		List<MenuFetcher> menuFetchers = MenuFetcher.getAllMenuFetchers();
		for(LocalDate day: dates) {
			List<Menu> menus = MenuFetcher.fetchAllMenus(menuFetchers, day);
			
			if(args.has(webOpt)) {
				WebpageCreator.createAndSaveWebpage(baseDir, day, menus);
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
