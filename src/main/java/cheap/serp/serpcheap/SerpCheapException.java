package cheap.serp.serpcheap;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/**
 * Unchecked error mirroring the serp.cheap API error taxonomy.
 * {@link #isRetryable()} marks the transient codes the client retries automatically.
 */
public class SerpCheapException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final Set<String> RETRYABLE = Set.of(
      "rate_limited",
      "too_many_concurrent_requests",
      "service_temporarily_unavailable",
      "result_timeout",
      "client_timeout",
      "network_error");

  private final String code;
  private final int status;
  private final Integer retryAfterMs;
  private final transient Object details;

  public SerpCheapException(String code, String message) {
    this(code, message, -1, null, null);
  }

  public SerpCheapException(String code, String message, int status) {
    this(code, message, status, null, null);
  }

  public SerpCheapException(String code, String message, int status, Integer retryAfterMs, Object details) {
    super(message);
    this.code = code;
    this.status = status;
    this.retryAfterMs = retryAfterMs;
    this.details = details;
  }

  /** Stable machine-readable error code. */
  public String getCode() {
    return code;
  }

  /** HTTP status, or -1 when the error has no HTTP origin. */
  public int getStatus() {
    return status;
  }

  /** Suggested wait before retrying, in milliseconds, or {@code null} when not provided. */
  public Integer getRetryAfterMs() {
    return retryAfterMs;
  }

  /** Optional structured error details from the API. */
  public Object getDetails() {
    return details;
  }

  /** Whether this error is transient and safe for the client to retry. */
  public boolean isRetryable() {
    return RETRYABLE.contains(code);
  }

  /** Map a non-2xx /v1/search response (status + parsed body) to a typed error. */
  static SerpCheapException mapApiError(int status, JsonNode body) {
    JsonNode b = (body != null && body.isObject()) ? body : null;
    String apiCode = text(b, "error");
    String msg = text(b, "message");

    switch (apiCode) {
      case "invalid_request":
        return new SerpCheapException("invalid_request",
            nonEmpty(msg, "The request parameters were rejected."), status, null, detailsOf(b));
      case "missing_api_key":
        return new SerpCheapException("missing_api_key",
            nonEmpty(msg, "No API key was sent."), status);
      case "unknown_api_key":
        return new SerpCheapException("unknown_api_key",
            "The API key is not recognized.", status);
      case "inactive_api_key":
        return new SerpCheapException("inactive_api_key",
            "The API key is inactive.", status);
      case "account_blocked":
        return new SerpCheapException("account_blocked",
            nonEmpty(msg, "This account is blocked."), status);
      case "insufficient_credits": {
        Integer required = intOrNull(b, "required");
        Integer balance = intOrNull(b, "balance");
        String detail = (required != null && balance != null)
            ? " (needs " + required + ", balance " + balance + ")" : "";
        return new SerpCheapException("insufficient_credits",
            "Not enough credits" + detail + ".", status);
      }
      case "rate_limited": {
        Integer retryAfterMs = intOrNull(b, "retry_after_ms");
        String suffix = retryAfterMs != null ? "; retry in " + retryAfterMs + " ms" : "";
        return new SerpCheapException("rate_limited",
            "Rate limit exceeded" + suffix + ".", status, retryAfterMs, null);
      }
      case "request_in_progress":
        return new SerpCheapException("request_in_progress",
            "An identical request is in flight.", status);
      case "too_many_concurrent_requests":
        return new SerpCheapException("too_many_concurrent_requests",
            nonEmpty(msg, "Too many concurrent requests."), status);
      case "service_temporarily_unavailable":
        return new SerpCheapException("service_temporarily_unavailable",
            nonEmpty(msg, "Temporarily unavailable."), status);
      case "result_timeout":
        return new SerpCheapException("result_timeout",
            nonEmpty(msg, "The search timed out."), status);
      default:
        break;
    }

    if (status == 401) {
      return new SerpCheapException("unknown_api_key", nonEmpty(msg, "Authentication failed."), status);
    }
    if (status == 403) {
      return new SerpCheapException("account_blocked", nonEmpty(msg, "Access denied."), status);
    }
    if (status == 429) {
      return new SerpCheapException("rate_limited", "Rate limit exceeded.", status);
    }
    if (status >= 500) {
      return new SerpCheapException("service_temporarily_unavailable", "HTTP " + status + ".", status);
    }
    return new SerpCheapException("internal",
        nonEmpty(msg, "serp.cheap API returned HTTP " + status + "."), status);
  }

  private static String text(JsonNode b, String field) {
    if (b == null) {
      return "";
    }
    JsonNode n = b.get(field);
    return (n != null && n.isTextual()) ? n.asText() : "";
  }

  private static Integer intOrNull(JsonNode b, String field) {
    if (b == null) {
      return null;
    }
    JsonNode n = b.get(field);
    return (n != null && n.isNumber()) ? n.intValue() : null;
  }

  private static Object detailsOf(JsonNode b) {
    if (b == null) {
      return null;
    }
    JsonNode n = b.get("details");
    return n != null ? n : null;
  }

  private static String nonEmpty(String value, String fallback) {
    return (value != null && !value.isEmpty()) ? value : fallback;
  }
}
