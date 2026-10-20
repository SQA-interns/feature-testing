package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: confirmation and organizer emails (US-006, US-007). */
public interface RegistrationNotifier {

  void sendParticipantConfirmation(Registration registration);

  /** One notification per organizer address, with the JSON copy as attachment (AC-007-02). */
  void sendOrganizerNotification(Registration registration, byte[] jsonCopy);
}
