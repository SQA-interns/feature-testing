package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: the database of registrations (BR-07). */
public interface RegistrationStore {

  /** Inserts the registration inside the caller's transaction and flushes it. */
  void save(Registration registration);
}
