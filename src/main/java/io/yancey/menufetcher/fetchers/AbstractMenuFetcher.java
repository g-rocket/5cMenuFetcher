package io.yancey.menufetcher.fetchers;

public abstract class AbstractMenuFetcher implements MenuFetcher {
  protected final String name;
  protected final String id;

  public AbstractMenuFetcher(String name, String id) {
    this.name = name;
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }
}
