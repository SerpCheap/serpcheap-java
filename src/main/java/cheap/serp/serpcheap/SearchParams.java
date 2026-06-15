package cheap.serp.serpcheap;

/** Parameters for a {@link SerpCheap#search(SearchParams)} call. */
public final class SearchParams {
  public final String q;
  public final String gl;
  public final String hl;
  public final String tbs;
  public final int page;
  public final ScrapeOptions scrape;

  private SearchParams(Builder b) {
    this.q = b.q;
    this.gl = b.gl;
    this.hl = b.hl;
    this.tbs = b.tbs;
    this.page = b.page;
    this.scrape = b.scrape;
  }

  /** Build params for a query with defaults (gl="us", page=1). */
  public static SearchParams of(String query) {
    return builder().q(query).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String q;
    private String gl = "us";
    private String hl;
    private String tbs;
    private int page = 1;
    private ScrapeOptions scrape;

    public Builder q(String q) {
      this.q = q;
      return this;
    }

    public Builder gl(String gl) {
      this.gl = gl;
      return this;
    }

    public Builder hl(String hl) {
      this.hl = hl;
      return this;
    }

    public Builder tbs(String tbs) {
      this.tbs = tbs;
      return this;
    }

    public Builder page(int page) {
      this.page = page;
      return this;
    }

    public Builder scrape(ScrapeOptions scrape) {
      this.scrape = scrape;
      return this;
    }

    public SearchParams build() {
      if (q == null || q.isBlank()) {
        throw new SerpCheapException("invalid_request", "A query (q) is required.");
      }
      return new SearchParams(this);
    }
  }
}
