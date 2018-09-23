package io.yancey.menufetcher.fetchers;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;

import javax.script.*;

import com.google.gson.*;

import io.yancey.menufetcher.*;
import io.yancey.menufetcher.data.*;

public abstract class SodexoSmgMenuFetcher extends AbstractSodexoMenuFetcher {
  private final String sitename;
  private final String smgName;

  protected JsonObject smgCache = null;

  public SodexoSmgMenuFetcher(String name, String id, String sitename, String smgName) {
    super(name, id, sitename);
    this.sitename = sitename;
    this.smgName = smgName;
  }

  @Override
  public Menu getMeals(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
    if(smgCache == null) fetchSmg();

    if(smgCache != null) {
      return getMenuFromSmg(day);
    } else {
      throw new MenuNotAvailableException("no menu returned from smg for "+id);
    }
  }

  private Menu getMenuFromSmg(LocalDate day) throws MenuNotAvailableException, MalformedMenuException {
    JsonArray menuData = smgCache.getAsJsonArray("menu");
    JsonObject itemData = smgCache.getAsJsonObject("items");

    for(JsonElement week: menuData) {
      LocalDate startDate = LocalDate.parse(week.getAsJsonObject().get("startDate").getAsString());
      LocalDate endDate = LocalDate.parse(week.getAsJsonObject().get("endDate").getAsString());

      if(day.isBefore(startDate) || day.isAfter(endDate)) continue;

      JsonArray weekData = week.getAsJsonObject().getAsJsonArray("menus").get(0).getAsJsonObject()
          .getAsJsonArray("tabs");

      JsonObject dayData = null;

      for(JsonElement tab: weekData) {
        String tabDayName = tab.getAsJsonObject().get("title").getAsString();
        DayOfWeek tabDay = DayOfWeek.valueOf(tabDayName.toUpperCase());
        if(tabDay.equals(day.getDayOfWeek())) {
          dayData = tab.getAsJsonObject();
          break;
        }
      }

      if(dayData == null) {
        throw new MenuNotAvailableException("No menu in smg for "+day);
      }

      JsonArray mealsData = dayData.getAsJsonArray("groups");

      List<Meal> meals = new ArrayList<>(3);
      for(JsonElement mealData: mealsData) {
        String mealName = mealData.getAsJsonObject().get("title").getAsString();
        if(mealName.equals("Lunch") &&
            (day.getDayOfWeek().equals(DayOfWeek.SATURDAY) ||
            day.getDayOfWeek().equals(DayOfWeek.SUNDAY))) {
          mealName = "Brunch";
        }
        JsonArray stationsData = mealData.getAsJsonObject().getAsJsonArray("category");
        List<Station> stations = new ArrayList<>();
        for(JsonElement stationData: stationsData) {
          String stationName = stationData.getAsJsonObject().get("title").getAsString();
          //if(stationName.equals("Deli")) stationName = "Exhibition";
          if(stationName.startsWith("Grill-")) stationName = "Grill";
          JsonArray itemIds = stationData.getAsJsonObject().getAsJsonArray("products");
          List<MenuItem> items = new ArrayList<>();
          for(JsonElement itemId: itemIds) {
            String itemName = itemData.getAsJsonArray(itemId.getAsString()).get(22).getAsString();
            String itemDescription = itemData.getAsJsonArray(itemId.getAsString()).get(23).getAsString();
            Set<String> itemTags = new HashSet<>(Arrays.asList(
                itemData.getAsJsonArray(itemId.getAsString()).get(30).getAsString().split("\\s+")));
            /*if(stationName.equals("Exhibition") && itemName.equals("Made to Order Deli Bar")) {
              stations.add(new Station("Deli",
                  Arrays.asList(new MenuItem(itemName, itemDescription, itemTags))));
              continue;
            }*/
            items.add(new MenuItem(itemName, itemDescription, itemTags));
          }
          addStation(stations, stationName, items);
        }
        meals.add(new Meal(stations, getMealTime(mealName, day), mealName, ""));
      }

      return new Menu(name, id, getPublicSmgUrl(), meals);
    }

    throw new MenuNotAvailableException("No menu in smg for "+day);
  }

  /*
   * Add a new station, or merge if there's already a station of the same name
   */
  private static void addStation(List<Station> stations, String stationName, List<MenuItem> items) {
    for(ListIterator<Station> iter = stations.listIterator(); iter.hasNext();) {
      if(iter.next().name.equals(stationName)) {
        items.addAll(iter.previous().menu);
        iter.remove();
        iter.add(new Station(stationName, items));
        return;
      }
    }

    stations.add(new Station(stationName, items));
  }

  private void fetchSmg() {
    String smgUrl = getSmgUrl();
    String smgContents;
      HttpURLConnection connection;
    try {
      connection = (HttpURLConnection) new URL(smgUrl).openConnection();
    } catch (IOException e) {
      System.err.println("Error fetching smg for "+id+":");
      e.printStackTrace();
      return;
    }
      connection.setInstanceFollowRedirects(true);
    try(Scanner sc = new Scanner(new BufferedInputStream(connection.getInputStream()), "UTF-8")) {
      smgContents = sc.useDelimiter("\\A").next();
    } catch (IOException e) {
      System.err.println("Error fetching smg for "+id+":");
      e.printStackTrace();
      return;
    }
    try {
      smgCache = parseSmgJavascript(smgContents).getAsJsonObject();
    } catch (ScriptException e) {
      System.err.println("Error evaluating javascript for "+id+"'s smg:");
      e.printStackTrace();
    }
  }

  private static JsonElement parseSmgJavascript(String smgContents) throws ScriptException {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine nashorn = factory.getEngineByName("nashorn");
        String smgJson = (String) nashorn.eval(
            smgContents + "; var retData = {menu: menuData, items: aData}; JSON.stringify(retData)");

        return new JsonParser().parse(smgJson);
  }

  private String getSmgUrl() {
    return "https://" + sitename + ".sodexomyway.com/smgmenu/json/" + smgName + "?forcedesktop=true";
  }

  private String getPublicSmgUrl() {
    return "https://" + sitename + ".sodexomyway.com/smgmenu/display/" + smgName + "?forcedesktop=true";
  }
}
