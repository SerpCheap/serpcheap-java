package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A sitelink under an organic result or ad. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sitelink {
  public String title;
  public String link;
}
