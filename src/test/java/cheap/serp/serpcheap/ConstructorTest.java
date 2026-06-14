package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConstructorTest {

  @Test
  void emptyApiKeyThrowsMissingApiKey() {
    SerpCheapException e = TestErrors.capture(() -> new SerpCheap(""));
    assertEquals("missing_api_key", e.getCode());
  }

  @Test
  void blankApiKeyThrowsMissingApiKey() {
    SerpCheapException e = TestErrors.capture(() -> new SerpCheap("   "));
    assertEquals("missing_api_key", e.getCode());
  }

  @Test
  void nullApiKeyThrowsMissingApiKey() {
    SerpCheapException e = TestErrors.capture(() -> new SerpCheap(null));
    assertEquals("missing_api_key", e.getCode());
  }

  @Test
  void clientOptionsDefaults() {
    ClientOptions opts = ClientOptions.defaults();
    assertEquals("https://api.serp.cheap", opts.baseUrl);
    assertEquals(Duration.ofSeconds(15), opts.timeout);
    assertEquals(2, opts.maxRetries);
    assertNull(opts.httpClient);
  }

  @Test
  void builderOverridesApplied() {
    HttpClient custom = HttpClient.newHttpClient();
    ClientOptions opts = ClientOptions.builder()
        .baseUrl("https://example.test")
        .timeout(Duration.ofSeconds(3))
        .maxRetries(5)
        .httpClient(custom)
        .build();
    assertEquals("https://example.test", opts.baseUrl);
    assertEquals(Duration.ofSeconds(3), opts.timeout);
    assertEquals(5, opts.maxRetries);
    assertSame(custom, opts.httpClient);
  }

  @Test
  void suppliedHttpClientIsUsed() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      HttpClient custom = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      SerpCheap c = new SerpCheap(Fixtures.API_KEY, ClientOptions.builder()
          .baseUrl(api.baseUrl())
          .httpClient(custom)
          .build());
      c.search(SearchParams.of("x"));
      assertEquals(1, api.hits());
    }
  }
}
