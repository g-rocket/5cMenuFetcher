package io.yancey.menufetcher.fetchers.dininghalls;

import io.yancey.menufetcher.fetchers.*;

public class ScrippsMenuFetcher extends AbstractNewSodexoMenuFetcher {
	public static final String SCRIPPS_SITENAME = "scrippsdining";
	public static final int SCRIPPS_TCM = 1567;
	public static final int SCRIPPS_MENUID = 288;
	public static final int SCRIPPS_LOCATIONID = 10638001;
	public static final String SCRIPPS_CAFENAME = "mallot";
	
	public ScrippsMenuFetcher() {
		super("Malott", "scripps", SCRIPPS_SITENAME, SCRIPPS_MENUID, SCRIPPS_LOCATIONID, SCRIPPS_CAFENAME);
	}
}
