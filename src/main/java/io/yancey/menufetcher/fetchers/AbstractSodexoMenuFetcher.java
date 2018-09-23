package io.yancey.menufetcher.fetchers;

import java.io.*;
import java.time.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;

public abstract class AbstractSodexoMenuFetcher extends AbstractMenuFetcher {
  protected final String sitename;

  protected Map<String, Document> pageCache = new HashMap<>();

  public AbstractSodexoMenuFetcher(String name, String id, String sitename) {
    super(name, id);
    this.sitename = sitename;
  }

  protected abstract LocalTimeRange parseMealTime(Element accordianDiv, String mealName, LocalDate day);

  protected LocalTimeRange getMealTime(String mealName, LocalDate day) throws MenuNotAvailableException {
    return parseMealTime(fetchPortalPage().getElementById("accordion_3543")
        .getElementsByClass("accordionBody").get(1), mealName, day);
  }

  public Document fetchPortalPage() throws MenuNotAvailableException {
    if(!pageCache.containsKey(getPortalUrl())) {
      try {
        pageCache.put(getPortalUrl(), Jsoup.connect(getPortalUrl()).timeout(10*1000).get());
      } catch (IOException e) {
        throw new MenuNotAvailableException("Error fetching portal", e);
      }
    }
    return pageCache.get(getPortalUrl());
  }

  protected String getPortalUrl() {
    return "https://" + sitename + ".sodexomyway.com/dining-choices/index.html?forcedesktop=true";
  }

}
