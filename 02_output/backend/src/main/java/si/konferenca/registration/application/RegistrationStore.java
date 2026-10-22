package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: the database of registrations. */
public interface RegistrationStore {

  /** Adds a registration within the current transaction. */
  void add(Registration registration);
}
