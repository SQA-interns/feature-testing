package si.konferenca.registration.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Refuses settings that are missing or unsafe (ES-01, SR-02, SR-06): the backend does not start
 * with them. Messages name the setting, never its value.
 */
final class StartupChecks {

  private static final Set<String> ENVIRONMENTS = Set.of("local", "test", AppSettings.PRODUCTION);

  private StartupChecks() {}

  static void verify(AppSettings settings) {
    List<String> problems = new ArrayList<>();
    if (!ENVIRONMENTS.contains(settings.environment())) {
      problems.add("APP_ENVIRONMENT must be local, test or production");
    }
    requireValue(problems, settings.conferenceConfig(), "CONFERENCE_CONFIG");
    requireValue(problems, settings.jsonCopyDir(), "JSON_COPY_DIR");
    requireValue(problems, settings.organizer().username(), "ORGANIZER_USERNAME");
    requireValue(problems, settings.organizer().password(), "ORGANIZER_PASSWORD");
    requireValue(problems, settings.organizer().emails(), "ORGANIZER_EMAILS");
    if (settings.rateLimitPerMinute() < 1) {
      problems.add("RATE_LIMIT_PER_MINUTE must be at least 1");
    }
    if (settings.maxRequestBytes() < 1) {
      problems.add("MAX_REQUEST_BYTES must be at least 1");
    }
    if (settings.isProduction()) {
      if (settings.captcha().testMode()) {
        problems.add("CAPTCHA_TEST_MODE must be off in production");
      }
      requireValue(problems, settings.captcha().siteKey(), "RECAPTCHA_SITE_KEY");
      requireValue(problems, settings.captcha().secretKey(), "RECAPTCHA_SECRET_KEY");
      if (!settings.organizer().httpsOnly()) {
        problems.add("ORGANIZER_HTTPS_ONLY must be on in production");
      }
      if (hasValue(settings.corsAllowedOrigin())) {
        problems.add("CORS_ALLOWED_ORIGIN is allowed only locally");
      }
    }
    if (!problems.isEmpty()) {
      throw new IllegalStateException("Invalid configuration: " + String.join("; ", problems));
    }
  }

  private static void requireValue(List<String> problems, String value, String name) {
    if (!hasValue(value)) {
      problems.add(name + " is required");
    }
  }

  private static boolean hasValue(String value) {
    return value != null && !value.isBlank();
  }
}
