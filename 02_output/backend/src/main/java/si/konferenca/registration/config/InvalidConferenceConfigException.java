package si.konferenca.registration.config;

import java.nio.file.Path;

/** The conference configuration file breaks its schema; the application does not start. */
public class InvalidConferenceConfigException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidConferenceConfigException(Path file, String problem) {
    super("conference configuration " + file + ": " + problem);
  }
}
