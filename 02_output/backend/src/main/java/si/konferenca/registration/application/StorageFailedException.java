package si.konferenca.registration.application;

/** The registration could not be stored completely; nothing remains (AC-005-03). */
public class StorageFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StorageFailedException(Throwable cause) {
    super("Registration could not be stored", cause);
  }
}
