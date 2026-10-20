package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Server-side validation of a submission (SB-01): BR-01 … BR-05, OQ-01, OQ-04, D-08, D-09. All
 * errors are collected, one per field.
 */
public final class RegistrationValidator {

  /** D-08: maximum length of a text field after trimming. */
  public static final int MAX_TEXT_LENGTH = 200;

  /** D-08: maximum length of an email address (RFC 5321). */
  public static final int MAX_EMAIL_LENGTH = 254;

  static final String REQUIRED = "This field is required.";
  static final String TOO_LONG = "This field must be at most %d characters long.";
  static final String INVALID_EMAIL = "Enter a valid email address.";
  static final String INVALID_TYPE = "Choose a registration type.";
  static final String UNAVAILABLE_OPTION = "One of the selected options is not available.";
  static final String DUPLICATE_OPTION = "Each option can be selected only once.";
  static final String MISSING_CONSENT = "The required consent must be given.";
  static final String UNKNOWN_CONSENT = "One of the consents is not known.";
  static final String MISSING_CAPTCHA = "Please confirm that you are not a robot.";

  /** D-09: atext local part, dot-separated ASCII domain labels with at least one dot. */
  private static final Pattern EMAIL =
      Pattern.compile(
          "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
              + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
              + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+");

  private final ConferenceConfig conference;

  public RegistrationValidator(ConferenceConfig conference) {
    this.conference = conference;
  }

  /**
   * @return the trimmed, accepted values
   * @throws ValidationException with every invalid field
   */
  public ValidRegistration validate(RegistrationSubmission submission) {
    List<FieldError> errors = new ArrayList<>();
    RegistrationType type = parseType(submission.type(), errors);
    String firstName = requiredText("firstName", submission.firstName(), errors);
    String lastName = requiredText("lastName", submission.lastName(), errors);
    String email = email(submission.email(), errors);
    String organization = null;
    String studyInstitution = null;
    String studyProgramme = null;
    String studentId = null;
    if (type == RegistrationType.EXTERNAL) {
      organization = requiredText("organization", submission.organization(), errors);
    } else if (type == RegistrationType.STUDENT) {
      studyInstitution = requiredText("studyInstitution", submission.studyInstitution(), errors);
      studyProgramme = requiredText("studyProgramme", submission.studyProgramme(), errors);
      studentId = requiredText("studentId", submission.studentId(), errors);
    }
    List<String> options = options(type, submission.options(), errors);
    List<String> consents = consents(submission.consents(), errors);
    if (isBlank(submission.captchaToken())) {
      errors.add(new FieldError("captchaToken", MISSING_CAPTCHA));
    }
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    return new ValidRegistration(
        type,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        options,
        consents);
  }

  private static RegistrationType parseType(String value, List<FieldError> errors) {
    if (value != null) {
      for (RegistrationType type : RegistrationType.values()) {
        if (type.name().equals(value.strip())) {
          return type;
        }
      }
    }
    errors.add(new FieldError("type", INVALID_TYPE));
    return null;
  }

  private static String requiredText(String field, String value, List<FieldError> errors) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty()) {
      errors.add(new FieldError(field, REQUIRED));
      return null;
    }
    if (trimmed.length() > MAX_TEXT_LENGTH) {
      errors.add(new FieldError(field, TOO_LONG.formatted(MAX_TEXT_LENGTH)));
      return null;
    }
    return trimmed;
  }

  private static String email(String value, List<FieldError> errors) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty()) {
      errors.add(new FieldError("email", REQUIRED));
      return null;
    }
    if (trimmed.length() > MAX_EMAIL_LENGTH) {
      errors.add(new FieldError("email", TOO_LONG.formatted(MAX_EMAIL_LENGTH)));
      return null;
    }
    if (!EMAIL.matcher(trimmed).matches()) {
      errors.add(new FieldError("email", INVALID_EMAIL));
      return null;
    }
    return trimmed;
  }

  private List<String> options(RegistrationType type, List<String> ids, List<FieldError> errors) {
    List<String> selected = ids == null ? List.of() : ids;
    Set<String> seen = new HashSet<>();
    for (String id : selected) {
      if (!seen.add(id)) {
        errors.add(new FieldError("options", DUPLICATE_OPTION));
        return List.of();
      }
    }
    for (String id : selected) {
      Optional<ConferenceOption> option = conference.option(id);
      boolean available =
          option.isPresent()
              && option.get().active()
              && (type == null || option.get().isSelectableBy(type));
      if (!available) {
        errors.add(new FieldError("options", UNAVAILABLE_OPTION));
        return List.of();
      }
    }
    return selected;
  }

  private List<String> consents(List<String> ids, List<FieldError> errors) {
    List<String> given = ids == null ? List.of() : ids;
    Set<String> distinct = new HashSet<>(given);
    if (distinct.size() != given.size()
        || !given.stream().allMatch(id -> conference.consent(id).isPresent())) {
      errors.add(new FieldError("consents", UNKNOWN_CONSENT));
      return List.of();
    }
    for (ConsentDefinition consent : conference.consents()) {
      if (consent.mandatory() && !distinct.contains(consent.id())) {
        errors.add(new FieldError("consents", MISSING_CONSENT));
        return List.of();
      }
    }
    return given;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
