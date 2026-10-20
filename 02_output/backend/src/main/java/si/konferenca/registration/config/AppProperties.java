package si.konferenca.registration.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of docs/02_specification.md section 4, bound from application.properties. */
@ConfigurationProperties("app")
public record AppProperties(
    String environment,
    String conferenceConfigPath,
    String jsonCopyDir,
    Mail mail,
    Organizer organizer,
    Captcha captcha,
    Cors cors,
    RateLimit rateLimit,
    Request request) {

  /** Sender of every email. */
  public record Mail(String from) {}

  /** Organizer notification recipients and export login (SR-06: HTTPS only by default). */
  public record Organizer(
      List<String> emails, String username, String password, boolean httpsOnly) {

    public Organizer {
      emails =
          emails == null
              ? List.of()
              : List.copyOf(emails.stream().map(String::strip).filter(e -> !e.isEmpty()).toList());
    }
  }

  /** reCAPTCHA v2 (SR-01, SR-02). */
  public record Captcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {}

  /** Allowed frontend origin for CORS; empty means none. */
  public record Cors(String allowedOrigin) {}

  /** Requests per minute per client and endpoint (SR-03, D-10). */
  public record RateLimit(int perMinute) {}

  /** Largest accepted request body in bytes (SR-03). */
  public record Request(int maxBytes) {}

  public boolean production() {
    return "production".equalsIgnoreCase(environment);
  }
}
