package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: the database of registrations (BR-07). */
public interface RegistrationRepository {

  /** Stores the registration and flushes, so a database error surfaces here. */
  void save(Registration registration);

  /** Every registration ordered by registration time, then id. */
  List<Registration> findAllOrdered();
}
