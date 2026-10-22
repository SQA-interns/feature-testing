package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: emails about an accepted registration (US-006, US-007). */
public interface Notifier {

  /** Confirmation to the participant. */
  void sendConfirmation(Registration registration);

  /** Notification to every organizer address, with the JSON copy attached. */
  void notifyOrganizers(Registration registration, byte[] jsonCopy);
}
