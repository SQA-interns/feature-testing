package si.konferenca.registration.application;

/** A registration could not be stored; nothing of it remains (AC-005-03). */
public class StorageException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StorageException(Throwable cause) {
    super("registration could not be stored", cause);
  }
}
