package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The registration rules (BR-01..BR-05, SR-04), checked on the server whatever the client did
 * (SB-01). Reports the first problem of every invalid field.
 */
public final class RegistrationValidator {

  public static final int MAX_TEXT_LENGTH = 200;

  private final ConferenceConfiguration configuration;

  public RegistrationValidator(ConferenceConfiguration configuration) {
    this.configuration = configuration;
  }

  /** The outcome: either field errors or the normalized data, never both. */
  public record Result(List<FieldError> errors, RegistrationData data) {

    public boolean valid() {
      return errors.isEmpty();
    }
  }

  public Result validate(RegistrationSubmission submission) {
    List<FieldError> errors = new ArrayList<>();
    Optional<RegistrationType> type = RegistrationType.parse(submission.type());
    if (type.isEmpty()) {
      errors.add(new FieldError("type", "Choose EXTERNAL or STUDENT."));
    }
    String firstName = text("firstName", submission.firstName(), errors);
    String lastName = text("lastName", submission.lastName(), errors);
    String email = email(submission.email(), errors);
    String organization = null;
    String studyInstitution = null;
    String studyProgramme = null;
    String studentId = null;
    if (type.orElse(null) == RegistrationType.EXTERNAL) {
      organization = text("organization", submission.organization(), errors);
    } else if (type.orElse(null) == RegistrationType.STUDENT) {
      studyInstitution = text("studyInstitution", submission.studyInstitution(), errors);
      studyProgramme = text("studyProgramme", submission.studyProgramme(), errors);
      studentId = text("studentId", submission.studentId(), errors);
    }
    List<String> options = list(submission.options());
    type.ifPresent(t -> checkOptions(t, options, errors));
    List<String> consents = list(submission.consents());
    checkConsents(consents, errors);
    String token = submission.captchaToken();
    if (token == null || token.isBlank()) {
      errors.add(new FieldError("captchaToken", "Confirm that you are not a robot."));
    }
    if (!errors.isEmpty()) {
      return new Result(List.copyOf(errors), null);
    }
    return new Result(
        List.of(),
        new RegistrationData(
            type.orElseThrow(),
            firstName,
            lastName,
            email,
            organization,
            studyInstitution,
            studyProgramme,
            studentId,
            options,
            consents));
  }

  private static List<String> list(List<String> values) {
    return values == null ? List.of() : new ArrayList<>(values);
  }

  private static String text(String field, String value, List<FieldError> errors) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty()) {
      errors.add(new FieldError(field, "This field is required."));
      return null;
    }
    if (trimmed.length() > MAX_TEXT_LENGTH) {
      errors.add(new FieldError(field, "At most " + MAX_TEXT_LENGTH + " characters."));
      return null;
    }
    return trimmed;
  }

  private static String email(String value, List<FieldError> errors) {
    String trimmed = text("email", value, errors);
    if (trimmed != null && !EmailAddress.isValid(trimmed)) {
      errors.add(new FieldError("email", "Enter a valid email address."));
      return null;
    }
    return trimmed;
  }

  private void checkOptions(RegistrationType type, List<String> ids, List<FieldError> errors) {
    Set<String> seen = new HashSet<>();
    for (String id : ids) {
      if (id == null || !seen.add(id)) {
        errors.add(new FieldError("options", "Each option can be selected only once."));
        return;
      }
      boolean selectable =
          configuration.option(id).map(option -> option.selectableBy(type)).orElse(false);
      if (!selectable) {
        errors.add(new FieldError("options", "Option " + id + " is not available."));
        return;
      }
    }
  }

  private void checkConsents(List<String> ids, List<FieldError> errors) {
    Set<String> seen = new HashSet<>();
    for (String id : ids) {
      if (id == null || !seen.add(id) || configuration.consent(id).isEmpty()) {
        errors.add(new FieldError("consents", "Unknown or repeated consent."));
        return;
      }
    }
    for (ConsentDefinition consent : configuration.consents()) {
      if (consent.mandatory() && !seen.contains(consent.id())) {
        errors.add(new FieldError("consents", "The consent \"" + consent.id() + "\" is required."));
        return;
      }
    }
  }
}
