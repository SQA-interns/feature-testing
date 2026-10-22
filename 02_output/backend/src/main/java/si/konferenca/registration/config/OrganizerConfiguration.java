package si.konferenca.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.adapter.export.PoiWorkbookWriter;
import si.konferenca.registration.application.ExportRegistrations;
import si.konferenca.registration.application.RegistrationQueries;
import si.konferenca.registration.application.SearchRegistrations;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.SearchRequestValidator;

/** The organizer use cases: search (US-009) and export (US-008). */
@Configuration
public class OrganizerConfiguration {

  @Bean
  SearchRegistrations searchRegistrations(
      ConferenceCatalogue catalogue, RegistrationQueries queries) {
    return new SearchRegistrations(new SearchRequestValidator(catalogue), queries);
  }

  @Bean
  ExportRegistrations exportRegistrations(RegistrationQueries queries) {
    return new ExportRegistrations(queries, new PoiWorkbookWriter());
  }
}
