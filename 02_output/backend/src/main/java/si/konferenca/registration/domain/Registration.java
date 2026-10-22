package si.konferenca.registration.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** An accepted registration as it is stored (BR-07, AC-005-01). */
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

  /** A consent and the time it was given (SB-14). */
  public record GivenConsent(String consentId, Instant givenAt) {}

  /**
   * Accepts valid data now: a new id, the registration time in milliseconds, and every consent
   * given at that time.
   */
  public static Registration accept(UUID id, RegistrationData data, Instant now) {
    Instant at = now.truncatedTo(ChronoUnit.MILLIS);
    return new Registration(
        id,
        data.type(),
        data.firstName(),
        data.lastName(),
        data.email(),
        data.organization(),
        data.studyInstitution(),
        data.studyProgramme(),
        data.studentId(),
        data.options(),
        data.consents().stream().map(c -> new GivenConsent(c, at)).toList(),
        at);
  }
}
