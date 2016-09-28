package io.yancey.menufetcher.fetchers.dininghalls;

import io.yancey.menufetcher.fetchers.*;

public class CollinsMenuFetcher extends BonAppetitMenuFetcher {
	public static final int COLLINS_ID = 50;
	public static final String COLLINS_PUBLIC_MENU_URL_PREFIX = "collins-cmc";
	public static final String COLLINS_PUBLIC_MENU_URL_CAFE = "collins";

	public CollinsMenuFetcher() {
		super("Collins", "collins", COLLINS_ID, COLLINS_PUBLIC_MENU_URL_PREFIX, COLLINS_PUBLIC_MENU_URL_CAFE);
	}
}
