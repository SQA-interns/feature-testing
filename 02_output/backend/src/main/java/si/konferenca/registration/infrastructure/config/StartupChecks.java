package si.konferenca.registration.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Refuses to start with settings that are unsafe or incomplete (SR-02, SR-06, ES-01). */
@Configuration
@EnableConfigurationProperties(AppSettings.class)
public class StartupChecks {

  public StartupChecks(AppSettings settings) {
    check(settings);
  }

  static void check(AppSettings settings) {
    AppSettings.Captcha captcha = settings.captcha();
    if (!captcha.testMode() && (blank(captcha.siteKey()) || blank(captcha.secretKey()))) {
      fail("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY are required unless CAPTCHA_TEST_MODE");
    }
    AppSettings.Organizer organizer = settings.organizer();
    if (blank(organizer.username()) || blank(organizer.password())) {
      fail("ORGANIZER_USERNAME and ORGANIZER_PASSWORD are required");
    }
    if (settings.production()) {
      if (captcha.testMode()) {
        fail("CAPTCHA_TEST_MODE must be off in production");
      }
      if (!organizer.httpsOnly()) {
        fail("ORGANIZER_HTTPS_ONLY must be on in production");
      }
      if (!blank(settings.corsAllowedOrigin())) {
        fail("CORS_ALLOWED_ORIGIN must not be set in production");
      }
    }
    if (settings.rateLimitPerMinute() < 1 || settings.requestMaxBytes() < 1) {
      fail("RATE_LIMIT_PER_MINUTE and REQUEST_MAX_BYTES must be positive");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static void fail(String message) {
    throw new IllegalStateException("Refusing to start: " + message);
  }
}
