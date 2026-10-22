package si.konferenca.registration.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.CaptchaSettings;

/** Beans the application layer needs from the settings. */
@Configuration
public class ApplicationBeans {

  @Bean
  public CaptchaSettings captchaSettings(AppSettings settings) {
    String siteKey = settings.captcha().siteKey();
    return new CaptchaSettings(settings.captcha().testMode(), siteKey == null ? "" : siteKey);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
