package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An accepted registration as stored in the database and the JSON copy (BR-07). */
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

  /** Accepts a validated submission now; every consent is given at the registration time. */
  public static Registration accept(ValidRegistration valid, UUID id, Instant registeredAt) {
    return new Registration(
        id,
        valid.type(),
        valid.firstName(),
        valid.lastName(),
        valid.email(),
        valid.organization(),
        valid.studyInstitution(),
        valid.studyProgramme(),
        valid.studentId(),
        valid.options(),
        valid.consents().stream().map(c -> new GivenConsent(c, registeredAt)).toList(),
        registeredAt);
  }
}
