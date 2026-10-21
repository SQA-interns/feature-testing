package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.FieldError;

/** The submission is invalid; nothing was stored or sent. */
public class RegistrationRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public RegistrationRejectedException(List<FieldError> errors) {
    super("Registration rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
