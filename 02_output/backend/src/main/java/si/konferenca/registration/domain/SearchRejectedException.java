package si.konferenca.registration.domain;

import java.util.List;

/** An organizer search with an invalid text or filter; no registration data is returned. */
public class SearchRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public SearchRejectedException(List<FieldError> errors) {
    super("search rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
