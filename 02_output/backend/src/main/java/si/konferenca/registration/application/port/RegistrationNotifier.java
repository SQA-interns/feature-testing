package si.konferenca.registration.application.port;

import si.konferenca.registration.domain.Registration;

/** Emails about an accepted registration (US-006, US-007). */
public interface RegistrationNotifier {

  void sendParticipantConfirmation(Registration registration);

  /** Sends one notification per organizer address, with the JSON copy attached. */
  void sendOrganizerNotifications(Registration registration, byte[] jsonCopy);
}
