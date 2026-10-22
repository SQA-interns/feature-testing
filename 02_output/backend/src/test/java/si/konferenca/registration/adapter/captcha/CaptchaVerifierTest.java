package si.konferenca.registration.adapter.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CaptchaVerifierTest {

  private HttpServer server;
  private final AtomicReference<String> received = new AtomicReference<>();

  private RecaptchaVerifier verifierAnswering(int status, String body) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          received.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
    URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/verify");
    return new RecaptchaVerifier(HttpClient.newHttpClient(), url, "s3cret&key");
  }

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void successfulAnswerAcceptsAndSendsEncodedSecretAndToken() throws IOException {
    RecaptchaVerifier verifier = verifierAnswering(200, "{\"success\": true}");

    assertThat(verifier.verify("tok en+1")).isTrue();
    assertThat(received.get()).isEqualTo("secret=s3cret%26key&response=tok+en%2B1");
  }

  @Test
  void failedAnswerRejects() throws IOException {
    assertThat(verifierAnswering(200, "{\"success\": false}").verify("t")).isFalse();
  }

  @Test
  void successThatIsNotABooleanRejects() throws IOException {
    assertThat(verifierAnswering(200, "{\"success\": \"true\"}").verify("t")).isFalse();
  }

  @Test
  void otherStatusRejects() throws IOException {
    assertThat(verifierAnswering(500, "{\"success\": true}").verify("t")).isFalse();
  }

  @Test
  void unparsableAnswerRejects() throws IOException {
    assertThat(verifierAnswering(200, "not json").verify("t")).isFalse();
  }

  @Test
  void unreachableEndpointRejects() {
    RecaptchaVerifier verifier =
        new RecaptchaVerifier(
            HttpClient.newHttpClient(), URI.create("http://127.0.0.1:1/verify"), "s");

    assertThat(verifier.verify("t")).isFalse();
  }

  @Test
  void interruptedVerificationRejectsAndKeepsTheInterrupt() throws IOException {
    RecaptchaVerifier verifier = verifierAnswering(200, "{\"success\": true}");
    Thread.currentThread().interrupt();
    try {
      assertThat(verifier.verify("t")).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeCaptchaVerifier verifier = new TestModeCaptchaVerifier();

    assertThat(verifier.verify("test-pass")).isTrue();
    assertThat(verifier.verify("test-pass ")).isFalse();
    assertThat(verifier.verify("anything")).isFalse();
    assertThat(RecaptchaVerifier.TIMEOUT.toSeconds()).isEqualTo(5);
  }
}
