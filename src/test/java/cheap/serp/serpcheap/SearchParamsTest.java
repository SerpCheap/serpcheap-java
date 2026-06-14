package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SearchParamsTest {

  @Test
  void ofAppliesDefaults() {
    SearchParams p = SearchParams.of("coffee");
    assertEquals("coffee", p.q);
    assertEquals("us", p.gl);
    assertEquals(1, p.page);
    assertNull(p.hl);
    assertNull(p.tbs);
  }

  @Test
  void builderDefaults() {
    SearchParams p = SearchParams.builder().q("tea").build();
    assertEquals("us", p.gl);
    assertEquals(1, p.page);
    assertNull(p.hl);
    assertNull(p.tbs);
  }

  @Test
  void builderOptionalFields() {
    SearchParams p = SearchParams.builder()
        .q("tea")
        .gl("gb")
        .hl("en")
        .tbs("qdr:w")
        .page(3)
        .build();
    assertEquals("tea", p.q);
    assertEquals("gb", p.gl);
    assertEquals("en", p.hl);
    assertEquals("qdr:w", p.tbs);
    assertEquals(3, p.page);
  }

  @Test
  void missingQueryThrowsInvalidRequest() {
    SerpCheapException e = TestErrors.capture(() -> SearchParams.builder().build());
    assertEquals("invalid_request", e.getCode());
  }

  @Test
  void blankQueryThrowsInvalidRequest() {
    SerpCheapException e = TestErrors.capture(() -> SearchParams.builder().q("  ").build());
    assertEquals("invalid_request", e.getCode());
  }

  @Test
  void ofWithNullQueryThrowsInvalidRequest() {
    SerpCheapException e = TestErrors.capture(() -> SearchParams.of(null));
    assertEquals("invalid_request", e.getCode());
  }
}
