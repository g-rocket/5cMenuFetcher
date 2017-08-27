package io.yancey.menufetcher.fetchers.dininghalls;

import java.time.*;
import java.util.regex.*;

import org.jsoup.nodes.*;
import org.jsoup.select.*;

import io.yancey.menufetcher.data.*;
import io.yancey.menufetcher.fetchers.*;

public class HochMenuFetcher extends SodexoSmgMenuFetcher {
	public static final String HOCH_SITENAME = "hmc";
	public static final int HOCH_TCM = 1300;
	public static final String HOCH_SMG = "harvey%20mudd%20college%20-%20resident%20dining";
	
	public HochMenuFetcher() {
		super("The Hoch", "hoch", HOCH_SITENAME, HOCH_SMG);
	}

	private Pattern mealTimePattern = Pattern.compile(
			"\\s+([A-Z][a-z]*): ([1-9][0-9]?):([0-9][0-9]) ([ap]).m. - ([1-9][0-9]?):([0-9][0-9]) ([ap]).m.\\s*");
	@Override
	protected LocalTimeRange parseMealTime(Element accordianDiv, String mealName, LocalDate day) {
		for(Element mealsTimeP: accordianDiv.getElementsByTag("p")) {
			Elements mealDateStrong = mealsTimeP.getElementsByTag("strong");
			if(mealDateStrong.size() == 0) continue;
			if(mealDateStrong.text().contains("Monday - Friday") && 
					(day.getDayOfWeek().equals(DayOfWeek.SATURDAY) || 
							day.getDayOfWeek().equals(DayOfWeek.SUNDAY))) {
				continue;
			}
			if(mealDateStrong.text().contains("Weekends") &&
					!(day.getDayOfWeek().equals(DayOfWeek.SATURDAY) || 
							day.getDayOfWeek().equals(DayOfWeek.SUNDAY))) {
				continue;
			}
			for(TextNode mealTime: mealsTimeP.textNodes()) {
				Matcher mealTimeMatcher = mealTimePattern.matcher(mealTime.text());
				if(!mealTimeMatcher.matches()) continue;
				if(!mealTimeMatcher.group(1).equals(mealName)) continue;
				
				LocalTime startTime = LocalTime.of(
						Integer.parseInt(mealTimeMatcher.group(2))%12 + 
						(mealTimeMatcher.group(4).equals("p")? 12: 0), 
								Integer.parseInt(mealTimeMatcher.group(3)));
				LocalTime endTime = LocalTime.of(
						Integer.parseInt(mealTimeMatcher.group(5))%12 + 
						(mealTimeMatcher.group(7).equals("p")? 12: 0), 
								Integer.parseInt(mealTimeMatcher.group(6)));
				return new LocalTimeRange(startTime, endTime);
			}
		}
		return null;
	}
}
