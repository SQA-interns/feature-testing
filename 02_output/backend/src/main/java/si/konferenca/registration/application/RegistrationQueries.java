package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SearchCriteria;
import si.konferenca.registration.domain.SearchPage;

/** Port: reading stored registrations for organizers (US-008, US-009). */
public interface RegistrationQueries {

  /** One page of matches, newest first (registration time, then id, both descending). */
  SearchPage search(SearchCriteria criteria);

  /** Every registration, oldest first (registration time, then id). */
  List<Registration> all();
}
