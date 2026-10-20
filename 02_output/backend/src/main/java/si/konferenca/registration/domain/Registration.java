package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An accepted registration (BR-07): trimmed values; fields of the other registration type are
 * {@code null}.
 */
public record Registration(
    UUID id,
    RegistrationType type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> options,
    List<GivenConsent> consents,
    Instant registeredAt) {

  public Registration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }
}
