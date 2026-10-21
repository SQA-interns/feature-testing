package si.konferenca.registration.application;

import java.util.UUID;
import si.konferenca.registration.domain.Registration;

/** Port: the raw JSON copies on persistent storage (BR-07, AR-05). */
public interface JsonCopyStore {

  /**
   * Writes {@code <id>.json} and returns the bytes written.
   *
   * @throws RuntimeException when the copy cannot be written
   */
  byte[] save(Registration registration);

  /** Removes the copy again, if it exists. */
  void discard(UUID registrationId);
}
