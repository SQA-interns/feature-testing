package si.konferenca.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The backend's settings ({@code docs/02_specification.md} section 9), bound from {@code app.*};
 * {@code application.properties} maps each environment variable onto them.
 */
@ConfigurationProperties(prefix = "app")
public record AppSettings(
    String environment,
    String conferenceConfig,
    String conferenceName,
    String jsonCopyDir,
    Mail mail,
    Captcha captcha,
    Organizer organizer,
    String corsAllowedOrigin,
    int rateLimitPerMinute,
    long maxRequestBytes) {

  public static final String PRODUCTION = "production";

  public boolean isProduction() {
    return PRODUCTION.equals(environment);
  }

  /** SMTP connection and sender. */
  public record Mail(
      String host, int port, boolean tls, String username, String password, String from) {
    @Override
    public String toString() {
      return "Mail[host=" + host + ", port=" + port + ", tls=" + tls + ", from=" + from + "]";
    }
  }

  /** reCAPTCHA keys and test mode (SR-01, SR-02). */
  public record Captcha(boolean testMode, String siteKey, String secretKey, String verifyUrl) {
    @Override
    public String toString() {
      return "Captcha[testMode=" + testMode + ", verifyUrl=" + verifyUrl + "]";
    }
  }

  /** Organizer credentials, notification addresses and HTTPS-only access (SR-06). */
  public record Organizer(String username, String password, String emails, boolean httpsOnly) {
    @Override
    public String toString() {
      return "Organizer[httpsOnly=" + httpsOnly + "]";
    }
  }
}
