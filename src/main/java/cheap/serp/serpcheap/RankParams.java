package cheap.serp.serpcheap;

/** Parameters for a {@link SerpCheap#rank(RankParams)} call. */
public final class RankParams {
  public final String url;
  public final String q;
  public final String gl;
  public final String hl;
  public final String tbs;
  public final Integer pages;
  public final String matchType;

  private RankParams(Builder b) {
    this.url = b.url;
    this.q = b.q;
    this.gl = b.gl;
    this.hl = b.hl;
    this.tbs = b.tbs;
    this.pages = b.pages;
    this.matchType = b.matchType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String url;
    private String q;
    private String gl;
    private String hl;
    private String tbs;
    private Integer pages;
    private String matchType;

    public Builder url(String url) {
      this.url = url;
      return this;
    }

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

    public Builder pages(int pages) {
      this.pages = pages;
      return this;
    }

    public Builder matchType(String matchType) {
      this.matchType = matchType;
      return this;
    }

    public RankParams build() {
      if (url == null || url.isBlank()) {
        throw new SerpCheapException("invalid_request", "A url is required.");
      }
      if (q == null || q.isBlank()) {
        throw new SerpCheapException("invalid_request", "A query (q) is required.");
      }
      return new RankParams(this);
    }
  }
}
