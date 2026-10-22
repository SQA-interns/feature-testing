package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The registration rules V-01 … V-06 of {@code docs/02_specification.md} 4.2 (BR-01 … BR-05, SR-04,
 * OQ-01, OQ-04). Collects every error, one per field.
 */
public final class RegistrationValidator {

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int INSTITUTION_MAX = 200;
  static final int STUDENT_ID_MAX = 50;
  static final int OPTIONS_MAX = 50;

  static final String REQUIRED = "This field is required.";
  static final String INVALID_EMAIL = "Enter a valid email address.";

  private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

  private final ConferenceCatalogue catalogue;

  public RegistrationValidator(ConferenceCatalogue catalogue) {
    this.catalogue = catalogue;
  }

  /** The outcome: errors, or (when there are none) the normalised participant and choices. */
  public record Result(
      List<FieldError> errors,
      Participant participant,
      List<String> options,
      List<String> consents) {
    public boolean valid() {
      return errors.isEmpty();
    }
  }

  public Result validate(RegistrationForm form) {
    List<FieldError> errors = new ArrayList<>();
    Optional<RegistrationType> type = RegistrationType.parse(strip(form.type()));
    if (type.isEmpty()) {
      errors.add(new FieldError("type", "Choose external participant or student."));
    }
    String firstName = required(errors, "firstName", form.firstName(), NAME_MAX);
    String lastName = required(errors, "lastName", form.lastName(), NAME_MAX);
    String email = email(errors, form.email());
    String organization = null;
    String studyInstitution = null;
    String studyProgramme = null;
    String studentId = null;
    if (type.orElse(null) == RegistrationType.EXTERNAL) {
      organization = required(errors, "organization", form.organization(), INSTITUTION_MAX);
    } else if (type.orElse(null) == RegistrationType.STUDENT) {
      studyInstitution =
          required(errors, "studyInstitution", form.studyInstitution(), INSTITUTION_MAX);
      studyProgramme = required(errors, "studyProgramme", form.studyProgramme(), INSTITUTION_MAX);
      studentId = required(errors, "studentId", form.studentId(), STUDENT_ID_MAX);
    }
    List<String> options = options(errors, form.options(), type);
    List<String> consents = consents(errors, form.consents());
    Participant participant =
        errors.isEmpty()
            ? new Participant(
                type.orElseThrow(),
                firstName,
                lastName,
                email,
                organization,
                studyInstitution,
                studyProgramme,
                studentId)
            : null;
    return new Result(List.copyOf(errors), participant, options, consents);
  }

  private static String strip(String value) {
    return value == null ? null : value.strip();
  }

  private static String required(List<FieldError> errors, String field, String raw, int max) {
    String value = strip(raw);
    if (value == null || value.isEmpty()) {
      errors.add(new FieldError(field, REQUIRED));
      return null;
    }
    if (value.length() > max) {
      errors.add(new FieldError(field, "Use at most " + max + " characters."));
      return null;
    }
    return value;
  }

  private static String email(List<FieldError> errors, String raw) {
    String value = required(errors, "email", raw, EMAIL_MAX);
    if (value == null) {
      return null;
    }
    if (!EMAIL.matcher(value).matches() || value.chars().anyMatch(Character::isISOControl)) {
      errors.add(new FieldError("email", INVALID_EMAIL));
      return null;
    }
    return value;
  }

  private List<String> options(
      List<FieldError> errors, List<String> raw, Optional<RegistrationType> type) {
    List<String> selected = raw == null ? List.of() : raw;
    if (selected.size() > OPTIONS_MAX) {
      errors.add(new FieldError("options", "Select at most " + OPTIONS_MAX + " options."));
      return List.of();
    }
    Set<String> seen = new HashSet<>();
    for (String id : selected) {
      Optional<ConferenceOption> option = id == null ? Optional.empty() : catalogue.option(id);
      String problem;
      if (option.isEmpty() || !option.get().active()) {
        problem = "An option is not offered.";
      } else if (type.isPresent() && !option.get().isOfferedTo(type.get())) {
        problem = "An option is not available for this registration type.";
      } else if (!seen.add(id)) {
        problem = "An option is selected more than once.";
      } else {
        continue;
      }
      errors.add(new FieldError("options", problem));
      return List.of();
    }
    return List.copyOf(selected);
  }

  private List<String> consents(List<FieldError> errors, List<String> raw) {
    List<String> given = raw == null ? List.of() : raw;
    Set<String> unique = new HashSet<>();
    for (String id : given) {
      if (id == null || !catalogue.hasConsent(id) || !unique.add(id)) {
        errors.add(new FieldError("consents", "A consent is not valid."));
        return List.of();
      }
    }
    for (Consent consent : catalogue.mandatoryConsents()) {
      if (!unique.contains(consent.id())) {
        errors.add(new FieldError("consents", "This consent is required."));
        return List.of();
      }
    }
    return List.copyOf(given);
  }
}
