package si.konferenca.registration.application;

import si.konferenca.registration.domain.RegistrationPage;
import si.konferenca.registration.domain.SearchCriteria;

/** Port: the organizer's search over the stored registrations (US-009). */
public interface RegistrationSearch {

  /**
   * The registrations matching every condition of {@code criteria}, newest first, from its offset,
   * at most {@link SearchCriteria#PAGE_SIZE}, with the number of all matches (BR-10).
   */
  RegistrationPage search(SearchCriteria criteria);
}
