package cheap.serp.serpcheap;

import java.net.http.HttpClient;
import java.time.Duration;

/** Optional configuration for a {@link SerpCheap} client. */
public final class ClientOptions {
  final String baseUrl;
  final Duration timeout;
  final int maxRetries;
  final HttpClient httpClient;

  private ClientOptions(Builder b) {
    this.baseUrl = b.baseUrl;
    this.timeout = b.timeout;
    this.maxRetries = b.maxRetries;
    this.httpClient = b.httpClient;
  }

  public static ClientOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String baseUrl = "https://api.serp.cheap";
    private Duration timeout = Duration.ofSeconds(15);
    private int maxRetries = 2;
    private HttpClient httpClient;

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /** Test seam: supply a preconfigured {@link HttpClient}. */
    public Builder httpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    public ClientOptions build() {
      return new ClientOptions(this);
    }
  }
}
