package cheap.serp.serpcheap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Official serp.cheap API client: {@link #search(SearchParams)},
 * {@link #scrape(ScrapeParams)}, {@link #rank(RankParams)}.
 */
public final class SerpCheap {

  public static final String VERSION = "0.1.0"; // x-release-please-version

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String apiKey;
  private final String baseUrl;
  private final Duration timeout;
  private final int maxRetries;
  private final HttpClient http;

  public SerpCheap(String apiKey) {
    this(apiKey, ClientOptions.defaults());
  }

  public SerpCheap(String apiKey, ClientOptions opts) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new SerpCheapException("missing_api_key",
          "An API key is required. Get one at https://app.serp.cheap.");
    }
    this.apiKey = apiKey;
    this.baseUrl = opts.baseUrl.replaceAll("/+$", "");
    this.timeout = opts.timeout;
    this.maxRetries = opts.maxRetries;
    this.http = opts.httpClient != null
        ? opts.httpClient
        : HttpClient.newBuilder().connectTimeout(opts.timeout).build();
  }

  /** Run a Google search. Retries transient errors (429/503/timeout) with backoff. */
  public SearchResponse search(SearchParams params) {
    return send("/v1/search", payload -> buildSearchBody(payload, params),
        SearchResponse.class, body -> body.path("organic").isArray());
  }

  /** Fetch and extract a single arbitrary URL (content + optional screenshot). */
  public ScrapeResponse scrape(ScrapeParams params) {
    return send("/v1/scrape", payload -> buildScrapeBody(payload, params),
        ScrapeResponse.class, body -> body.path("url").isTextual());
  }

  /** Find where a url/domain ranks for a keyword across Google result pages. */
  public RankResponse rank(RankParams params) {
    return send("/v1/rank", payload -> buildRankBody(payload, params),
        RankResponse.class, body -> body.path("organic").isArray());
  }

  private <T> T send(String path, Consumer<ObjectNode> body, Class<T> type, Predicate<JsonNode> valid) {
    int attempt = 0;
    for (;;) {
      try {
        return once(path, body, type, valid);
      } catch (SerpCheapException e) {
        if (!e.isRetryable() || attempt >= maxRetries) {
          throw e;
        }
        long wait = e.getRetryAfterMs() != null
            ? e.getRetryAfterMs()
            : Math.min(2000L, 200L * (1L << attempt));
        sleep(wait);
        attempt++;
      }
    }
  }

  private <T> T once(String path, Consumer<ObjectNode> body, Class<T> type, Predicate<JsonNode> valid) {
    HttpResponse<byte[]> res;
    try {
      res = http.send(buildRequest(path, body), HttpResponse.BodyHandlers.ofByteArray());
    } catch (HttpTimeoutException e) {
      throw new SerpCheapException("client_timeout",
          "No response within " + timeout.toMillis() + " ms.");
    } catch (IOException e) {
      throw new SerpCheapException("network_error", reachError(e));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SerpCheapException("network_error", reachError(e));
    }

    int status = res.statusCode();
    JsonNode json;
    try {
      byte[] raw = res.body();
      json = (raw == null || raw.length == 0) ? MAPPER.createObjectNode() : MAPPER.readTree(raw);
    } catch (IOException e) {
      if (status < 200 || status >= 300) {
        throw SerpCheapException.mapApiError(status, MAPPER.createObjectNode());
      }
      throw new SerpCheapException("invalid_response", "The API returned a non-JSON body.", status);
    }

    if (status < 200 || status >= 300) {
      throw SerpCheapException.mapApiError(status, json);
    }
    if (json == null || !json.isObject() || !valid.test(json)) {
      throw new SerpCheapException("invalid_response",
          "The API response did not match the expected shape.", status);
    }
    try {
      return MAPPER.treeToValue(json, type);
    } catch (IOException e) {
      throw new SerpCheapException("invalid_response",
          "The API response could not be parsed.", status);
    }
  }

  private void buildSearchBody(ObjectNode payload, SearchParams params) {
    payload.put("q", params.q);
    payload.put("gl", params.gl != null ? params.gl : "us");
    payload.put("page", params.page);
    if (params.hl != null) {
      payload.put("hl", params.hl);
    }
    if (params.tbs != null) {
      payload.put("tbs", params.tbs);
    }
    if (params.scrape != null) {
      ObjectNode scrape = payload.putObject("scrape");
      ScrapeOptions o = params.scrape;
      if (o.renderJs != null) {
        scrape.put("render_js", o.renderJs);
      }
      if (o.screenshot != null) {
        scrape.put("screenshot", o.screenshot);
      }
      if (o.topN != null) {
        scrape.put("top_n", o.topN);
      }
      if (o.waitFor != null) {
        scrape.put("wait_for", o.waitFor);
      }
      if (o.waitMs != null) {
        scrape.put("wait_ms", o.waitMs);
      }
      if (o.screenshotWidth != null) {
        scrape.put("screenshot_width", o.screenshotWidth);
      }
      if (o.screenshotHeight != null) {
        scrape.put("screenshot_height", o.screenshotHeight);
      }
    }
  }

  private void buildScrapeBody(ObjectNode payload, ScrapeParams params) {
    payload.put("url", params.url);
    if (params.renderJs != null) {
      payload.put("render_js", params.renderJs);
    }
    if (params.screenshot != null) {
      payload.put("screenshot", params.screenshot);
    }
    if (params.waitFor != null) {
      payload.put("wait_for", params.waitFor);
    }
    if (params.waitMs != null) {
      payload.put("wait_ms", params.waitMs);
    }
    if (params.screenshotWidth != null) {
      payload.put("screenshot_width", params.screenshotWidth);
    }
    if (params.screenshotHeight != null) {
      payload.put("screenshot_height", params.screenshotHeight);
    }
  }

  private void buildRankBody(ObjectNode payload, RankParams params) {
    payload.put("url", params.url);
    payload.put("q", params.q);
    if (params.gl != null) {
      payload.put("gl", params.gl);
    }
    if (params.hl != null) {
      payload.put("hl", params.hl);
    }
    if (params.tbs != null) {
      payload.put("tbs", params.tbs);
    }
    if (params.pages != null) {
      payload.put("pages", params.pages);
    }
    if (params.matchType != null) {
      payload.put("match_type", params.matchType);
    }
  }

  private HttpRequest buildRequest(String path, Consumer<ObjectNode> body) {
    ObjectNode payload = MAPPER.createObjectNode();
    body.accept(payload);

    byte[] json;
    try {
      json = MAPPER.writeValueAsBytes(payload);
    } catch (IOException e) {
      throw new SerpCheapException("internal", "Failed to serialize request body.");
    }

    return HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + path))
        .timeout(timeout)
        .header("Content-Type", "application/json")
        .header("x-api-key", apiKey)
        .header("User-Agent", "serpcheap-java/" + VERSION)
        .POST(HttpRequest.BodyPublishers.ofByteArray(json))
        .build();
  }

  private String reachError(Exception e) {
    String raw = e.getMessage() != null ? e.getMessage() : e.toString();
    return "Could not reach " + baseUrl + ": " + raw.replace(apiKey, "[redacted]");
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
