package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A "related searches" suggestion. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelatedSearch {
  public String query;
  public String link;
}
