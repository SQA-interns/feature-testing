package si.konferenca.registration.domain;

import java.util.List;

/**
 * A registration as submitted, before validation: every value may be missing ({@code null}) or
 * untrimmed.
 */
public record RegistrationSubmission(
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

  public RegistrationSubmission {
    options = options == null ? null : List.copyOf(options.stream().map(String::valueOf).toList());
    consents =
        consents == null ? null : List.copyOf(consents.stream().map(String::valueOf).toList());
  }
}
