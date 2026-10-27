package si.konferenca.registration.application.port;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Raw JSON copies on persistent storage (BR-07, registration-copy.schema.json). */
public interface JsonCopyStore {

  /**
   * Writes the copy atomically.
   *
   * @return the exact bytes written
   */
  byte[] write(Registration registration);

  /** Removes the copy, if it exists. */
  void delete(UUID registrationId);
}
