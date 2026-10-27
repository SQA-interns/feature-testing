package si.konferenca.registration.adapter.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.config.AppProperties;

class CaptchaVerifierTest {

  private HttpServer server;
  private final List<String> bodies = new CopyOnWriteArrayList<>();
  private volatile int status;
  private volatile String answer;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/siteverify",
        exchange -> {
          bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private SiteverifyCaptchaVerifier verifier() {
    return new SiteverifyCaptchaVerifier(
        "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify", "s3cret&x=1");
  }

  private boolean verify(int status, String answer) {
    this.status = status;
    this.answer = answer;
    return verifier().verify("tok en+1");
  }

  @Test
  void acceptsOnlyHttp200WithSuccessTrue() {
    assertThat(verify(200, "{\"success\":true,\"hostname\":\"x\"}")).isTrue();
    assertThat(bodies).containsExactly("secret=s3cret%26x%3D1&response=tok+en%2B1");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "200|{\"success\":false}",
        "200|{\"success\":\"true\"}",
        "200|{\"hostname\":\"x\"}",
        "200|not json",
        "500|{\"success\":true}",
        "302|{\"success\":true}",
      })
  void everythingElseIsARejection(int status, String answer) {
    assertThat(verify(status, answer)).isFalse();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void blankTokenIsRejectedWithoutACall(String token) {
    assertThat(verifier().verify(token)).isFalse();
    assertThat(bodies).isEmpty();
  }

  @Test
  void unreachableVerifierIsARejection() {
    int port = server.getAddress().getPort();
    server.stop(0);

    assertThat(
            new SiteverifyCaptchaVerifier("http://127.0.0.1:" + port + "/siteverify", "s")
                .verify("token"))
        .isFalse();
  }

  @Test
  void interruptedVerificationIsARejectionAndKeepsTheFlag() {
    status = 200;
    answer = "{\"success\":true}";
    Thread.currentThread().interrupt();
    try {
      assertThat(verifier().verify("token")).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void testModeAcceptsOnlyThePassToken() {
    TestModeCaptchaVerifier testMode = new TestModeCaptchaVerifier();

    assertThat(testMode.verify("test-pass")).isTrue();
    assertThat(testMode.verify("test-fail")).isFalse();
    assertThat(testMode.verify(null)).isFalse();
  }

  private static AppProperties recaptcha(boolean testMode) {
    return new AppProperties(
        "test",
        null,
        Path.of("c"),
        new AppProperties.Recaptcha(testMode, "site", "secret", "http://127.0.0.1:1/siteverify"),
        null,
        null,
        null,
        null,
        0);
  }

  @Test
  void configurationChoosesTheVerifierByTestMode() {
    CaptchaConfiguration configuration = new CaptchaConfiguration();

    assertThat(configuration.captchaVerifier(recaptcha(true)))
        .isInstanceOf(TestModeCaptchaVerifier.class);
    assertThat(configuration.captchaVerifier(recaptcha(false)))
        .isInstanceOf(SiteverifyCaptchaVerifier.class);
  }
}
