package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** A paid search result. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ad {
  public int position;
  public String title;
  public String link;
  public String displayedLink;
  public String snippet;
  public String block;
  public List<Sitelink> sitelinks;
}
