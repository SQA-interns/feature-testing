package si.konferenca.registration.domain;

import java.util.List;

/** Either a valid registration or the field errors that reject it. */
public record ValidationResult(ValidRegistration registration, List<FieldError> errors) {

  public ValidationResult {
    errors = List.copyOf(errors);
  }

  public static ValidationResult accepted(ValidRegistration registration) {
    return new ValidationResult(registration, List.of());
  }

  public static ValidationResult rejected(List<FieldError> errors) {
    return new ValidationResult(null, errors);
  }

  public boolean isValid() {
    return errors.isEmpty();
  }
}
