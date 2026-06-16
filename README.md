# serpcheap — Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/cheap.serp/serpcheap)](https://central.sonatype.com/artifact/cheap.serp/serpcheap)
[![javadoc](https://javadoc.io/badge2/cheap.serp/serpcheap/javadoc.svg)](https://javadoc.io/doc/cheap.serp/serpcheap)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Official Java client for the [serp.cheap](https://serp.cheap) **Google Search API** — real-time Google SERP data (organic results, ads, knowledge graph, page scraping, rank tracking).

The **cheapest Google Search API** we know of: $0.0003 per cached search, $0.0006 fresh, no monthly minimum (~10× cheaper than SerpApi).

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
        .screenshotWidth(1280)   // screenshot width in px, default 1920, max 1920 (optional)
        .screenshotHeight(720)   // screenshot height in px, default 1080, max 1920 (optional)
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

### Scrape a single URL

Fetch and extract the content (and optionally a screenshot) of any page:

```java
ScrapeResponse res = client.scrape(ScrapeParams.builder()
    .url("https://example.com/article")
    .renderJs(true)          // render with a headless browser (optional)
    .screenshot(true)        // capture a screenshot URL (optional)
    .waitFor(".loaded")      // CSS selector to wait for, render_js only (optional)
    .waitMs(500)             // extra settle time in ms, render_js only (optional)
    .screenshotWidth(1280)   // px, default 1920, max 1920 (optional)
    .screenshotHeight(720)   // px, default 1080, max 1920 (optional)
    .build());

System.out.println(res.status + " " + res.title);
System.out.println(res.content);          // markdown
System.out.println(res.contentText);      // plain text
System.out.println(res.screenshotUrl);    // 48h presigned URL
```

`ScrapeParams.of("https://example.com")` builds a request with all options defaulted.

### Rank tracking

Find where a domain or URL ranks for a keyword across Google result pages:

```java
RankResponse res = client.rank(RankParams.builder()
    .url("runnersworld.com")        // domain or full URL to locate
    .q("best running shoes")        // keyword to rank for
    .gl("us")                       // country (optional)
    .hl("en")                       // UI language (optional)
    .tbs("qdr:w")                   // time filter (optional)
    .pages(3)                       // result pages to scan, 1-10, default 1 (optional)
    .matchType("domain")            // "domain" or "exact", default "domain" (optional)
    .build());

if (res.found) {
  System.out.println("Ranks at #" + res.rank);
  for (RankMatch m : res.matches) {
    System.out.println("page " + m.page + " pos " + m.positionOnPage + ": " + m.link);
  }
}

for (OrganicResult r : res.organic) {
  System.out.println(r.position + ". " + r.link);
}
```

`res.partial` is `true` when one or more pages failed to fetch (see `res.pagesFailed`).

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
