package si.konferenca.registration.domain;

import java.util.List;

/** A submission that breaks a rule; nothing is stored. */
public class RegistrationRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public RegistrationRejectedException(List<FieldError> errors) {
    super("registration rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
