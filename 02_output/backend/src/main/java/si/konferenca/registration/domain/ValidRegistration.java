package si.konferenca.registration.domain;

import java.util.List;

/** A validated submission: trimmed values, fields of the other type null (BR-01, BR-02). */
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
}
