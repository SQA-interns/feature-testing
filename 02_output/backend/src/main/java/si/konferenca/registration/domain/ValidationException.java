package si.konferenca.registration.domain;

import java.util.List;

/** A submission was rejected; one {@link FieldError} per invalid field. */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final List<FieldError> errors;

  public ValidationException(List<FieldError> errors) {
    super("registration rejected: " + errors.size() + " field error(s)");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
