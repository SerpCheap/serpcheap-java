package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TimeoutNetworkTest {

  @Test
  void slowResponseTriggersClientTimeout() throws Exception {
    try (MockApi api = MockApi.slow(2000, Fixtures.MINIMAL)) {
      SerpCheapException e = TestErrors.capture(() ->
          Fixtures.client(api.baseUrl(), 0, Duration.ofMillis(150)).search(SearchParams.of("x")));
      assertEquals("client_timeout", e.getCode());
    }
  }

  @Test
  void closedPortTriggersNetworkError() throws Exception {
    int port = freePort();
    String baseUrl = "http://127.0.0.1:" + port;
    SerpCheapException e = TestErrors.capture(() ->
        Fixtures.client(baseUrl, 0, Duration.ofSeconds(2)).search(SearchParams.of("x")));
    assertEquals("network_error", e.getCode());
  }

  @Test
  void networkErrorRedactsApiKey() throws Exception {
    int port = freePort();
    String baseUrl = "http://127.0.0.1:" + port;
    SerpCheapException e = TestErrors.capture(() ->
        Fixtures.client(baseUrl, 0, Duration.ofSeconds(2)).search(SearchParams.of("x")));
    assertEquals("network_error", e.getCode());
    assertFalse(e.getMessage().contains(Fixtures.API_KEY),
        "the api key must never leak into the network error message");
  }

  private static int freePort() throws Exception {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    }
  }
}
