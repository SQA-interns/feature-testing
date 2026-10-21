package si.konferenca.registration.domain;

import java.util.Collections;
import java.util.List;

/** A submission as received, before validation; any value may be null. */
public record RegistrationInput(
    String type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> options,
    List<String> consents,
    String captchaToken) {

  public RegistrationInput {
    options = options == null ? null : Collections.unmodifiableList(options);
    consents = consents == null ? null : Collections.unmodifiableList(consents);
  }
}
