package si.konferenca.registration.web;

import java.util.List;
import si.konferenca.registration.domain.FieldError;

/** Error bodies of the API: field errors for 400, a general message otherwise (SB-07). */
public final class ErrorResponses {

  private ErrorResponses() {}

  /** {"errors": [{"field", "message"}]} */
  public record FieldErrors(List<FieldError> errors) {}

  /** {"message"} without internal details. */
  public record General(String message) {}
}
