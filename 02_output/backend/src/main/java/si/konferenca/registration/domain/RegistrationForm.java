package si.konferenca.registration.domain;

import java.util.List;

/** A submitted registration exactly as received; any field may be missing (null). */
public record RegistrationForm(
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

  /**
   * A missing list counts as empty; a missing entry as an empty id, which no option or consent has.
   */
  public RegistrationForm {
    options =
        options == null
            ? List.of()
            : List.copyOf(options.stream().map(id -> id == null ? "" : id).toList());
    consents =
        consents == null
            ? List.of()
            : List.copyOf(consents.stream().map(id -> id == null ? "" : id).toList());
  }
}
