package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: database storage of registrations (BR-07). */
public interface RegistrationRepository {

  /** Inserts the registration and flushes, so a failure surfaces inside the transaction. */
  void save(Registration registration);

  /** Every stored registration, oldest first. */
  List<Registration> findAll();
}
