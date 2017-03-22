package io.yancey.menufetcher.fetchers.dininghalls;

import java.time.*;
import java.util.regex.*;

import org.jsoup.nodes.*;
import org.jsoup.select.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;

public class ScrippsMenuFetcher extends SodexoMenuFetcher {
	public static final String SCRIPPS_SITENAME = "scrippsdining";
	public static final int SCRIPPS_TCM = 1567;
	
	public ScrippsMenuFetcher() {
		super("Malott", "scripps", SCRIPPS_SITENAME, SCRIPPS_TCM, null);
	}

	private Pattern mealTimesPattern = Pattern.compile(
			"\\s+([A-Z][a-z]*) - ([A-Z][a-z]*): ([1-9][0-9]?):([0-9][0-9]) ([ap]).m. - ([1-9][0-9]?):([0-9][0-9]) ([ap]).m.");
	@Override
	protected LocalTimeRange parseMealTime(Element accordianDiv, String mealName, LocalDate day) {
		// deliberately invalid range, will be updated
		LocalDateRange currentValidDates = new LocalDateRange(LocalDate.of(0, 1, 1), LocalDate.of(0, 1, 1));
		for(Element mealTimesP: accordianDiv.getElementsByTag("p")) {
			if(mealTimesP.text().contains("Week of:")) {
				LocalDateRange newValidDates = parseDateRange(mealTimesP.text());
				if(newValidDates != null) {
					currentValidDates = newValidDates;
					continue;
				}
			}
			if(!currentValidDates.contains(day)) continue;
			Elements mealNameSpan = mealTimesP.getElementsByTag("span");
			if(mealNameSpan.size() > 0 && mealNameSpan.get(0).text().equals(mealName + ":")) {
				for(TextNode mealTimes: mealTimesP.textNodes()) {
					Matcher mealTimesMatcher = mealTimesPattern.matcher(mealTimes.text());
					if(mealTimesMatcher.matches()) {
						DayOfWeek startDay = DayOfWeek.valueOf(mealTimesMatcher.group(1).toUpperCase());
						DayOfWeek endDay = DayOfWeek.valueOf(mealTimesMatcher.group(2).toUpperCase());
						boolean isInRange = endDay.equals(day.getDayOfWeek());
						for(DayOfWeek dow = startDay; !isInRange && !dow.equals(endDay); dow = dow.plus(1)) {
							isInRange = dow.equals(day.getDayOfWeek());
						}
						if(!isInRange) continue;
						
						LocalTime startTime = LocalTime.of(
								Integer.parseInt(mealTimesMatcher.group(3))%12 + 
								(mealTimesMatcher.group(5).equals("p")? 12: 0), 
										Integer.parseInt(mealTimesMatcher.group(4)));
						LocalTime endTime = LocalTime.of(
								Integer.parseInt(mealTimesMatcher.group(6))%12 + 
								(mealTimesMatcher.group(8).equals("p")? 12: 0), 
										Integer.parseInt(mealTimesMatcher.group(7)));
						return new LocalTimeRange(startTime, endTime);
					}
				}
			}
		}
		return null;
	}
	
	public static void main(String[] args) throws MenuNotAvailableException, MalformedMenuException {
		System.out.println(new ScrippsMenuFetcher().getMeals(LocalDate.now()));
	}
}
