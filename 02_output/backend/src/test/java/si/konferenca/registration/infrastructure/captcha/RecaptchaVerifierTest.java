package si.konferenca.registration.infrastructure.captcha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.application.CaptchaUnavailableException;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.infrastructure.config.AppSettings;

/** DoD-P05: the production reCAPTCHA path against a mocked verification endpoint. */
class RecaptchaVerifierTest {

  private HttpServer server;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> contentType = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String answer = "{\"success\": true}";

  @BeforeEach
  void startMockEndpoint() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/siteverify",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private RecaptchaVerifier verifier() {
    return RecaptchaVerifier.create(
        "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify", "s3cret&key");
  }

  @DisplayName("DoD-P05 accepted token")
  @Test
  void acceptedTokenIsVerifiedWithSecretAndResponse() {
    assertThat(verifier().verify("tok en+1")).isTrue();
    assertThat(requestBody.get()).isEqualTo("secret=s3cret%26key&response=tok+en%2B1");
    assertThat(contentType.get()).isEqualTo("application/x-www-form-urlencoded");
  }

  @DisplayName("DoD-P05 rejected token")
  @Test
  void rejectedTokenIsNotAccepted() {
    answer = "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}";

    assertThat(verifier().verify("bad")).isFalse();
  }

  @Test
  void answerWithoutSuccessIsNotAccepted() {
    answer = "{}";

    assertThat(verifier().verify("x")).isFalse();
  }

  @Test
  void errorStatusMeansUnavailable() {
    status = 500;

    assertThatThrownBy(() -> verifier().verify("x"))
        .isInstanceOf(CaptchaUnavailableException.class)
        .hasMessage("reCAPTCHA answered 500");
  }

  @Test
  void malformedAnswerMeansUnavailable() {
    answer = "not json";

    assertThatThrownBy(() -> verifier().verify("x"))
        .isInstanceOf(CaptchaUnavailableException.class)
        .hasMessage("reCAPTCHA verification failed");
  }

  @Test
  void unreachableEndpointMeansUnavailable() throws IOException {
    int closed;
    try (ServerSocket socket = new ServerSocket(0)) {
      closed = socket.getLocalPort();
    }

    assertThatThrownBy(
            () -> RecaptchaVerifier.create("http://127.0.0.1:" + closed + "/", "k").verify("x"))
        .isInstanceOf(CaptchaUnavailableException.class)
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void interruptedVerificationKeepsTheInterruptFlag() {
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> verifier().verify("x"))
          .isInstanceOf(CaptchaUnavailableException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeCaptchaVerifier testMode = new TestModeCaptchaVerifier();

    assertThat(testMode.verify("test-pass")).isTrue();
    assertThat(testMode.verify("TEST-PASS")).isFalse();
    assertThat(testMode.verify(null)).isFalse();
  }

  private static AppSettings settings(boolean testMode) {
    return new AppSettings(
        "local",
        "C",
        "m@x.y",
        "",
        "dir",
        new AppSettings.Organizer("o", "p", "", true),
        new AppSettings.Captcha(testMode, "site", "secret", "https://verify.example/api"),
        "",
        60,
        100);
  }

  @Test
  void configChoosesTheVerifierFromTheTestModeSetting() {
    CaptchaVerifier test = new CaptchaConfig().captchaVerifier(settings(true));
    CaptchaVerifier production = new CaptchaConfig().captchaVerifier(settings(false));

    assertThat(test).isInstanceOf(TestModeCaptchaVerifier.class);
    assertThat(production).isInstanceOf(RecaptchaVerifier.class);
  }
}
