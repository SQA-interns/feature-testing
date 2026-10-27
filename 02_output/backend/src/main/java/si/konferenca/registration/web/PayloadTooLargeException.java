package si.konferenca.registration.web;

import java.io.IOException;

/** A request body went over the size limit while it was read (SR-03). */
public class PayloadTooLargeException extends IOException {

  private static final long serialVersionUID = 1L;

  public PayloadTooLargeException() {
    super("request body too large");
  }
}
