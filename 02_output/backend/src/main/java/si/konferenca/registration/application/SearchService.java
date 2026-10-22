package si.konferenca.registration.application;

import java.util.List;
import org.springframework.stereotype.Service;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.SearchCriteria;

/** Organizer search and filter of registrations (US-009, BR-09, BR-10). */
@Service
public class SearchService {

  private final RegistrationStore store;
  private final ConferenceConfiguration configuration;

  public SearchService(RegistrationStore store, ConferenceConfiguration configuration) {
    this.store = store;
    this.configuration = configuration;
  }

  /** The requested page, or the errors of the invalid parameters. */
  public record Result(List<FieldError> errors, int page, SearchPage results) {}

  public Result search(String text, String type, String option, String page) {
    SearchCriteria.Parsed parsed = SearchCriteria.parse(text, type, option, page, configuration);
    if (parsed.criteria() == null) {
      return new Result(parsed.errors(), 0, null);
    }
    return new Result(List.of(), parsed.criteria().page(), store.search(parsed.criteria()));
  }
}
