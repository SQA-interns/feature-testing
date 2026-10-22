package si.konferenca.registration.adapter.captcha;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.application.CaptchaVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies a token with Google reCAPTCHA v2 ({@code docs/02_contracts/recaptcha.md}); anything but
 * an HTTP 200 answer with {@code "success": true} is a rejection.
 */
public class RecaptchaVerifier implements CaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(RecaptchaVerifier.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final HttpClient client;
  private final URI verifyUrl;
  private final String secret;

  public RecaptchaVerifier(HttpClient client, URI verifyUrl, String secret) {
    this.client = client;
    this.verifyUrl = verifyUrl;
    this.secret = secret;
  }

  @Override
  public boolean verify(String token) {
    String form = "secret=" + encode(secret) + "&response=" + encode(token);
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("reCAPTCHA verification answered HTTP {}", response.statusCode());
        return false;
      }
      JsonNode answer = JSON.readTree(response.body());
      return answer.path("success").isBoolean() && answer.path("success").asBoolean();
    } catch (IOException | RuntimeException e) {
      LOG.warn("reCAPTCHA verification failed: {}", e.getClass().getSimpleName());
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
