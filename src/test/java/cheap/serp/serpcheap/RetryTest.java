package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryTest {

  @Test
  void retriesTransientThenSucceeds() throws Exception {
    try (MockApi api = MockApi.failThenOk(1, 503,
        "{\"error\":\"service_temporarily_unavailable\"}", Fixtures.GOLDEN)) {
      SearchResponse res = Fixtures.client(api.baseUrl(), 2, Duration.ofSeconds(5))
          .search(SearchParams.of("x"));
      assertNotNull(res);
      assertEquals(2, api.hits());
    }
  }

  @Test
  void exhaustsMaxRetriesThenThrows() throws Exception {
    try (MockApi api = MockApi.fixed(503, "{\"error\":\"service_temporarily_unavailable\"}")) {
      SerpCheapException e = TestErrors.capture(() ->
          Fixtures.client(api.baseUrl(), 2, Duration.ofSeconds(5)).search(SearchParams.of("x")));
      assertEquals("service_temporarily_unavailable", e.getCode());
      assertEquals(3, api.hits());
    }
  }

  @Test
  void doesNotRetry400() throws Exception {
    try (MockApi api = MockApi.fixed(400, "{\"error\":\"invalid_request\"}")) {
      SerpCheapException e = TestErrors.capture(() ->
          Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals("invalid_request", e.getCode());
      assertEquals(1, api.hits());
    }
  }

  @Test
  void doesNotRetry401() throws Exception {
    try (MockApi api = MockApi.fixed(401, "{\"error\":\"unknown_api_key\"}")) {
      TestErrors.capture(() -> Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals(1, api.hits());
    }
  }

  @Test
  void doesNotRetry402() throws Exception {
    try (MockApi api = MockApi.fixed(402, "{\"error\":\"insufficient_credits\"}")) {
      TestErrors.capture(() -> Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals(1, api.hits());
    }
  }

  @Test
  void doesNotRetry403() throws Exception {
    try (MockApi api = MockApi.fixed(403, "{\"error\":\"account_blocked\"}")) {
      TestErrors.capture(() -> Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals(1, api.hits());
    }
  }

  @Test
  void retryAfterMsHonoredFast() throws Exception {
    try (MockApi api = MockApi.failThenOk(1, 429,
        "{\"error\":\"rate_limited\",\"retry_after_ms\":1}", Fixtures.GOLDEN)) {
      long start = System.nanoTime();
      SearchResponse res = Fixtures.client(api.baseUrl(), 2, Duration.ofSeconds(5))
          .search(SearchParams.of("x"));
      long elapsedMs = (System.nanoTime() - start) / 1_000_000;
      assertNotNull(res);
      assertEquals(2, api.hits());
      org.junit.jupiter.api.Assertions.assertTrue(elapsedMs < 2000, "retryAfterMs=1 should not back off seconds");
    }
  }

  @Test
  void maxRetriesZeroMeansSingleHit() throws Exception {
    try (MockApi api = MockApi.fixed(503, "{\"error\":\"service_temporarily_unavailable\"}")) {
      SerpCheapException e = TestErrors.capture(() ->
          Fixtures.client(api.baseUrl(), 0, Duration.ofSeconds(5)).search(SearchParams.of("x")));
      assertEquals("service_temporarily_unavailable", e.getCode());
      assertEquals(1, api.hits());
    }
  }
}
