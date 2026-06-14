package cheap.serp.serpcheap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** In-process HTTP server for client unit tests. Records the last request and counts hits. */
final class MockApi implements AutoCloseable {

  /** A captured inbound request. */
  static final class Captured {
    final String method;
    final String path;
    final String body;
    final String contentType;
    final String apiKey;
    final String userAgent;

    Captured(String method, String path, String body, String contentType, String apiKey, String userAgent) {
      this.method = method;
      this.path = path;
      this.body = body;
      this.contentType = contentType;
      this.apiKey = apiKey;
      this.userAgent = userAgent;
    }
  }

  private final HttpServer server;
  private final AtomicInteger hits = new AtomicInteger();
  private final List<Captured> requests = new ArrayList<>();

  private MockApi(HttpServer server) {
    this.server = server;
  }

  /** Responds with a fixed status + body for every request. */
  static MockApi fixed(int status, String body) {
    return create((exchange, n) -> respond(exchange, status, body));
  }

  /** Sleeps {@code sleepMs} before responding 200 — used to force a client-side timeout. */
  static MockApi slow(long sleepMs, String body) {
    return create((exchange, n) -> {
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      respond(exchange, 200, body);
    });
  }

  /** Returns {@code failStatus}/{@code failBody} for the first {@code failCount} hits, then 200/{@code okBody}. */
  static MockApi failThenOk(int failCount, int failStatus, String failBody, String okBody) {
    return create((exchange, n) -> {
      if (n <= failCount) {
        respond(exchange, failStatus, failBody);
      } else {
        respond(exchange, 200, okBody);
      }
    });
  }

  interface CountingHandler {
    void handle(HttpExchange exchange, int hitNumber) throws IOException;
  }

  static MockApi create(CountingHandler handler) {
    try {
      HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      MockApi api = new MockApi(s);
      s.createContext("/", new Recorder(api, handler));
      s.setExecutor(null);
      s.start();
      return api;
    } catch (IOException e) {
      throw new IllegalStateException("failed to start mock server", e);
    }
  }

  private static final class Recorder implements HttpHandler {
    private final MockApi api;
    private final CountingHandler handler;

    Recorder(MockApi api, CountingHandler handler) {
      this.api = api;
      this.handler = handler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      int n = api.hits.incrementAndGet();
      String body;
      try (InputStream in = exchange.getRequestBody()) {
        body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
      Captured c = new Captured(
          exchange.getRequestMethod(),
          exchange.getRequestURI().getPath(),
          body,
          exchange.getRequestHeaders().getFirst("Content-Type"),
          exchange.getRequestHeaders().getFirst("x-api-key"),
          exchange.getRequestHeaders().getFirst("User-Agent"));
      synchronized (api.requests) {
        api.requests.add(c);
      }
      handler.handle(exchange, n);
    }
  }

  static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
    if (bytes.length > 0) {
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(bytes);
      }
    } else {
      exchange.close();
    }
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  int hits() {
    return hits.get();
  }

  Captured lastRequest() {
    synchronized (requests) {
      if (requests.isEmpty()) {
        throw new IllegalStateException("no request captured");
      }
      return requests.get(requests.size() - 1);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
