package si.konferenca.registration.config;

/** The conference configuration does not match its schema; the backend does not start. */
public class InvalidConferenceConfigException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public InvalidConferenceConfigException(String message) {
    super("Invalid conference configuration: " + message);
  }

  public InvalidConferenceConfigException(String message, Throwable cause) {
    super("Invalid conference configuration: " + message, cause);
  }
}
