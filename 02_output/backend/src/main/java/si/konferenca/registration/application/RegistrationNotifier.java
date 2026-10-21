package si.konferenca.registration.application;

import si.konferenca.registration.domain.Registration;

/** Port: the confirmation and notification emails (US-006, US-007). */
public interface RegistrationNotifier {

  void notifyParticipant(Registration registration);

  /** Sends one notification per organizer address, with the JSON copy attached. */
  void notifyOrganizers(Registration registration, byte[] jsonCopy);
}
