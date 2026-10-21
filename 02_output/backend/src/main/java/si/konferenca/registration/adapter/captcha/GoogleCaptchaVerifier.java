package si.konferenca.registration.adapter.captcha;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import si.konferenca.registration.application.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies a reCAPTCHA v2 token with the verification endpoint ({@code recaptcha.json}); any answer
 * other than HTTP 200 with {@code success: true} counts as rejected.
 */
public class GoogleCaptchaVerifier implements CaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleCaptchaVerifier.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final RestClient client;
  private final String verifyUrl;
  private final String secretKey;
  private final JsonMapper json = JsonMapper.builder().build();

  public GoogleCaptchaVerifier(String verifyUrl, String secretKey) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(TIMEOUT);
    factory.setReadTimeout(TIMEOUT);
    this.client = RestClient.builder().requestFactory(factory).build();
    this.verifyUrl = verifyUrl;
    this.secretKey = secretKey;
  }

  @Override
  public boolean verify(String token) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("secret", secretKey);
    form.add("response", token);
    try {
      String body =
          client
              .post()
              .uri(verifyUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(String.class);
      if (body == null) {
        return false;
      }
      JsonNode success = json.readTree(body).path("success");
      return success.isBoolean() && success.booleanValue();
    } catch (RestClientException | JacksonException e) {
      String reason = e.getClass().getSimpleName();
      LOG.warn("reCAPTCHA verification failed: {}", reason);
      return false;
    }
  }
}
