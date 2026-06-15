package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** One organic (non-ad) search result. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganicResult {
  public int position;
  public String title;
  public String link;
  public String snippet;
  public String date;
  public List<Sitelink> sitelinks;
  public String content;

  @JsonProperty("screenshot_url")
  public String screenshotUrl;

  @JsonProperty("scrape_error")
  public String scrapeError;
}
