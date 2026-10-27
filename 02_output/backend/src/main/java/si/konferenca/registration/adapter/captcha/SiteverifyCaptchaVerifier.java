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
import si.konferenca.registration.application.port.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google reCAPTCHA v2 verification (recaptcha-siteverify.schema.json): a token is accepted only on
 * HTTP 200 with {@code success: true}; anything else, including a timeout, is a rejection.
 */
public class SiteverifyCaptchaVerifier implements CaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(SiteverifyCaptchaVerifier.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final URI verifyUrl;
  private final String secret;
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(TIMEOUT)
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();
  private final JsonMapper json = JsonMapper.builder().build();

  public SiteverifyCaptchaVerifier(String verifyUrl, String secret) {
    this.verifyUrl = URI.create(verifyUrl);
    this.secret = secret;
  }

  @Override
  public boolean verify(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    String form = "secret=" + encode(secret) + "&response=" + encode(token);
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("captcha verification answered HTTP {}", response.statusCode());
        return false;
      }
      JsonNode success = json.readTree(response.body()).get("success");
      return success != null && success.isBoolean() && success.asBoolean();
    } catch (IOException | JacksonException e) {
      LOG.warn("captcha verification failed: {}", e.getClass().getSimpleName());
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
