package si.konferenca.registration.domain;

import java.util.List;

/**
 * The outcome of validating a submission: either errors, or the normalised type, participant,
 * options and consents.
 */
public record ValidatedSubmission(
    List<FieldError> errors,
    RegistrationType type,
    Participant participant,
    List<SelectedOption> options,
    List<ConsentDefinition> consents) {

  /** Copies the lists so the result cannot change afterwards. */
  public ValidatedSubmission {
    errors = List.copyOf(errors);
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  public boolean isValid() {
    return errors.isEmpty();
  }
}
