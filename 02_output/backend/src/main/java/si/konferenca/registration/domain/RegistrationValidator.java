package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Validates a submission against BR-01..BR-05 and the catalog (specification S4.1..S4.4). */
public final class RegistrationValidator {

  /** Maximum length of names, organization, study institution and programme (D-14). */
  public static final int TEXT_MAX = 200;

  /** Maximum length of an email address (D-14). */
  public static final int EMAIL_MAX = 254;

  /** Maximum length of a student ID (D-14). */
  public static final int STUDENT_ID_MAX = 50;

  private final ConferenceCatalog catalog;

  public RegistrationValidator(ConferenceCatalog catalog) {
    this.catalog = catalog;
  }

  /** Validates every field and collects all errors; nothing is cut short. */
  public ValidatedSubmission validate(Submission submission) {
    List<FieldError> errors = new ArrayList<>();
    RegistrationType type = parseType(submission.type());
    if (type == null) {
      errors.add(new FieldError("type", ErrorCode.REQUIRED));
    }
    String firstName = required("firstName", submission.firstName(), TEXT_MAX, errors);
    String lastName = required("lastName", submission.lastName(), TEXT_MAX, errors);
    String email = required("email", submission.email(), EMAIL_MAX, errors);
    if (email != null && !EmailAddress.isValid(email)) {
      errors.add(new FieldError("email", ErrorCode.INVALID_FORMAT));
    }
    Participant participant = null;
    if (type == RegistrationType.EXTERNAL) {
      String organization = required("organization", submission.organization(), TEXT_MAX, errors);
      notAllowed("studyInstitution", submission.studyInstitution(), errors);
      notAllowed("studyProgramme", submission.studyProgramme(), errors);
      notAllowed("studentId", submission.studentId(), errors);
      participant = new Participant(firstName, lastName, email, organization, null, null, null);
    } else if (type == RegistrationType.STUDENT) {
      String institution =
          required("studyInstitution", submission.studyInstitution(), TEXT_MAX, errors);
      String programme = required("studyProgramme", submission.studyProgramme(), TEXT_MAX, errors);
      String studentId = required("studentId", submission.studentId(), STUDENT_ID_MAX, errors);
      notAllowed("organization", submission.organization(), errors);
      participant =
          new Participant(firstName, lastName, email, null, institution, programme, studentId);
    }
    List<SelectedOption> options = options(submission.optionIds(), type, errors);
    List<ConsentDefinition> consents = consents(submission.consents(), errors);
    if (isBlank(submission.recaptchaToken())) {
      errors.add(new FieldError("recaptchaToken", ErrorCode.CAPTCHA_FAILED));
    }
    return new ValidatedSubmission(errors, type, participant, options, consents);
  }

  private static RegistrationType parseType(String value) {
    if (value == null) {
      return null;
    }
    String name = value.strip();
    return Arrays.stream(RegistrationType.values())
        .filter(t -> t.name().equals(name))
        .findFirst()
        .orElse(null);
  }

  private static String required(String field, String value, int max, List<FieldError> errors) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty()) {
      errors.add(new FieldError(field, ErrorCode.REQUIRED));
      return null;
    }
    if (trimmed.codePointCount(0, trimmed.length()) > max) {
      errors.add(new FieldError(field, ErrorCode.TOO_LONG));
      return null;
    }
    return trimmed;
  }

  private static void notAllowed(String field, String value, List<FieldError> errors) {
    if (!isBlank(value)) {
      errors.add(new FieldError(field, ErrorCode.NOT_ALLOWED));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private List<SelectedOption> options(
      List<String> ids, RegistrationType type, List<FieldError> errors) {
    List<SelectedOption> selected = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (String id : ids) {
      if (id != null && !seen.add(id)) {
        errors.add(new FieldError("optionIds", ErrorCode.DUPLICATE_OPTION));
        continue;
      }
      Optional<ConferenceOption> found = id == null ? Optional.empty() : catalog.option(id);
      ErrorCode problem = optionProblem(found, type);
      if (problem == null) {
        ConferenceOption option = found.orElseThrow();
        selected.add(new SelectedOption(option.id(), option.name(), option.category()));
      } else {
        errors.add(new FieldError("optionIds", problem));
      }
    }
    return selected;
  }

  private static ErrorCode optionProblem(Optional<ConferenceOption> found, RegistrationType type) {
    if (found.isEmpty()) {
      return ErrorCode.UNKNOWN_OPTION;
    }
    ConferenceOption option = found.get();
    if (!option.active()) {
      return ErrorCode.INACTIVE_OPTION;
    }
    if (type != null && !option.isAvailableTo(type)) {
      return ErrorCode.OPTION_NOT_AVAILABLE;
    }
    return null;
  }

  private List<ConsentDefinition> consents(List<String> ids, List<FieldError> errors) {
    Set<String> given = new HashSet<>();
    for (String id : ids) {
      if (id == null || catalog.consent(id).isEmpty()) {
        errors.add(new FieldError("consents." + id, ErrorCode.UNKNOWN_CONSENT));
      } else {
        given.add(id);
      }
    }
    List<ConsentDefinition> result = new ArrayList<>();
    for (ConsentDefinition consent : catalog.consents()) {
      if (given.contains(consent.id())) {
        result.add(consent);
      } else if (consent.mandatory()) {
        errors.add(new FieldError("consents." + consent.id(), ErrorCode.CONSENT_REQUIRED));
      }
    }
    return result;
  }
}
