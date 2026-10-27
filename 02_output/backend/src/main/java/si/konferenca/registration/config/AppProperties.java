package si.konferenca.registration.config;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed settings; application.yml maps the variables of specification S7.1 onto them. */
@ConfigurationProperties("app")
public record AppProperties(
    String environment,
    Conference conference,
    Path jsonCopyDir,
    Recaptcha recaptcha,
    Organizer organizer,
    Mail mail,
    String corsAllowedOrigin,
    RateLimit rateLimit,
    int maxRequestBytes) {

  /** Conference name and the options file (AR-04). */
  public record Conference(String name, Path configFile) {}

  /** reCAPTCHA settings (SR-01, SR-02). */
  public record Recaptcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {}

  /** Organizer credentials and notification recipients (BR-08, US-007, SR-06). */
  public record Organizer(String username, String password, String emails, boolean httpsOnly) {

    /** The comma-separated recipients as a list. */
    public List<String> emailList() {
      return List.of(emails.split(",")).stream()
          .map(String::strip)
          .filter(e -> !e.isEmpty())
          .toList();
    }

    @Override
    public String toString() {
      return "Organizer[username=" + username + ", httpsOnly=" + httpsOnly + "]";
    }
  }

  /** SMTP server and sender (US-006, US-007). */
  public record Mail(
      String host, int port, boolean starttls, String username, String password, String from) {

    @Override
    public String toString() {
      return "Mail[host=" + host + ", port=" + port + ", starttls=" + starttls + "]";
    }
  }

  /** Requests per client and minute (SR-03). */
  public record RateLimit(
      int formConfigPerMinute, int registrationPerMinute, int exportPerMinute) {}
}
