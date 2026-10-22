package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.SearchPage;
import si.konferenca.registration.domain.SearchRequestValidator;

/** Organizer search (US-009, BR-09, BR-10). */
public class SearchRegistrations {

  /** Found a page, or rejected parameters. */
  public sealed interface Outcome {
    /** A page of matches. */
    record Found(SearchPage page) implements Outcome {}

    /** Invalid parameters, one error each. */
    record Rejected(List<FieldError> errors) implements Outcome {}
  }

  private final SearchRequestValidator validator;
  private final RegistrationQueries store;

  public SearchRegistrations(SearchRequestValidator validator, RegistrationQueries store) {
    this.validator = validator;
    this.store = store;
  }

  public Outcome search(String text, String type, String option, String page) {
    SearchRequestValidator.Result result = validator.validate(text, type, option, page);
    if (!result.errors().isEmpty()) {
      return new Outcome.Rejected(result.errors());
    }
    return new Outcome.Found(store.search(result.criteria()));
  }
}
