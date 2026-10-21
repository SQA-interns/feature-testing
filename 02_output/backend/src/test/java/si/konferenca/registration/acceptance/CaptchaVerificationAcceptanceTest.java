package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/**
 * SR-01, DoD-P05: test mode off, so the production reCAPTCHA path verifies every token against a
 * mocked verification endpoint that accepts only {@code good-token} with the configured secret.
 */
@AcceptanceTest
class CaptchaVerificationAcceptanceTest {

  private static final String SECRET = "acceptance-recaptcha-secret";
  private static final String SITE_KEY = "acceptance-recaptcha-site-key";
  private static final List<Map<String, String>> REQUESTS = new CopyOnWriteArrayList<>();
  private static final HttpServer VERIFIER = startVerifier();

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry,
        Map.of(
            "app.captcha.test-mode",
            "false",
            "app.captcha.site-key",
            SITE_KEY,
            "app.captcha.secret-key",
            SECRET,
            "app.captcha.verify-url",
            "http://127.0.0.1:" + VERIFIER.getAddress().getPort() + "/recaptcha/api/siteverify"));
  }

  @Test
  void ac_001_07_tokenAcceptedByTheVerifierRegistersAndSendsTheSecret() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("captchaToken", "good-token");

    mvc.perform(Submissions.post(body)).andExpect(status().isCreated());

    assertThat(db.countByEmail((String) body.get("email"))).isEqualTo(1);
    assertThat(REQUESTS)
        .anySatisfy(
            r -> {
              assertThat(r).containsEntry("secret", SECRET);
              assertThat(r).containsEntry("response", "good-token");
            });
  }

  @Test
  void ac_001_07_tokenRejectedByTheVerifierIsRejectedAndNothingStored() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("captchaToken", "bad-token");

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("captchaToken");
    assertThat(db.countByEmail((String) body.get("email"))).isZero();
    assertThat(REQUESTS).anySatisfy(r -> assertThat(r).containsEntry("response", "bad-token"));
  }

  @Test
  void ac_002_06_testModeTokenIsRejectedWhenTestModeIsOff() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("captchaToken", Submissions.CAPTCHA_PASS);

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("captchaToken");
    assertThat(db.countByEmail((String) body.get("email"))).isZero();
  }

  @Test
  void ac_002_06_studentTokenAcceptedByTheVerifierRegisters() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("captchaToken", "good-token");

    mvc.perform(Submissions.post(body)).andExpect(status().isCreated());

    assertThat(db.countByEmail((String) body.get("email"))).isEqualTo(1);
  }

  @Test
  void ac_001_07_formConfigCarriesTheConfiguredSiteKeyAndNoTestMode() throws Exception {
    var config = FormConfigAcceptanceTest.formConfig(mvc);

    assertThat(config.path("captchaSiteKey").asString("")).isEqualTo(SITE_KEY);
    assertThat(config.path("captchaTestMode").asBoolean(true)).isFalse();
    assertThat(config.toString()).doesNotContain(SECRET);
  }

  private static HttpServer startVerifier() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/recaptcha/api/siteverify",
          exchange -> {
            Map<String, String> form =
                parseForm(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            REQUESTS.add(form);
            boolean ok =
                SECRET.equals(form.get("secret")) && "good-token".equals(form.get("response"));
            byte[] answer =
                (ok
                        ? "{\"success\": true}"
                        : "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, String> parseForm(String body) {
    Map<String, String> form = new HashMap<>();
    for (String pair : body.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        form.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return form;
  }
}
