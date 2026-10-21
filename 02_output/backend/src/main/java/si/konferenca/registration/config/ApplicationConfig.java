package si.konferenca.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceCatalog;

/** Wires the use cases to their ports (composition root). */
@Configuration
public class ApplicationConfig {

  @Bean
  public FormConfiguration formConfiguration(ConferenceCatalog catalog, AppProperties properties) {
    return new FormConfiguration(
        catalog, properties.captcha().siteKey(), properties.captcha().testMode());
  }
}
