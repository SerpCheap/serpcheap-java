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

/** Official serp.cheap SERP API client. One method: {@link #search(SearchParams)}. */
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
    int attempt = 0;
    for (;;) {
      try {
        return once(params);
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

  private SearchResponse once(SearchParams params) {
    HttpResponse<byte[]> res;
    try {
      res = http.send(buildRequest(params), HttpResponse.BodyHandlers.ofByteArray());
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
    JsonNode body;
    try {
      byte[] raw = res.body();
      body = (raw == null || raw.length == 0) ? MAPPER.createObjectNode() : MAPPER.readTree(raw);
    } catch (IOException e) {
      if (status < 200 || status >= 300) {
        throw SerpCheapException.mapApiError(status, MAPPER.createObjectNode());
      }
      throw new SerpCheapException("invalid_response", "The API returned a non-JSON body.", status);
    }

    if (status < 200 || status >= 300) {
      throw SerpCheapException.mapApiError(status, body);
    }
    if (body == null || !body.isObject() || !body.path("organic").isArray()) {
      throw new SerpCheapException("invalid_response",
          "The API response did not match the expected shape.", status);
    }
    try {
      return MAPPER.treeToValue(body, SearchResponse.class);
    } catch (IOException e) {
      throw new SerpCheapException("invalid_response",
          "The API response could not be parsed.", status);
    }
  }

  private HttpRequest buildRequest(SearchParams params) {
    ObjectNode payload = MAPPER.createObjectNode();
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
    }

    byte[] json;
    try {
      json = MAPPER.writeValueAsBytes(payload);
    } catch (IOException e) {
      throw new SerpCheapException("internal", "Failed to serialize request body.");
    }

    return HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/v1/search"))
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
