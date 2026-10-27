package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A registration as submitted, before validation; every value may be missing. */
public record Submission(
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

  /** Copies the lists (which may hold nulls, as submitted); a missing list becomes empty. */
  public Submission {
    optionIds =
        Collections.unmodifiableList(
            optionIds == null ? new ArrayList<>() : new ArrayList<>(optionIds));
    consents =
        Collections.unmodifiableList(
            consents == null ? new ArrayList<>() : new ArrayList<>(consents));
  }
}
