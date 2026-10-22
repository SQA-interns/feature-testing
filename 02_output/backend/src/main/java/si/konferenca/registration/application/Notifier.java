package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: the participant confirmation and organizer notification emails (US-006, US-007). */
public interface Notifier {

  /** Sends both emails for a stored registration; {@code jsonCopy} is the stored copy's bytes. */
  void registrationAccepted(Registration registration, byte[] jsonCopy);
}
