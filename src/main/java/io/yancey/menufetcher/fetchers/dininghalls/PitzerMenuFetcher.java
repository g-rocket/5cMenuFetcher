package io.yancey.menufetcher.fetchers.dininghalls;

import io.yancey.menufetcher.fetchers.*;

public class PitzerMenuFetcher extends BonAppetitMenuFetcher {
	public static final int PITZER_ID = 219;
	public static final String PITZER_PUBLIC_MENU_URL_PREFIX = "pitzer";
	public static final String PITZER_PUBLIC_MENU_URL_CAFE = "mcconnell-bistro";

	public PitzerMenuFetcher() {
		super("McConnell", "pitzer", PITZER_ID, PITZER_PUBLIC_MENU_URL_PREFIX, PITZER_PUBLIC_MENU_URL_CAFE);
	}
}
