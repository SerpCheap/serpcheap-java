package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/** The result of a {@link SerpCheap#search(SearchParams)} call. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {
  public String search;
  public int page;
  public KnowledgeGraph knowledgeGraph;
  public List<OrganicResult> organic = new ArrayList<>();
  public List<Ad> ads;
  public List<String> peopleAlsoAsk;
  public List<RelatedSearch> relatedSearches;
  public SearchStats stats;
}
