package si.konferenca.registration.application;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copies on persistent storage (BR-07). */
public interface JsonCopyStore {

  /** Writes the copy of the registration and returns the bytes written; throws when it cannot. */
  byte[] write(Registration registration);

  /** Removes the copy if it exists; never throws. */
  void delete(UUID registrationId);
}
