package si.konferenca.registration.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceCatalogue;

/** Wires the use cases to their adapters and checks the settings at start-up. */
@Configuration
@EnableConfigurationProperties(AppSettings.class)
public class ApplicationConfiguration {

  @Bean
  ConferenceCatalogue conferenceCatalogue(AppSettings settings, ResourceLoader resourceLoader) {
    StartupChecks.verify(settings);
    return ConferenceConfigLoader.load(resourceLoader, settings.conferenceConfig());
  }

  @Bean
  FormConfiguration formConfiguration(ConferenceCatalogue catalogue) {
    return new FormConfiguration(catalogue);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
