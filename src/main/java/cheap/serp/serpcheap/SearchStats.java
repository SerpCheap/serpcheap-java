package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Billing and cache metadata for a search. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchStats {
  public int balance;
  public int cost;
  public boolean cached;
}
