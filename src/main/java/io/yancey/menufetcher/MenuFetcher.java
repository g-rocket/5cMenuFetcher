package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public interface MenuFetcher {
	public List<Meal> getMeals(LocalDate day);
}
