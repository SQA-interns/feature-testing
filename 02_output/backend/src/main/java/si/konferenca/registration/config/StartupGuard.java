package si.konferenca.registration.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start with settings that are allowed only locally or that leave a control without its
 * secret (SR-02, SR-06, ES-01).
 */
@Component
public final class StartupGuard {

  public StartupGuard(AppProperties properties, Environment environment) {
    List<String> problems =
        problems(
            properties,
            environment.acceptsProfiles(Profiles.of("local", "test")),
            environment.acceptsProfiles(Profiles.of("local")));
    if (!problems.isEmpty()) {
      throw new IllegalStateException("Refusing to start: " + String.join("; ", problems));
    }
  }

  List<String> problems(AppProperties p, boolean localOrTest, boolean local) {
    List<String> problems = new ArrayList<>();
    AppProperties.Captcha captcha = p.captcha();
    if (captcha.testMode() && !localOrTest) {
      problems.add("CAPTCHA_TEST_MODE=true is allowed only in the local and test profiles");
    }
    if (!captcha.testMode() && (blank(captcha.siteKey()) || blank(captcha.secretKey()))) {
      problems.add("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY are required without test mode");
    }
    AppProperties.Organizer organizer = p.organizer();
    if (!organizer.httpsOnly() && !localOrTest) {
      problems.add("ORGANIZER_HTTPS_ONLY=false is allowed only in the local and test profiles");
    }
    if (blank(organizer.username()) || blank(organizer.password())) {
      problems.add("ORGANIZER_USERNAME and ORGANIZER_PASSWORD are required");
    }
    if (organizer.emailList().isEmpty()) {
      problems.add("ORGANIZER_EMAILS is required");
    }
    if (!blank(p.cors().allowedOrigin()) && !local) {
      problems.add("CORS_ALLOWED_ORIGIN is allowed only in the local profile");
    }
    return problems;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
