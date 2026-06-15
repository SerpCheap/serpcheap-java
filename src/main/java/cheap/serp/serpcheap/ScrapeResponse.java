package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The result of a {@link SerpCheap#scrape(ScrapeParams)} call. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrapeResponse {
  public String url;
  public Integer status;
  public String title;
  public String content;

  @JsonProperty("content_text")
  public String contentText;

  @JsonProperty("screenshot_url")
  public String screenshotUrl;

  public Stats stats;

  /** Billing metadata for a scrape. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Stats {
    public int balance;
    public int cost;
  }
}
