package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One matched result in a {@link RankResponse}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RankMatch {
  public int rank;
  public int page;

  @JsonProperty("position_on_page")
  public int positionOnPage;

  public String link;
  public String title;
}
