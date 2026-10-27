package si.konferenca.registration.application.port;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Database storage of registrations (BR-07). */
public interface RegistrationStore {

  /** Inserts the registration and flushes it within the current transaction. */
  void save(Registration registration);

  /** Every stored registration, oldest first. */
  List<Registration> findAllOldestFirst();
}
