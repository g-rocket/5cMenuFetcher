package io.yancey.menufetcher;

import java.time.*;
import java.util.*;

public abstract class SodexoMenuFetcher implements MenuFetcher {
	protected abstract String getMenuUrl(LocalDate day);
	
	@Override
	public List<Meal> getMeals(LocalDate day) {
		return Collections.emptyList();
	}
}
