package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ScrapeRankTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String SCRAPE_OK = "{"
      + "\"url\":\"https://example.com/page\",\"status\":200,\"title\":\"Example\","
      + "\"content\":\"# Example\\nbody\",\"content_text\":\"Example body\","
      + "\"screenshot_url\":\"https://shot.example/p.png\","
      + "\"stats\":{\"balance\":900,\"cost\":4}}";

  private static final String RANK_OK = "{"
      + "\"url\":\"runnersworld.com\",\"search\":\"best running shoes\",\"gl\":\"us\","
      + "\"match_type\":\"domain\",\"pages_scanned\":2,\"found\":true,\"rank\":3,"
      + "\"matches\":[{\"rank\":3,\"page\":1,\"position_on_page\":3,"
      + "\"link\":\"https://www.runnersworld.com/best\",\"title\":\"Best Running Shoes\"}],"
      + "\"organic\":["
      + "{\"position\":1,\"title\":\"A\",\"link\":\"https://a.example\",\"snippet\":\"s\"},"
      + "{\"position\":2,\"title\":\"B\",\"link\":\"https://b.example\",\"snippet\":\"s\"},"
      + "{\"position\":3,\"title\":\"Best Running Shoes\",\"link\":\"https://www.runnersworld.com/best\",\"snippet\":\"s\"}],"
      + "\"partial\":false,\"pages_failed\":[],"
      + "\"stats\":{\"balance\":880,\"cost\":12,\"pages_cached\":1,\"pages_fresh\":1}}";

  private JsonNode bodyOf(MockApi api) throws IOException {
    return MAPPER.readTree(api.lastRequest().body);
  }

  @Test
  void scrapePostsToScrapePathWithHeaders() throws Exception {
    try (MockApi api = MockApi.fixed(200, SCRAPE_OK)) {
      Fixtures.client(api.baseUrl()).scrape(ScrapeParams.of("https://example.com/page"));
      MockApi.Captured req = api.lastRequest();
      assertEquals("POST", req.method);
      assertEquals("/v1/scrape", req.path);
      assertEquals("application/json", req.contentType);
      assertEquals(Fixtures.API_KEY, req.apiKey);
      assertEquals("serpcheap-java/" + SerpCheap.VERSION, req.userAgent);
    }
  }

  @Test
  void scrapeBodyHasUrlAndOmitsUnsetOptions() throws Exception {
    try (MockApi api = MockApi.fixed(200, SCRAPE_OK)) {
      Fixtures.client(api.baseUrl()).scrape(ScrapeParams.of("https://example.com/page"));
      JsonNode body = bodyOf(api);
      assertEquals("https://example.com/page", body.get("url").asText());
      assertFalse(body.has("render_js"));
      assertFalse(body.has("screenshot"));
      assertFalse(body.has("wait_for"));
      assertFalse(body.has("wait_ms"));
      assertFalse(body.has("screenshot_width"));
      assertFalse(body.has("screenshot_height"));
    }
  }

  @Test
  void scrapeSerializesAllOptionsSnakeCase() throws Exception {
    try (MockApi api = MockApi.fixed(200, SCRAPE_OK)) {
      Fixtures.client(api.baseUrl()).scrape(ScrapeParams.builder()
          .url("https://example.com/page")
          .renderJs(true)
          .screenshot(true)
          .waitFor(".loaded")
          .waitMs(500)
          .screenshotWidth(1280)
          .screenshotHeight(720)
          .build());
      JsonNode body = bodyOf(api);
      assertTrue(body.get("render_js").asBoolean());
      assertTrue(body.get("screenshot").asBoolean());
      assertEquals(".loaded", body.get("wait_for").asText());
      assertEquals(500, body.get("wait_ms").asInt());
      assertEquals(1280, body.get("screenshot_width").asInt());
      assertEquals(720, body.get("screenshot_height").asInt());
    }
  }

  @Test
  void scrapeDeserializesResponse() throws Exception {
    try (MockApi api = MockApi.fixed(200, SCRAPE_OK)) {
      ScrapeResponse res = Fixtures.client(api.baseUrl())
          .scrape(ScrapeParams.of("https://example.com/page"));
      assertEquals("https://example.com/page", res.url);
      assertEquals(200, res.status);
      assertEquals("Example", res.title);
      assertEquals("# Example\nbody", res.content);
      assertEquals("Example body", res.contentText);
      assertEquals("https://shot.example/p.png", res.screenshotUrl);
      assertNotNull(res.stats);
      assertEquals(900, res.stats.balance);
      assertEquals(4, res.stats.cost);
    }
  }

  @Test
  void scrapeRetriesTransientThenSucceeds() throws Exception {
    try (MockApi api = MockApi.failThenOk(1, 503, "{\"error\":{\"code\":\"x\"}}", SCRAPE_OK)) {
      ScrapeResponse res = Fixtures.client(api.baseUrl())
          .scrape(ScrapeParams.of("https://example.com/page"));
      assertEquals(200, res.status);
      assertEquals(2, api.hits());
    }
  }

  @Test
  void scrapeWithoutUrlInResponseIsInvalid() throws Exception {
    try (MockApi api = MockApi.fixed(200, "{}")) {
      SerpCheapException e = TestErrors.capture(() ->
          Fixtures.client(api.baseUrl()).scrape(ScrapeParams.of("https://example.com")));
      assertEquals("invalid_response", e.getCode());
    }
  }

  @Test
  void scrapeRequiresUrl() {
    SerpCheapException e = TestErrors.capture(() -> ScrapeParams.of("  "));
    assertEquals("invalid_request", e.getCode());
  }

  @Test
  void rankPostsToRankPathWithHeaders() throws Exception {
    try (MockApi api = MockApi.fixed(200, RANK_OK)) {
      Fixtures.client(api.baseUrl()).rank(RankParams.builder()
          .url("runnersworld.com").q("best running shoes").build());
      MockApi.Captured req = api.lastRequest();
      assertEquals("POST", req.method);
      assertEquals("/v1/rank", req.path);
      assertEquals(Fixtures.API_KEY, req.apiKey);
    }
  }

  @Test
  void rankBodyHasRequiredAndOmitsUnset() throws Exception {
    try (MockApi api = MockApi.fixed(200, RANK_OK)) {
      Fixtures.client(api.baseUrl()).rank(RankParams.builder()
          .url("runnersworld.com").q("best running shoes").build());
      JsonNode body = bodyOf(api);
      assertEquals("runnersworld.com", body.get("url").asText());
      assertEquals("best running shoes", body.get("q").asText());
      assertFalse(body.has("gl"));
      assertFalse(body.has("hl"));
      assertFalse(body.has("tbs"));
      assertFalse(body.has("pages"));
      assertFalse(body.has("match_type"));
    }
  }

  @Test
  void rankSerializesAllOptions() throws Exception {
    try (MockApi api = MockApi.fixed(200, RANK_OK)) {
      Fixtures.client(api.baseUrl()).rank(RankParams.builder()
          .url("runnersworld.com")
          .q("best running shoes")
          .gl("de")
          .hl("en")
          .tbs("qdr:w")
          .pages(5)
          .matchType("exact")
          .build());
      JsonNode body = bodyOf(api);
      assertEquals("de", body.get("gl").asText());
      assertEquals("en", body.get("hl").asText());
      assertEquals("qdr:w", body.get("tbs").asText());
      assertEquals(5, body.get("pages").asInt());
      assertEquals("exact", body.get("match_type").asText());
    }
  }

  @Test
  void rankDeserializesResponseIncludingOrganicAndMatches() throws Exception {
    try (MockApi api = MockApi.fixed(200, RANK_OK)) {
      RankResponse res = Fixtures.client(api.baseUrl()).rank(RankParams.builder()
          .url("runnersworld.com").q("best running shoes").build());
      assertEquals("runnersworld.com", res.url);
      assertEquals("best running shoes", res.search);
      assertEquals("us", res.gl);
      assertEquals("domain", res.matchType);
      assertEquals(2, res.pagesScanned);
      assertTrue(res.found);
      assertEquals(3, res.rank);
      assertFalse(res.partial);
      assertNotNull(res.pagesFailed);
      assertTrue(res.pagesFailed.isEmpty());

      assertEquals(1, res.matches.size());
      RankMatch m = res.matches.get(0);
      assertEquals(3, m.rank);
      assertEquals(1, m.page);
      assertEquals(3, m.positionOnPage);
      assertEquals("https://www.runnersworld.com/best", m.link);
      assertEquals("Best Running Shoes", m.title);

      assertEquals(3, res.organic.size());
      assertEquals(1, res.organic.get(0).position);
      assertEquals("Best Running Shoes", res.organic.get(2).title);

      assertNotNull(res.stats);
      assertEquals(880, res.stats.balance);
      assertEquals(12, res.stats.cost);
      assertEquals(1, res.stats.pagesCached);
      assertEquals(1, res.stats.pagesFresh);
    }
  }

  @Test
  void rankNotFoundHasNullRank() throws Exception {
    String body = "{\"url\":\"nope.example\",\"search\":\"q\",\"gl\":\"us\",\"match_type\":\"domain\","
        + "\"pages_scanned\":1,\"found\":false,\"rank\":null,\"matches\":[],\"organic\":[],"
        + "\"partial\":false,\"pages_failed\":[]}";
    try (MockApi api = MockApi.fixed(200, body)) {
      RankResponse res = Fixtures.client(api.baseUrl()).rank(RankParams.builder()
          .url("nope.example").q("q").build());
      assertFalse(res.found);
      assertNull(res.rank);
      assertTrue(res.matches.isEmpty());
    }
  }

  @Test
  void rankRequiresUrlAndQuery() {
    SerpCheapException missingUrl = TestErrors.capture(() ->
        RankParams.builder().q("x").build());
    assertEquals("invalid_request", missingUrl.getCode());
    SerpCheapException missingQ = TestErrors.capture(() ->
        RankParams.builder().url("x.com").build());
    assertEquals("invalid_request", missingQ.getCode());
  }
}
