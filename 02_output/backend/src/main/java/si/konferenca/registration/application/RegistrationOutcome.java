package si.konferenca.registration.application;

import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.FieldError;

/** The answer to a submission: accepted with its id, or rejected with field errors. */
public sealed interface RegistrationOutcome {

  /** Stored in the database and as a JSON copy. */
  record Accepted(UUID id) implements RegistrationOutcome {}

  /** Not stored; one error per invalid field. */
  record Rejected(List<FieldError> errors) implements RegistrationOutcome {
    public Rejected {
      errors = List.copyOf(errors);
    }
  }
}
