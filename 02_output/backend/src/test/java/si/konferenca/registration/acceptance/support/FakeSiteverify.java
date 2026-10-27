package si.konferenca.registration.acceptance.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mocked Google siteverify endpoint (docs/02_contracts/recaptcha-siteverify.schema.json, DoD-P05).
 * Token {@code good-token} gets success=true, {@code server-error} gets HTTP 500, anything else
 * gets success=false.
 */
public final class FakeSiteverify {

  public static final String SECRET = "test-recaptcha-secret";
  public static final String SITE_KEY = "test-recaptcha-site-key";
  public static final String GOOD_TOKEN = "good-token";
  public static final String SERVER_ERROR_TOKEN = "server-error";

  private static final FakeSiteverify INSTANCE = new FakeSiteverify();

  private final HttpServer server;
  private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();

  private FakeSiteverify() {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    server.createContext(
        "/siteverify",
        exchange -> {
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          Map<String, String> form = parseForm(body);
          requests.add(form);
          String token = form.getOrDefault("response", "");
          boolean secretOk = SECRET.equals(form.get("secret"));
          int status = SERVER_ERROR_TOKEN.equals(token) ? 500 : 200;
          String json =
              status == 500
                  ? "{}"
                  : (secretOk && GOOD_TOKEN.equals(token)
                      ? "{\"success\":true,\"hostname\":\"localhost\"}"
                      : "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}");
          byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  public static FakeSiteverify instance() {
    return INSTANCE;
  }

  public String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify";
  }

  /** Form fields of every verify call received so far. */
  public List<Map<String, String>> requests() {
    return requests;
  }

  private static Map<String, String> parseForm(String body) {
    Map<String, String> form = new java.util.LinkedHashMap<>();
    for (String pair : body.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int eq = pair.indexOf('=');
      String key = eq < 0 ? pair : pair.substring(0, eq);
      String value = eq < 0 ? "" : pair.substring(eq + 1);
      form.put(
          URLDecoder.decode(key, StandardCharsets.UTF_8),
          URLDecoder.decode(value, StandardCharsets.UTF_8));
    }
    return form;
  }
}
