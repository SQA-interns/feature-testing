package si.konferenca.registration.acceptance.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The backend with captcha test mode off, verifying tokens against a local stand-in for Google's
 * siteverify endpoint (DoD-P05). The stub accepts only {@link #GOOD_TOKEN} sent with {@link
 * #SECRET}.
 */
public abstract class CaptchaVerifierStubAcceptanceTest extends AcceptanceTestBase {

  public static final String GOOD_TOKEN = "token-the-verifier-accepts";
  public static final String SECRET = "stub-secret-key";

  protected static final Path JSON_COPY_DIR = Settings.newDirectory("acceptance-captcha");

  /** Form bodies the stub received, decoded as name to value. */
  protected static final List<Map<String, String>> VERIFY_CALLS = new CopyOnWriteArrayList<>();

  private static final HttpServer STUB = startStub();

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    Map<String, String> values = Settings.defaults(JSON_COPY_DIR);
    values.put("CAPTCHA_TEST_MODE", "false");
    values.put("RECAPTCHA_SECRET_KEY", SECRET);
    values.put(
        "RECAPTCHA_VERIFY_URL",
        "http://127.0.0.1:" + STUB.getAddress().getPort() + "/recaptcha/api/siteverify");
    Settings.register(registry, values);
  }

  @Override
  protected Path jsonCopyDirectory() {
    return JSON_COPY_DIR;
  }

  private static HttpServer startStub() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/recaptcha/api/siteverify",
          exchange -> {
            String form =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> fields = decode(form);
            VERIFY_CALLS.add(fields);
            boolean success =
                "POST".equals(exchange.getRequestMethod())
                    && SECRET.equals(fields.get("secret"))
                    && GOOD_TOKEN.equals(fields.get("response"));
            byte[] body =
                (success
                        ? "{\"success\": true, \"hostname\": \"localhost\"}"
                        : "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
              out.write(body);
            }
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, String> decode(String form) {
    Map<String, String> fields = new java.util.LinkedHashMap<>();
    for (String pair : form.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        fields.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return fields;
  }
}
