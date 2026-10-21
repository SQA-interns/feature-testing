package si.konferenca.registration.web;

import java.io.IOException;

/** A request body exceeded {@code app.request.max-bytes} while it was read (SR-03). */
public class PayloadTooLargeException extends IOException {

  private static final long serialVersionUID = 1L;

  public PayloadTooLargeException() {
    super("Request body too large");
  }
}
