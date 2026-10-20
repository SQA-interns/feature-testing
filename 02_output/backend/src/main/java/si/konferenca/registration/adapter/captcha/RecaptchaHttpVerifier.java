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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google reCAPTCHA v2 verification on the backend (SR-01): posts the secret and the token to the
 * verify URL and accepts only {@code {"success": true}}. Any other answer, error or timeout rejects
 * the token.
 */
public class RecaptchaHttpVerifier implements CaptchaVerifier {

  static final Duration TIMEOUT = Duration.ofSeconds(5);

  private static final Logger LOG = LoggerFactory.getLogger(RecaptchaHttpVerifier.class);
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final URI verifyUrl;
  private final String secretKey;
  private final HttpClient client;

  public RecaptchaHttpVerifier(URI verifyUrl, String secretKey, HttpClient client) {
    this.verifyUrl = verifyUrl;
    this.secretKey = secretKey;
    this.client = client;
  }

  @Override
  public boolean verify(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    String form = "secret=" + encode(secretKey) + "&response=" + encode(token);
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
      JsonNode answer = MAPPER.readTree(response.body());
      return answer.path("success").isBoolean() && answer.path("success").asBoolean();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (IOException | JacksonException | IllegalArgumentException e) {
      LOG.warn("reCAPTCHA verification failed ({})", e.getClass().getSimpleName());
      return false;
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
