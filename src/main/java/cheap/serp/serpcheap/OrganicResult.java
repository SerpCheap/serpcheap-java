package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
}
