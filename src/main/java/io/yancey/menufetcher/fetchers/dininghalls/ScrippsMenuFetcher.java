package io.yancey.menufetcher.fetchers.dininghalls;

import io.yancey.menufetcher.fetchers.*;

public class ScrippsMenuFetcher extends SodexoMenuFetcher {
	public static final String SCRIPPS_SITENAME = "scrippsdining";
	public static final int SCRIPPS_TCM = 1567;
	
	public ScrippsMenuFetcher() {
		super("Malott", "scripps", SCRIPPS_SITENAME, SCRIPPS_TCM, null);
	}
}
