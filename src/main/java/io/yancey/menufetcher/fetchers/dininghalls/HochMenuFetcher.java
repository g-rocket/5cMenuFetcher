package io.yancey.menufetcher.fetchers.dininghalls;

import io.yancey.menufetcher.fetchers.*;

public class HochMenuFetcher extends AbstractNewSodexoMenuFetcher {
	public static final String HOCH_SITENAME = "hmc";
	public static final int HOCH_TCM = 1300;
	public static final String HOCH_SMG = "harvey%20mudd%20college%20-%20resident%20dining";
	public static final int HOCH_MENUID = 344;
	public static final int HOCH_LOCATIONID = 13147001;
	
	public HochMenuFetcher() {
		super("The Hoch", "hoch", HOCH_SITENAME, HOCH_MENUID, HOCH_LOCATIONID);
	}
}
