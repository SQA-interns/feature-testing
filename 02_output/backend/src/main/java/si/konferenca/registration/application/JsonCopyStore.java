package si.konferenca.registration.application;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copies on persistent storage (BR-07). */
public interface JsonCopyStore {

  /**
   * Writes the copy named after the registration id and returns its exact bytes.
   *
   * @throws StorageException if the copy cannot be written
   */
  byte[] write(Registration registration);

  /** Removes the copy if it exists; used when the database commit fails afterwards. */
  void delete(UUID id);
}
