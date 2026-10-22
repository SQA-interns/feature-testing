package si.konferenca.registration.domain;

import java.util.List;

/**
 * A valid submission with trimmed text (BR-02); the fields of the other registration type are null.
 */
public record RegistrationData(
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

  public RegistrationData {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }
}
