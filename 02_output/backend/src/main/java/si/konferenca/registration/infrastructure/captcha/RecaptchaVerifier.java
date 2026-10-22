package si.konferenca.registration.infrastructure.captcha;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import si.konferenca.registration.application.CaptchaUnavailableException;
import si.konferenca.registration.application.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Verifies a Google reCAPTCHA v2 token with the siteverify endpoint (SR-01, DoD-P05). */
public class RecaptchaVerifier implements CaptchaVerifier {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpClient client;
  private final URI verifyUrl;
  private final String secretKey;

  public RecaptchaVerifier(HttpClient client, String verifyUrl, String secretKey) {
    this.client = client;
    this.verifyUrl = URI.create(verifyUrl);
    this.secretKey = secretKey;
  }

  public static RecaptchaVerifier create(String verifyUrl, String secretKey) {
    return new RecaptchaVerifier(
        HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), verifyUrl, secretKey);
  }

  @Override
  public boolean verify(String token) {
    String form =
        "secret="
            + URLEncoder.encode(secretKey, StandardCharsets.UTF_8)
            + "&response="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new CaptchaUnavailableException("reCAPTCHA answered " + response.statusCode(), null);
      }
      return JSON.readTree(response.body()).path("success").asBoolean(false);
    } catch (IOException | JacksonException e) {
      throw new CaptchaUnavailableException("reCAPTCHA verification failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CaptchaUnavailableException("reCAPTCHA verification interrupted", e);
    }
  }
}
