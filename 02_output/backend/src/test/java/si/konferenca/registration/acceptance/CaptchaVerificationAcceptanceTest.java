package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Production reCAPTCHA path (test mode off) against a mocked verification endpoint: AC-001-07,
 * AC-002-06, DoD-P05, SR-01; the site key comes from configuration (AR-07).
 */
class CaptchaVerificationAcceptanceTest extends AcceptanceTestSupport {

  static final String SITE_KEY = "acceptance-site-key";
  static final String SECRET_KEY = "acceptance-secret-key";
  static final String GOOD_TOKEN = "token-accepted-by-google";

  /** Form fields of every request the mocked endpoint received. */
  static final List<Map<String, String>> VERIFY_REQUESTS = new CopyOnWriteArrayList<>();

  static final HttpServer VERIFIER;

  static {
    try {
      VERIFIER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    VERIFIER.createContext(
        "/siteverify",
        exchange -> {
          String form =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          Map<String, String> fields = new LinkedHashMap<>();
          for (String pair : form.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
              fields.put(
                  URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                  URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
          }
          VERIFY_REQUESTS.add(fields);
          boolean ok =
              "POST".equals(exchange.getRequestMethod())
                  && SECRET_KEY.equals(fields.get("secret"))
                  && GOOD_TOKEN.equals(fields.get("response"));
          byte[] answer =
              (ok
                      ? "{\"success\": true, \"hostname\": \"localhost\"}"
                      : "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}")
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, answer.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(answer);
          }
        });
    VERIFIER.start();
  }

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    registerSettings(
        registry,
        Map.of(
            "CAPTCHA_TEST_MODE",
            "false",
            "RECAPTCHA_SITE_KEY",
            SITE_KEY,
            "RECAPTCHA_SECRET_KEY",
            SECRET_KEY,
            "RECAPTCHA_VERIFY_URL",
            "http://127.0.0.1:" + VERIFIER.getAddress().getPort() + "/siteverify"));
  }

  @BeforeEach
  void clearVerifyRequests() {
    VERIFY_REQUESTS.clear();
  }

  @Test
  @DisplayName("AC-001-07 / DoD-P05 a token the verifier accepts lets the registration through")
  void ac_001_07_tokenAcceptedByVerifierIsAccepted() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("captchaToken", GOOD_TOKEN);

    String id = createdId(postRegistration(body));

    assertThat(registrationCount()).isEqualTo(1);
    assertThat(registrationRow(id).get("type")).isEqualTo("EXTERNAL");
    assertThat(VERIFY_REQUESTS)
        .anySatisfy(
            f -> {
              assertThat(f.get("secret")).isEqualTo(SECRET_KEY);
              assertThat(f.get("response")).isEqualTo(GOOD_TOKEN);
            });
  }

  @Test
  @DisplayName("AC-001-07 / DoD-P05 a token the verifier rejects is rejected, nothing stored")
  void ac_001_07_tokenRejectedByVerifierIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("captchaToken", "token-rejected-by-google");

    assertThat(errorFields(postRegistration(body))).contains("captchaToken");
    assertNothingStored();
    assertThat(VERIFY_REQUESTS)
        .anySatisfy(f -> assertThat(f.get("response")).isEqualTo("token-rejected-by-google"));
  }

  @Test
  @DisplayName("AC-001-07 with test mode off the test token is rejected")
  void ac_001_07_testTokenIsRejectedWhenTestModeIsOff() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("captchaToken", "test-pass");

    assertThat(errorFields(postRegistration(body))).contains("captchaToken");
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-002-06 / DoD-P05 a student token the verifier rejects is rejected")
  void ac_002_06_studentTokenRejectedByVerifierIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("captchaToken", "token-rejected-by-google");

    assertThat(errorFields(postRegistration(body))).contains("captchaToken");
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-002-06 / DoD-P05 a student token the verifier accepts is accepted")
  void ac_002_06_studentTokenAcceptedByVerifierIsAccepted() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("captchaToken", GOOD_TOKEN);

    String id = createdId(postRegistration(body));
    assertThat(registrationRow(id).get("type")).isEqualTo("STUDENT");
  }

  @Test
  @DisplayName("AC-003-01 the form configuration carries the configured site key (AR-07)")
  void ac_003_01_formConfigCarriesConfiguredSiteKey() throws Exception {
    var response = get("/api/form-config");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(jsonBody(response).path("captchaSiteKey").asString()).isEqualTo(SITE_KEY);
  }
}
