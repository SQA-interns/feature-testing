package si.konferenca.registration.application;

import si.konferenca.registration.domain.RegistrationPage;
import si.konferenca.registration.domain.SearchCriteriaParser;

/** Use case: the organizer's search and list of registrations (US-009, BR-10). */
public class SearchService {

  private final SearchCriteriaParser parser;
  private final RegistrationSearch registrations;

  public SearchService(SearchCriteriaParser parser, RegistrationSearch registrations) {
    this.parser = parser;
    this.registrations = registrations;
  }

  /**
   * @throws si.konferenca.registration.domain.ValidationException when a parameter is invalid
   */
  public RegistrationPage search(String text, String type, String option, String page) {
    return registrations.search(parser.parse(text, type, option, page));
  }
}
