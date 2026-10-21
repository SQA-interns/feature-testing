package si.konferenca.registration.adapter.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoogleCaptchaVerifierTest {

  private HttpServer server;
  private final AtomicInteger status = new AtomicInteger(200);
  private final AtomicReference<String> answer = new AtomicReference<>("{\"success\": true}");
  private final AtomicReference<String> received = new AtomicReference<>();

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          received.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] body = answer.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status.get(), body.length == 0 ? -1 : body.length);
          if (body.length > 0) {
            exchange.getResponseBody().write(body);
          }
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private GoogleCaptchaVerifier verifier() {
    return new GoogleCaptchaVerifier(
        "http://127.0.0.1:" + server.getAddress().getPort() + "/verify", "the secret");
  }

  @Test
  void acceptsSuccessTrueAndPostsSecretAndToken() {
    assertThat(verifier().verify("tok en")).isTrue();
    assertThat(received.get()).isEqualTo("secret=the+secret&response=tok+en");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}",
        "{\"success\": \"true\"}",
        "{}",
        "not json",
        ""
      })
  void rejectsEveryOtherAnswer(String body) {
    answer.set(body);

    assertThat(verifier().verify("token")).isFalse();
  }

  @Test
  void rejectsAnErrorStatus() {
    status.set(500);

    assertThat(verifier().verify("token")).isFalse();
  }

  @Test
  void rejectsWhenTheEndpointCannotBeReached() throws IOException {
    int closed;
    try (ServerSocket socket = new ServerSocket(0)) {
      closed = socket.getLocalPort();
    }

    assertThat(new GoogleCaptchaVerifier("http://127.0.0.1:" + closed + "/v", "s").verify("t"))
        .isFalse();
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeCaptchaVerifier testMode = new TestModeCaptchaVerifier();

    assertThat(testMode.verify("test-pass")).isTrue();
    assertThat(testMode.verify("test-pass ")).isFalse();
    assertThat(testMode.verify(null)).isFalse();
  }
}
