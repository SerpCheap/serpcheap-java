# serpcheap — Java SDK

Official Java client for the [serp.cheap](https://serp.cheap) Google SERP API.

## Install

Maven:

```xml
<dependency>
  <groupId>cheap.serp</groupId>
  <artifactId>serpcheap</artifactId>
  <version>0.1.0</version>
</dependency>
```

Requires Java 11+.

## Quickstart

```java
import cheap.serp.serpcheap.*;

SerpCheap client = new SerpCheap("YOUR_API_KEY");

SearchResponse res = client.search(SearchParams.of("best running shoes"));

for (OrganicResult r : res.organic) {
  System.out.println(r.position + ". " + r.title + " — " + r.link);
}
```

### Parameters

```java
SearchParams params = SearchParams.builder()
    .q("best running shoes")
    .gl("us")        // country, default "us"
    .hl("en")        // UI language (optional)
    .tbs("qdr:w")    // time filter: qdr:h | qdr:d | qdr:w (optional)
    .page(1)         // 1-indexed, default 1
    .build();

SearchResponse res = client.search(params);
```

### Page scraping

By default a search returns SERP metadata only. Attach `scrape` to also fetch the
page content of the top results (billed on top of the search):

```java
SearchResponse res = client.search(SearchParams.builder()
    .q("best running shoes")
    .scrape(ScrapeOptions.builder()
        .renderJs(true)     // render with a headless browser (optional)
        .screenshot(true)   // capture a full-page screenshot URL (optional)
        .topN(3)            // how many top results to scrape, default 5 (optional)
        .build())
    .build());

for (OrganicResult r : res.organic) {
  if (r.scrapeError != null) {
    System.out.println(r.link + " — scrape failed: " + r.scrapeError);
  } else {
    System.out.println(r.link + "\n" + r.content);
    System.out.println("screenshot: " + r.screenshotUrl);
  }
}
```

### Client options

```java
SerpCheap client = new SerpCheap("YOUR_API_KEY", ClientOptions.builder()
    .baseUrl("https://api.serp.cheap")
    .timeout(java.time.Duration.ofSeconds(15))
    .maxRetries(2)
    .build());
```

## Error handling

`SerpCheapException` is unchecked. Transient errors (rate limits, timeouts,
temporary unavailability) are retried automatically up to `maxRetries`.

```java
try {
  SearchResponse res = client.search(SearchParams.of("coffee"));
} catch (SerpCheapException e) {
  System.err.println(e.getCode() + ": " + e.getMessage());
  if (e.isRetryable()) {
    // transient — already retried, safe to try again later
  }
}
```

Error codes include `missing_api_key`, `unknown_api_key`, `account_blocked`,
`insufficient_credits`, `rate_limited`, `service_temporarily_unavailable`,
`result_timeout`, `client_timeout`, `network_error`.

## License

MIT
