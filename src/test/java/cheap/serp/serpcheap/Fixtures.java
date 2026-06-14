package cheap.serp.serpcheap;

import java.time.Duration;

/** Shared golden payloads and client builders for the unit tests. */
final class Fixtures {

  private Fixtures() {}

  static final String GOLDEN = "{"
      + "\"search\":\"best running shoes\",\"page\":1,"
      + "\"knowledgeGraph\":{"
      + "\"title\":\"Running shoe\",\"description\":\"A running shoe is a type of footwear...\","
      + "\"descriptionSource\":\"Wikipedia\",\"attributes\":{\"Type\":\"Athletic shoe\"}},"
      + "\"organic\":["
      + "{\"position\":1,\"title\":\"The 15 Best Running Shoes of 2026\",\"link\":\"https://www.runnersworld.com/best\","
      + "\"snippet\":\"Our editors' top picks...\",\"date\":\"May 13, 2026\","
      + "\"sitelinks\":[{\"title\":\"Read more\",\"link\":\"https://www.runnersworld.com/best#r\"}]},"
      + "{\"position\":2,\"title\":\"6 Best Running Shoes in 2026\",\"link\":\"https://example.com/2\",\"snippet\":\"Tested by runners...\"},"
      + "{\"position\":3,\"title\":\"Running shoe reviews\",\"link\":\"https://example.com/3\",\"snippet\":\"100+ reviews...\"}],"
      + "\"ads\":[{\"position\":1,\"title\":\"Shop Running Shoes\",\"link\":\"https://ad.example.com\","
      + "\"displayedLink\":\"example.com/shop\",\"snippet\":\"Free shipping\",\"block\":\"top\"}],"
      + "\"peopleAlsoAsk\":[\"What is the #1 running shoe?\",\"Which brand is best?\"],"
      + "\"relatedSearches\":[{\"query\":\"best running shoes men\",\"link\":\"https://www.google.com/search?q=best+running+shoes+men\"}],"
      + "\"stats\":{\"balance\":994,\"cost\":6,\"cached\":false}}";

  static final String MINIMAL = "{\"organic\":[]}";

  static final String API_KEY = "sk-test-RECOGNIZABLE-12345";

  static SerpCheap client(String baseUrl) {
    return client(baseUrl, 2, Duration.ofSeconds(5));
  }

  static SerpCheap client(String baseUrl, int maxRetries, Duration timeout) {
    return new SerpCheap(API_KEY, ClientOptions.builder()
        .baseUrl(baseUrl)
        .maxRetries(maxRetries)
        .timeout(timeout)
        .build());
  }
}
