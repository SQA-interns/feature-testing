package si.konferenca.registration.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import si.konferenca.registration.domain.Submission;

/** Body of POST /api/registrations (openapi.yaml RegistrationRequest). */
public record RegistrationRequest(
    String type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId,
    List<String> optionIds,
    List<String> consents,
    String recaptchaToken) {

  /** Copies the lists as received (nulls kept); a missing list becomes empty. */
  public RegistrationRequest {
    optionIds =
        Collections.unmodifiableList(
            optionIds == null ? new ArrayList<>() : new ArrayList<>(optionIds));
    consents =
        Collections.unmodifiableList(
            consents == null ? new ArrayList<>() : new ArrayList<>(consents));
  }

  Submission toSubmission() {
    return new Submission(
        type,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        optionIds,
        consents,
        recaptchaToken);
  }
}
