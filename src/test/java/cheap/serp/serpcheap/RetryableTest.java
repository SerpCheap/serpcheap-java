package cheap.serp.serpcheap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RetryableTest {

  private static final Set<String> RETRYABLE = Set.of(
      "rate_limited",
      "too_many_concurrent_requests",
      "service_temporarily_unavailable",
      "result_timeout",
      "client_timeout",
      "network_error");

  private static final Set<String> NON_RETRYABLE = Set.of(
      "invalid_request",
      "missing_api_key",
      "unknown_api_key",
      "inactive_api_key",
      "account_blocked",
      "insufficient_credits",
      "request_in_progress",
      "invalid_response",
      "internal");

  @Test
  void retryableCodesAreRetryable() {
    for (String code : RETRYABLE) {
      assertTrue(new SerpCheapException(code, "m").isRetryable(), code + " should be retryable");
    }
  }

  @Test
  void nonRetryableCodesAreNotRetryable() {
    for (String code : NON_RETRYABLE) {
      assertFalse(new SerpCheapException(code, "m").isRetryable(), code + " should not be retryable");
    }
  }

  @Test
  void unknownCodeIsNotRetryable() {
    assertFalse(new SerpCheapException("made_up_code", "m").isRetryable());
  }

  @Test
  void statusAndRetryAfterAccessors() {
    SerpCheapException e = new SerpCheapException("rate_limited", "m", 429, 500, "deets");
    assertTrue(e.getStatus() == 429);
    assertTrue(e.getRetryAfterMs() == 500);
    assertTrue("deets".equals(e.getDetails()));
  }

  @Test
  void defaultStatusIsNegativeOne() {
    SerpCheapException e = new SerpCheapException("internal", "m");
    assertTrue(e.getStatus() == -1);
  }
}
