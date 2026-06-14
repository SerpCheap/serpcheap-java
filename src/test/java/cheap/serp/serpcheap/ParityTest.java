package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ParityTest {

  private static final int PORT = 8793;
  private static final String BASE_URL = "http://127.0.0.1:" + PORT;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static Process mock;
  private static JsonNode fixtures;
  private static SerpCheap client;

  @BeforeAll
  static void startMock() throws Exception {
    Path moduleDir = Paths.get(System.getProperty("user.dir"));
    Path serverPath = moduleDir.resolve(Paths.get("..", "..", "contract", "mockserver", "server.mjs"))
        .normalize();
    Path casesPath = moduleDir.resolve(Paths.get("..", "..", "contract", "fixtures", "cases.json"))
        .normalize();

    fixtures = MAPPER.readTree(Files.readAllBytes(casesPath));

    mock = new ProcessBuilder("node", serverPath.toString(), String.valueOf(PORT))
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .start();

    waitForHealthz();

    client = new SerpCheap("test-key", ClientOptions.builder()
        .baseUrl(BASE_URL)
        .timeout(Duration.ofSeconds(10))
        .maxRetries(2)
        .build());
  }

  @AfterAll
  static void stopMock() {
    if (mock != null) {
      mock.destroy();
    }
  }

  @TestFactory
  List<DynamicTest> parityCases() {
    List<DynamicTest> tests = new ArrayList<>();
    for (JsonNode c : fixtures.get("cases")) {
      String name = c.get("name").asText();
      tests.add(DynamicTest.dynamicTest(name, () -> runCase(c)));
    }
    return tests;
  }

  private void runCase(JsonNode c) {
    String q = c.get("q").asText();
    SearchParams params = SearchParams.builder().q(q).build();

    if (c.hasNonNull("expectError")) {
      SerpCheapException e = assertThrows(SerpCheapException.class, () -> client.search(params));
      assertEquals(c.get("expectError").asText(), e.getCode());
      if (c.hasNonNull("retryAfterMs")) {
        assertEquals(c.get("retryAfterMs").asInt(), e.getRetryAfterMs());
      }
      return;
    }

    SearchResponse res = client.search(params);
    JsonNode expect = c.get("expect");
    if (expect == null) {
      return;
    }
    if (expect.hasNonNull("organic")) {
      assertEquals(expect.get("organic").asInt(), res.organic.size());
    }
    if (expect.hasNonNull("kg")) {
      if (expect.get("kg").asBoolean()) {
        assertNotNull(res.knowledgeGraph);
      }
    }
    if (expect.hasNonNull("ads")) {
      assertNotNull(res.ads);
      assertEquals(expect.get("ads").asInt(), res.ads.size());
    }
    if (expect.hasNonNull("cached")) {
      assertNotNull(res.stats);
      assertEquals(expect.get("cached").asBoolean(), res.stats.cached);
    }
  }

  @Test
  void missingApiKeyThrows() {
    SerpCheapException e = assertThrows(SerpCheapException.class, () -> new SerpCheap(""));
    assertEquals("missing_api_key", e.getCode());
    assertTrue(new SerpCheap("k").search(SearchParams.of("ok")).organic.size() >= 0);
  }

  private static void waitForHealthz() throws Exception {
    HttpClient probe = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + "/healthz"))
        .timeout(Duration.ofSeconds(1))
        .GET()
        .build();
    for (int i = 0; i < 100; i++) {
      try {
        HttpResponse<String> res = probe.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200 && res.body().contains("\"ok\"")) {
          return;
        }
      } catch (IOException | InterruptedException ignored) {
        // not up yet
      }
      Thread.sleep(100);
    }
    throw new IllegalStateException("mock server did not become healthy on " + BASE_URL);
  }
}
