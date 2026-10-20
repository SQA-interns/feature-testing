package si.konferenca.registration.application;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copy of each accepted registration on persistent storage (BR-07). */
public interface JsonCopyStore {

  /**
   * Writes the copy named after the registration id.
   *
   * @return the bytes written, which the organizer notification attaches
   */
  byte[] write(Registration registration);

  /** Removes the copy if it exists; used when the database transaction fails after the write. */
  void delete(UUID id);
}
