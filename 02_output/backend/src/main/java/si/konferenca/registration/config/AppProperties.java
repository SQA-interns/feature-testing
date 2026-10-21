package si.konferenca.registration.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** The {@code app.*} settings of {@code docs/02_specification.md} section 3 (ES-01). */
@ConfigurationProperties("app")
public record AppProperties(
    Mail mail,
    Conference conference,
    JsonCopy jsonCopy,
    Captcha captcha,
    Organizer organizer,
    Cors cors,
    RateLimit rateLimit,
    Request request) {

  /** Sender of every email. */
  public record Mail(String from) {}

  /** Conference name and the location of the options configuration. */
  public record Conference(String name, String configLocation) {}

  /** Directory of the raw JSON copies. */
  public record JsonCopy(String directory) {}

  /** reCAPTCHA settings; test mode accepts only {@code test-pass}. */
  public record Captcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {

    @Override
    public String toString() {
      return "Captcha[testMode=" + testMode + ", siteKey=" + siteKey + "]";
    }
  }

  /** Organizer login, notification recipients and HTTPS-only access. */
  public record Organizer(String username, String password, String emails, boolean httpsOnly) {

    /** The configured recipients, trimmed, without empty entries. */
    public List<String> emailList() {
      if (emails == null) {
        return List.of();
      }
      return Arrays.stream(emails.split(",")).map(String::strip).filter(e -> !e.isEmpty()).toList();
    }

    @Override
    public String toString() {
      return "Organizer[username=" + username + ", httpsOnly=" + httpsOnly + "]";
    }
  }

  /** The one origin allowed for cross-origin API calls, or empty. */
  public record Cors(String allowedOrigin) {}

  /** Requests per minute per client and endpoint. */
  public record RateLimit(int perMinute) {}

  /** Maximum request body size in bytes. */
  public record Request(long maxBytes) {}
}
