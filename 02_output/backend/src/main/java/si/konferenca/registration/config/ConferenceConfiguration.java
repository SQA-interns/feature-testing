package si.konferenca.registration.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.FormConfigService;
import si.konferenca.registration.domain.ConferenceConfig;

/** Loads the conference configuration once at start (AR-04). */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ConferenceConfiguration {

  @Bean
  ConferenceConfig conferenceConfig(AppProperties properties) {
    return ConferenceConfigLoader.load(properties.conferenceConfigPath());
  }

  @Bean
  FormConfigService formConfigService(ConferenceConfig conference, AppProperties properties) {
    return new FormConfigService(
        conference, properties.captcha().siteKey(), properties.captcha().testMode());
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
