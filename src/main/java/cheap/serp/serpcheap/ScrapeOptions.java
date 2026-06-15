package cheap.serp.serpcheap;

/** Opt-in page-content scraping attached to a {@link SearchParams} search. */
public final class ScrapeOptions {
  public final Boolean renderJs;
  public final Boolean screenshot;
  public final Integer topN;
  public final String waitFor;
  public final Integer waitMs;

  private ScrapeOptions(Builder b) {
    this.renderJs = b.renderJs;
    this.screenshot = b.screenshot;
    this.topN = b.topN;
    this.waitFor = b.waitFor;
    this.waitMs = b.waitMs;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Boolean renderJs;
    private Boolean screenshot;
    private Integer topN;
    private String waitFor;
    private Integer waitMs;

    public Builder renderJs(boolean renderJs) {
      this.renderJs = renderJs;
      return this;
    }

    public Builder screenshot(boolean screenshot) {
      this.screenshot = screenshot;
      return this;
    }

    public Builder topN(int topN) {
      this.topN = topN;
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

    public ScrapeOptions build() {
      return new ScrapeOptions(this);
    }
  }
}
