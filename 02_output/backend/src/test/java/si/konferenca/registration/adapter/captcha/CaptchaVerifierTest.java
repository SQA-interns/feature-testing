package si.konferenca.registration.adapter.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CaptchaVerifierTest {

  HttpServer server;
  final List<String> requests = new ArrayList<>();
  int status = 200;
  String answer = "{\"success\": true}";

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/verify",
        exchange -> {
          requests.add(
              exchange.getRequestMethod()
                  + " "
                  + exchange.getRequestHeaders().getFirst("Content-Type")
                  + " "
                  + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] body = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  RecaptchaHttpVerifier verifier(String path) {
    return new RecaptchaHttpVerifier(
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path),
        "s3cr&t",
        HttpClient.newHttpClient());
  }

  @Test
  void postsTheEncodedSecretAndToken() {
    assertThat(verifier("/verify").verify("to ken")).isTrue();

    assertThat(requests)
        .containsExactly("POST application/x-www-form-urlencoded secret=s3cr%26t&response=to+ken");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "200|{\"success\": false}",
        "200|{\"success\": \"true\"}",
        "200|{}",
        "200|not json",
        "500|{\"success\": true}",
      })
  void anythingButSuccessIsRejected(int status, String answer) {
    this.status = status;
    this.answer = answer;

    assertThat(verifier("/verify").verify("token")).isFalse();
  }

  @Test
  void blankTokensAreRejectedWithoutACall() {
    assertThat(verifier("/verify").verify(null)).isFalse();
    assertThat(verifier("/verify").verify(" ")).isFalse();
    assertThat(requests).isEmpty();
  }

  @Test
  void unreachableEndpointIsRejected() {
    server.stop(0);
    assertThat(verifier("/verify").verify("token")).isFalse();
  }

  @Test
  void nullSecretIsSentEmpty() {
    RecaptchaHttpVerifier noSecret =
        new RecaptchaHttpVerifier(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/verify"),
            null,
            HttpClient.newHttpClient());
    noSecret.verify("t");
    assertThat(requests).singleElement().asString().endsWith("secret=&response=t");
  }

  @Test
  void interruptedCallIsRejectedAndKeepsTheInterrupt() {
    Thread.currentThread().interrupt();
    try {
      assertThat(verifier("/verify").verify("token")).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void testModeAcceptsOnlyTheTestToken() {
    TestModeCaptchaVerifier testMode = new TestModeCaptchaVerifier();
    assertThat(testMode.verify("test-pass")).isTrue();
    assertThat(testMode.verify("test-pass ")).isFalse();
    assertThat(testMode.verify(null)).isFalse();
  }
}
