package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An accepted registration (BR-07). */
public record Registration(
    UUID id,
    RegistrationType type,
    Instant registeredAt,
    Participant participant,
    List<SelectedOption> options,
    List<GivenConsent> consents) {

  /** Copies the lists so the registration cannot change afterwards. */
  public Registration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }
}
