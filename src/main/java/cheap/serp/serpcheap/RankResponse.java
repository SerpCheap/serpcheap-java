package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/** The result of a {@link SerpCheap#rank(RankParams)} call. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RankResponse {
  public String url;
  public String search;
  public String gl;

  @JsonProperty("match_type")
  public String matchType;

  @JsonProperty("pages_scanned")
  public int pagesScanned;

  public boolean found;
  public Integer rank;
  public List<RankMatch> matches = new ArrayList<>();
  public List<OrganicResult> organic = new ArrayList<>();
  public boolean partial;

  @JsonProperty("pages_failed")
  public List<Integer> pagesFailed;

  public Stats stats;

  /** Billing and cache metadata for a rank lookup. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Stats {
    public int balance;
    public int cost;

    @JsonProperty("pages_cached")
    public int pagesCached;

    @JsonProperty("pages_fresh")
    public int pagesFresh;
  }
}
