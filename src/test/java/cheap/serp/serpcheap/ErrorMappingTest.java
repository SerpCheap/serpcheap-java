package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ErrorMappingTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ObjectNode obj() {
    return MAPPER.createObjectNode();
  }

  private SerpCheapException map(int status, ObjectNode body) {
    return SerpCheapException.mapApiError(status, body);
  }

  @Test
  void invalidRequest() {
    ObjectNode b = obj().put("error", "invalid_request").put("message", "q required");
    b.set("details", obj().put("q", "required"));
    SerpCheapException e = map(400, b);
    assertEquals("invalid_request", e.getCode());
    assertEquals("q required", e.getMessage());
    assertEquals(400, e.getStatus());
    assertTrue(e.getDetails() != null);
  }

  @Test
  void invalidRequestFallbackMessage() {
    SerpCheapException e = map(400, obj().put("error", "invalid_request"));
    assertEquals("invalid_request", e.getCode());
    assertEquals("The request parameters were rejected.", e.getMessage());
  }

  @Test
  void missingApiKey() {
    SerpCheapException e = map(400, obj().put("error", "missing_api_key"));
    assertEquals("missing_api_key", e.getCode());
    assertEquals("No API key was sent.", e.getMessage());
  }

  @Test
  void unknownApiKey() {
    SerpCheapException e = map(401, obj().put("error", "unknown_api_key"));
    assertEquals("unknown_api_key", e.getCode());
    assertEquals("The API key is not recognized.", e.getMessage());
  }

  @Test
  void inactiveApiKey() {
    SerpCheapException e = map(401, obj().put("error", "inactive_api_key"));
    assertEquals("inactive_api_key", e.getCode());
    assertEquals("The API key is inactive.", e.getMessage());
  }

  @Test
  void accountBlocked() {
    SerpCheapException e = map(403, obj().put("error", "account_blocked"));
    assertEquals("account_blocked", e.getCode());
    assertEquals("This account is blocked.", e.getMessage());
  }

  @Test
  void insufficientCreditsWithRequiredAndBalance() {
    ObjectNode b = obj().put("error", "insufficient_credits");
    b.put("required", 6).put("balance", 2);
    SerpCheapException e = map(402, b);
    assertEquals("insufficient_credits", e.getCode());
    assertEquals("Not enough credits (needs 6, balance 2).", e.getMessage());
  }

  @Test
  void insufficientCreditsWithoutDetail() {
    SerpCheapException e = map(402, obj().put("error", "insufficient_credits"));
    assertEquals("insufficient_credits", e.getCode());
    assertEquals("Not enough credits.", e.getMessage());
  }

  @Test
  void rateLimitedParsesRetryAfter() {
    ObjectNode b = obj().put("error", "rate_limited").put("retry_after_ms", 1200);
    SerpCheapException e = map(429, b);
    assertEquals("rate_limited", e.getCode());
    assertEquals(Integer.valueOf(1200), e.getRetryAfterMs());
    assertEquals("Rate limit exceeded; retry in 1200 ms.", e.getMessage());
  }

  @Test
  void rateLimitedWithoutRetryAfter() {
    SerpCheapException e = map(429, obj().put("error", "rate_limited"));
    assertEquals("rate_limited", e.getCode());
    assertNull(e.getRetryAfterMs());
    assertEquals("Rate limit exceeded.", e.getMessage());
  }

  @Test
  void requestInProgress() {
    SerpCheapException e = map(409, obj().put("error", "request_in_progress"));
    assertEquals("request_in_progress", e.getCode());
    assertEquals("An identical request is in flight.", e.getMessage());
  }

  @Test
  void tooManyConcurrentRequests() {
    SerpCheapException e = map(429, obj().put("error", "too_many_concurrent_requests"));
    assertEquals("too_many_concurrent_requests", e.getCode());
    assertEquals("Too many concurrent requests.", e.getMessage());
  }

  @Test
  void serviceTemporarilyUnavailable() {
    SerpCheapException e = map(503, obj().put("error", "service_temporarily_unavailable"));
    assertEquals("service_temporarily_unavailable", e.getCode());
    assertEquals("Temporarily unavailable.", e.getMessage());
  }

  @Test
  void resultTimeout() {
    SerpCheapException e = map(504, obj().put("error", "result_timeout"));
    assertEquals("result_timeout", e.getCode());
    assertEquals("The search timed out.", e.getMessage());
  }

  @Test
  void status401FallbackUnknownApiKey() {
    SerpCheapException e = map(401, obj());
    assertEquals("unknown_api_key", e.getCode());
    assertEquals("Authentication failed.", e.getMessage());
  }

  @Test
  void status403FallbackAccountBlocked() {
    SerpCheapException e = map(403, obj());
    assertEquals("account_blocked", e.getCode());
    assertEquals("Access denied.", e.getMessage());
  }

  @Test
  void status429FallbackRateLimited() {
    SerpCheapException e = map(429, obj());
    assertEquals("rate_limited", e.getCode());
    assertEquals("Rate limit exceeded.", e.getMessage());
  }

  @Test
  void status500FallbackServiceUnavailable() {
    SerpCheapException e = map(500, obj());
    assertEquals("service_temporarily_unavailable", e.getCode());
    assertEquals("HTTP 500.", e.getMessage());
  }

  @Test
  void status502FallbackServiceUnavailable() {
    assertEquals("service_temporarily_unavailable", map(502, obj()).getCode());
  }

  @Test
  void status503FallbackServiceUnavailable() {
    assertEquals("service_temporarily_unavailable", map(503, obj()).getCode());
  }

  @Test
  void status400FallbackInternal() {
    SerpCheapException e = map(400, obj());
    assertEquals("internal", e.getCode());
    assertEquals("serp.cheap API returned HTTP 400.", e.getMessage());
  }

  @Test
  void status418FallbackInternal() {
    assertEquals("internal", map(418, obj()).getCode());
  }

  @Test
  void status400FallbackInternalUsesApiMessage() {
    SerpCheapException e = map(400, obj().put("message", "weird"));
    assertEquals("internal", e.getCode());
    assertEquals("weird", e.getMessage());
  }

  @Test
  void emptyBodyMapsByStatus() {
    assertEquals("unknown_api_key", map(401, MAPPER.createObjectNode()).getCode());
    assertEquals("service_temporarily_unavailable", map(500, MAPPER.createObjectNode()).getCode());
  }

  @Test
  void nullBodyMapsByStatus() {
    SerpCheapException e = SerpCheapException.mapApiError(403, null);
    assertEquals("account_blocked", e.getCode());
  }

  @Test
  void unknownApiCodeFallsThroughToStatus() {
    SerpCheapException e = map(429, obj().put("error", "totally_made_up"));
    assertEquals("rate_limited", e.getCode());
  }
}
