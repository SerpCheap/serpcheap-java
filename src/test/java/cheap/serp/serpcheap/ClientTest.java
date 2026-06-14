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

class ClientTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonNode bodyOf(MockApi api) throws IOException {
    return MAPPER.readTree(api.lastRequest().body);
  }

  @Test
  void postsToSearchPathWithHeaders() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.of("hello"));
      MockApi.Captured req = api.lastRequest();
      assertEquals("POST", req.method);
      assertEquals("/v1/search", req.path);
      assertEquals("application/json", req.contentType);
      assertEquals(Fixtures.API_KEY, req.apiKey);
      assertEquals("serpcheap-java/" + SerpCheap.VERSION, req.userAgent);
    }
  }

  @Test
  void bodyHasQuery() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.of("pizza near me"));
      assertEquals("pizza near me", bodyOf(api).get("q").asText());
    }
  }

  @Test
  void glDefaultsToUsWhenUnset() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.builder().q("x").build());
      assertEquals("us", bodyOf(api).get("gl").asText());
    }
  }

  @Test
  void glPassedThroughWhenSet() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.builder().q("x").gl("de").build());
      assertEquals("de", bodyOf(api).get("gl").asText());
    }
  }

  @Test
  void pageDefaultsToOne() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.of("x"));
      assertEquals(1, bodyOf(api).get("page").asInt());
    }
  }

  @Test
  void pagePassedThroughWhenSet() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.builder().q("x").page(4).build());
      assertEquals(4, bodyOf(api).get("page").asInt());
    }
  }

  @Test
  void hlAndTbsOmittedWhenNull() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(SearchParams.of("x"));
      JsonNode body = bodyOf(api);
      assertFalse(body.has("hl"));
      assertFalse(body.has("tbs"));
    }
  }

  @Test
  void hlAndTbsIncludedWhenSet() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      Fixtures.client(api.baseUrl()).search(
          SearchParams.builder().q("x").hl("en").tbs("qdr:d").build());
      JsonNode body = bodyOf(api);
      assertEquals("en", body.get("hl").asText());
      assertEquals("qdr:d", body.get("tbs").asText());
    }
  }

  @Test
  void trailingSlashBaseUrlNormalized() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      SerpCheap c = new SerpCheap(Fixtures.API_KEY, ClientOptions.builder()
          .baseUrl(api.baseUrl() + "///")
          .build());
      c.search(SearchParams.of("x"));
      assertEquals("/v1/search", api.lastRequest().path);
    }
  }

  @Test
  void customBaseUrlHonored() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      assertNotNull(Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals(1, api.hits());
    }
  }

  @Test
  void parsesGoldenResponse() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.GOLDEN)) {
      SearchResponse res = Fixtures.client(api.baseUrl()).search(SearchParams.of("best running shoes"));

      assertEquals("best running shoes", res.search);
      assertEquals(1, res.page);

      assertEquals(3, res.organic.size());
      assertEquals(1, res.organic.get(0).position);
      assertEquals(2, res.organic.get(1).position);
      assertEquals(3, res.organic.get(2).position);
      assertEquals("The 15 Best Running Shoes of 2026", res.organic.get(0).title);
      assertEquals("May 13, 2026", res.organic.get(0).date);
      assertEquals(1, res.organic.get(0).sitelinks.size());
      assertEquals("Read more", res.organic.get(0).sitelinks.get(0).title);

      assertNotNull(res.knowledgeGraph);
      assertEquals("Running shoe", res.knowledgeGraph.title);
      assertEquals("Wikipedia", res.knowledgeGraph.descriptionSource);
      assertEquals("Athletic shoe", res.knowledgeGraph.attributes.get("Type"));

      assertNotNull(res.ads);
      assertEquals(1, res.ads.size());
      assertEquals("Shop Running Shoes", res.ads.get(0).title);
      assertEquals("example.com/shop", res.ads.get(0).displayedLink);
      assertEquals("top", res.ads.get(0).block);

      assertEquals(2, res.peopleAlsoAsk.size());
      assertEquals("What is the #1 running shoe?", res.peopleAlsoAsk.get(0));

      assertEquals(1, res.relatedSearches.size());
      assertEquals("best running shoes men", res.relatedSearches.get(0).query);

      assertNotNull(res.stats);
      assertEquals(994, res.stats.balance);
      assertEquals(6, res.stats.cost);
      assertFalse(res.stats.cached);
    }
  }

  @Test
  void parsesMinimalResponseWithoutNpe() throws Exception {
    try (MockApi api = MockApi.fixed(200, Fixtures.MINIMAL)) {
      SearchResponse res = Fixtures.client(api.baseUrl()).search(SearchParams.of("x"));
      assertNotNull(res.organic);
      assertTrue(res.organic.isEmpty());
      assertNull(res.knowledgeGraph);
      assertNull(res.ads);
      assertNull(res.peopleAlsoAsk);
      assertNull(res.relatedSearches);
      assertNull(res.stats);
    }
  }

  @Test
  void unknownExtraFieldsIgnored() throws Exception {
    String body = "{\"organic\":[],\"surpriseField\":42,\"nested\":{\"a\":1},\"stats\":{\"balance\":1,\"cost\":3,\"cached\":true,\"extra\":\"x\"}}";
    try (MockApi api = MockApi.fixed(200, body)) {
      SearchResponse res = Fixtures.client(api.baseUrl()).search(SearchParams.of("x"));
      assertTrue(res.organic.isEmpty());
      assertTrue(res.stats.cached);
    }
  }

  @Test
  void emptyJsonObjectWithoutOrganicIsInvalidResponse() throws Exception {
    try (MockApi api = MockApi.fixed(200, "{}")) {
      SerpCheapException e = TestErrors.capture(() -> Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals("invalid_response", e.getCode());
    }
  }

  @Test
  void nonJsonBodyOn200IsInvalidResponse() throws Exception {
    try (MockApi api = MockApi.fixed(200, "<html>not json</html>")) {
      SerpCheapException e = TestErrors.capture(() -> Fixtures.client(api.baseUrl()).search(SearchParams.of("x")));
      assertEquals("invalid_response", e.getCode());
      assertEquals(200, e.getStatus());
    }
  }
}
