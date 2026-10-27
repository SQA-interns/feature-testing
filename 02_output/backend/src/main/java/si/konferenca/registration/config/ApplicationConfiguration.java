package si.konferenca.registration.config;

import java.time.Clock;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.FormSettings;
import si.konferenca.registration.domain.ConferenceCatalog;

/** Beans built from the settings, and the startup checks of SR-02 and AR-04. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfiguration {

  private static final Set<String> ENVIRONMENTS = Set.of("local", "test", "production");

  @Bean
  public ConferenceCatalog conferenceCatalog(AppProperties properties) {
    return new ConferenceConfigLoader().load(properties.conference().configFile());
  }

  @Bean
  public FormSettings formSettings(AppProperties properties) {
    checkEnvironment(properties);
    return new FormSettings(
        properties.conference().name(),
        properties.recaptcha().testMode(),
        properties.recaptcha().siteKey());
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Refuses to start with an unknown environment, or in production with reCAPTCHA test mode on or
   * empty keys (SR-02), or without organizer credentials and recipients.
   */
  static void checkEnvironment(AppProperties properties) {
    if (!ENVIRONMENTS.contains(properties.environment())) {
      throw new IllegalStateException("APP_ENVIRONMENT must be one of " + ENVIRONMENTS);
    }
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    if ("production".equals(properties.environment())
        && (recaptcha.testMode()
            || isBlank(recaptcha.siteKey())
            || isBlank(recaptcha.secretKey()))) {
      throw new IllegalStateException(
          "production needs RECAPTCHA_TEST_MODE=false and both reCAPTCHA keys");
    }
    AppProperties.Organizer organizer = properties.organizer();
    if (isBlank(organizer.username())
        || isBlank(organizer.password())
        || organizer.emailList().isEmpty()) {
      throw new IllegalStateException(
          "ORGANIZER_USERNAME, ORGANIZER_PASSWORD and ORGANIZER_EMAILS are required");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
