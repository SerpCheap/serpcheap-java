package cheap.serp.serpcheap;

/** Parameters for a {@link SerpCheap#scrape(ScrapeParams)} call. */
public final class ScrapeParams {
  public final String url;
  public final Boolean renderJs;
  public final Boolean screenshot;
  public final String waitFor;
  public final Integer waitMs;
  public final Integer screenshotWidth;
  public final Integer screenshotHeight;

  private ScrapeParams(Builder b) {
    this.url = b.url;
    this.renderJs = b.renderJs;
    this.screenshot = b.screenshot;
    this.waitFor = b.waitFor;
    this.waitMs = b.waitMs;
    this.screenshotWidth = b.screenshotWidth;
    this.screenshotHeight = b.screenshotHeight;
  }

  /** Build params for a URL with all options defaulted. */
  public static ScrapeParams of(String url) {
    return builder().url(url).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String url;
    private Boolean renderJs;
    private Boolean screenshot;
    private String waitFor;
    private Integer waitMs;
    private Integer screenshotWidth;
    private Integer screenshotHeight;

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder renderJs(boolean renderJs) {
      this.renderJs = renderJs;
      return this;
    }

    public Builder screenshot(boolean screenshot) {
      this.screenshot = screenshot;
      return this;
    }

    public Builder waitFor(String waitFor) {
      this.waitFor = waitFor;
      return this;
    }

    public Builder waitMs(int waitMs) {
      this.waitMs = waitMs;
      return this;
    }

    public Builder screenshotWidth(int screenshotWidth) {
      this.screenshotWidth = screenshotWidth;
      return this;
    }

    public Builder screenshotHeight(int screenshotHeight) {
      this.screenshotHeight = screenshotHeight;
      return this;
    }

    public ScrapeParams build() {
      if (url == null || url.isBlank()) {
        throw new SerpCheapException("invalid_request", "A url is required.");
      }
      return new ScrapeParams(this);
    }
  }
}
