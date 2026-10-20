package si.konferenca.registration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A submission that passed validation, not yet given an id and a time. */
public record ValidRegistration(
    RegistrationType type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> options,
    List<String> consents) {

  public ValidRegistration {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  /** The accepted registration; every consent counts as given at the registration time. */
  public Registration accept(UUID id, Instant registeredAt) {
    return new Registration(
        id,
        type,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        options,
        consents.stream().map(c -> new GivenConsent(c, registeredAt)).toList(),
        registeredAt);
  }
}
