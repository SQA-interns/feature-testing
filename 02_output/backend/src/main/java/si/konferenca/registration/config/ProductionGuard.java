package si.konferenca.registration.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * SR-02 and environments.md: with {@code APP_ENVIRONMENT=production} the backend refuses to start
 * when a setting is unsafe or left at its local default. Messages name settings, never values.
 */
@Component
final class ProductionGuard {

  ProductionGuard(AppProperties properties, Environment environment) {
    if (properties.production()) {
      List<String> problems = problems(properties, environment);
      if (!problems.isEmpty()) {
        throw new IllegalStateException(
            "Refusing to start in production: " + String.join("; ", problems));
      }
    }
  }

  static List<String> problems(AppProperties properties, Environment environment) {
    List<String> problems = new ArrayList<>();
    AppProperties.Captcha captcha = properties.captcha();
    if (captcha.testMode()) {
      problems.add("CAPTCHA_TEST_MODE must be false");
    }
    if (isBlank(captcha.siteKey()) || isBlank(captcha.secretKey())) {
      problems.add("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
    }
    if (!properties.organizer().httpsOnly()) {
      problems.add("ORGANIZER_HTTPS_ONLY must be true");
    }
    if (!isBlank(properties.cors().allowedOrigin())) {
      problems.add("ALLOWED_ORIGIN is for local development only");
    }
    for (String required : List.of("DB_URL", "SMTP_HOST", "CONFERENCE_CONFIG_PATH")) {
      if (isBlank(environment.getProperty(required))) {
        problems.add(required + " must be set explicitly");
      }
    }
    return problems;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
