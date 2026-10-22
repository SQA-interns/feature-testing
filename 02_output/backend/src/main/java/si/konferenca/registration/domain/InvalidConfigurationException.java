package si.konferenca.registration.domain;

/** The conference configuration breaks a rule; the backend must not start with it. */
public class InvalidConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidConfigurationException(String message) {
    super("Invalid conference configuration: " + message);
  }
}
