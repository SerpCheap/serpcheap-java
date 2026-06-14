package cheap.serp.serpcheap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/** The knowledge panel shown alongside results. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeGraph {
  public String title;
  public String imageUrl;
  public String description;
  public String descriptionSource;
  public String descriptionLink;
  public Map<String, String> attributes;
}
