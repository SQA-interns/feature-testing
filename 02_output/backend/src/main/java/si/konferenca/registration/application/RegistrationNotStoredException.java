package si.konferenca.registration.application;

import java.util.UUID;

/** The registration could not be stored completely, so nothing of it was kept (AR-05). */
public class RegistrationNotStoredException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RegistrationNotStoredException(UUID id, Throwable cause) {
    super("Registration " + id + " could not be stored", cause);
  }
}
