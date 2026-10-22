package si.konferenca.registration.application;

/** The captcha verification service could not answer; nothing is stored. */
public class CaptchaUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CaptchaUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
