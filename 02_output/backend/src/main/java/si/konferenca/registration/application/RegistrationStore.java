package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SearchCriteria;

/** Port: the database of registrations (BR-07). */
public interface RegistrationStore {

  /** Inserts the registration inside the caller's transaction and flushes it. */
  void save(Registration registration);

  /** Every registration in the order they were stored (export, BR-08). */
  List<Registration> findAllOldestFirst();

  /** The page of matching registrations, newest first, and the number of all matches. */
  SearchPage search(SearchCriteria criteria);
}
