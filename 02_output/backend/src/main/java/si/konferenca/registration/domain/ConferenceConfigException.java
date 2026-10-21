package si.konferenca.registration.domain;

/** The conference configuration is missing or invalid; the backend must not start (AC-003-03). */
public class ConferenceConfigException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConferenceConfigException(String message) {
    super(message);
  }

  public ConferenceConfigException(String message, Throwable cause) {
    super(message, cause);
  }
}
