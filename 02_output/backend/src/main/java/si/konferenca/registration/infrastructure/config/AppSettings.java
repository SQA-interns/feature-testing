package si.konferenca.registration.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Environment-specific settings (ES-01), bound from `app.*` in application.yml. */
@ConfigurationProperties("app")
public record AppSettings(
    String environment,
    String conferenceName,
    String mailFrom,
    String configFile,
    String jsonCopyDir,
    Organizer organizer,
    Captcha captcha,
    String corsAllowedOrigin,
    int rateLimitPerMinute,
    long requestMaxBytes) {

  public boolean production() {
    return "production".equalsIgnoreCase(environment);
  }

  /** Organizer access and notification recipients; the password is never printed. */
  public record Organizer(String username, String password, String emails, boolean httpsOnly) {

    public List<String> emailList() {
      if (emails == null) {
        return List.of();
      }
      return Arrays.stream(emails.split(",")).map(String::trim).filter(e -> !e.isEmpty()).toList();
    }

    @Override
    public String toString() {
      return "Organizer[username=" + username + ", httpsOnly=" + httpsOnly + "]";
    }
  }

  /** reCAPTCHA settings (SR-01, SR-02); the secret key is never printed. */
  public record Captcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {

    @Override
    public String toString() {
      return "Captcha[testMode=" + testMode + ", verifyUrl=" + verifyUrl + "]";
    }
  }
}
