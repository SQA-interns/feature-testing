package si.konferenca.registration.config;

import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.adapter.catalog.JsonConferenceCatalogLoader;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceConfigException;

/** Loads the conference options and consents once at start (AR-04, AC-003-02, AC-003-03). */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class CatalogConfig {

  @Bean
  public ConferenceCatalog conferenceCatalog(AppProperties properties, ResourceLoader loader) {
    String location = properties.conference().configLocation();
    if (location == null || location.isBlank()) {
      throw new ConferenceConfigException("CONFERENCE_CONFIG is not set");
    }
    Resource resource = loader.getResource(location);
    if (!resource.exists()) {
      throw new ConferenceConfigException("Conference configuration " + location + " not found");
    }
    try {
      return new JsonConferenceCatalogLoader().load(resource.getInputStream(), location);
    } catch (IOException e) {
      throw new ConferenceConfigException(
          "Conference configuration " + location + " is unreadable", e);
    }
  }
}
