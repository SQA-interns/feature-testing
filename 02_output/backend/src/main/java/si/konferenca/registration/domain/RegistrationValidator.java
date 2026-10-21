package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates a submission on the trusted side (SB-01): required fields after trimming (BR-02), email
 * format and lengths (BR-03, D-12), options against the active configured set (BR-04, SR-04, OQ-01,
 * OQ-04), consents (BR-05, D-11) and the presence of a captcha token.
 */
public final class RegistrationValidator {

  /** {@code EMAIL_PATTERN} of {@code docs/02_specification.md} section 4. */
  public static final Pattern EMAIL =
      Pattern.compile(
          "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
              + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

  public static final int MAX_TEXT_LENGTH = 200;
  public static final int MAX_EMAIL_LENGTH = 254;

  public static final String TYPE_INVALID = "Registration type must be EXTERNAL or STUDENT.";
  public static final String REQUIRED = "This field is required.";
  public static final String TOO_LONG = "This value is too long.";
  public static final String EMAIL_INVALID = "Enter a valid email address.";

  private final ConferenceCatalog catalog;

  public RegistrationValidator(ConferenceCatalog catalog) {
    this.catalog = catalog;
  }

  public ValidationResult validate(RegistrationInput input) {
    Optional<RegistrationType> parsed = type(input.type());
    if (parsed.isEmpty()) {
      return ValidationResult.rejected(List.of(new FieldError("type", TYPE_INVALID)));
    }
    RegistrationType type = parsed.get();
    List<FieldError> errors = new ArrayList<>();
    String firstName = text(errors, "firstName", input.firstName(), MAX_TEXT_LENGTH);
    String lastName = text(errors, "lastName", input.lastName(), MAX_TEXT_LENGTH);
    String email = email(errors, input.email());
    String organization = null;
    String studyInstitution = null;
    String studyProgramme = null;
    String studentId = null;
    if (type == RegistrationType.EXTERNAL) {
      organization = text(errors, "organization", input.organization(), MAX_TEXT_LENGTH);
    } else {
      studyInstitution =
          text(errors, "studyInstitution", input.studyInstitution(), MAX_TEXT_LENGTH);
      studyProgramme = text(errors, "studyProgramme", input.studyProgramme(), MAX_TEXT_LENGTH);
      studentId = text(errors, "studentId", input.studentId(), MAX_TEXT_LENGTH);
    }
    List<String> options = options(errors, type, input.options());
    List<String> consents = consents(errors, input.consents());
    if (input.captchaToken() == null || input.captchaToken().isBlank()) {
      errors.add(new FieldError("captchaToken", REQUIRED));
    }
    if (!errors.isEmpty()) {
      return ValidationResult.rejected(errors);
    }
    return ValidationResult.accepted(
        new ValidRegistration(
            type,
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

  private static Optional<RegistrationType> type(String value) {
    for (RegistrationType type : RegistrationType.values()) {
      if (type.name().equals(value)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  private static String text(List<FieldError> errors, String field, String value, int max) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty()) {
      errors.add(new FieldError(field, REQUIRED));
      return null;
    }
    if (trimmed.codePointCount(0, trimmed.length()) > max) {
      errors.add(new FieldError(field, TOO_LONG));
      return null;
    }
    return trimmed;
  }

  private static String email(List<FieldError> errors, String value) {
    int before = errors.size();
    String email = text(errors, "email", value, MAX_EMAIL_LENGTH);
    if (errors.size() == before && !EMAIL.matcher(email).matches()) {
      errors.add(new FieldError("email", EMAIL_INVALID));
      return null;
    }
    return email;
  }

  private List<String> options(List<FieldError> errors, RegistrationType type, List<String> ids) {
    if (ids == null) {
      return List.of();
    }
    Set<String> seen = new HashSet<>();
    for (String id : ids) {
      if (!seen.add(id)) {
        errors.add(new FieldError("options", "Option '" + id + "' is selected more than once."));
        return List.of();
      }
      boolean available =
          id != null
              && catalog.option(id).filter(o -> o.active() && o.isAvailableTo(type)).isPresent();
      if (!available) {
        errors.add(new FieldError("options", "Option '" + id + "' is not available."));
        return List.of();
      }
    }
    return List.copyOf(ids);
  }

  private List<String> consents(List<FieldError> errors, List<String> ids) {
    Set<String> given = new LinkedHashSet<>();
    if (ids != null) {
      for (String id : ids) {
        if (id == null || catalog.consent(id).isEmpty()) {
          errors.add(new FieldError("consents", "Consent '" + id + "' is unknown."));
          return List.of();
        }
        given.add(id);
      }
    }
    for (ConsentDefinition consent : catalog.consents()) {
      if (consent.mandatory() && !given.contains(consent.id())) {
        errors.add(new FieldError("consents", "Consent '" + consent.id() + "' is required."));
        return List.of();
      }
    }
    return List.copyOf(given);
  }
}
