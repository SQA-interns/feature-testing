package si.konferenca.registration.application;

/** Port: verification of a reCAPTCHA token on the server (SR-01). */
public interface CaptchaVerifier {

  /**
   * Whether the token is accepted.
   *
   * @throws CaptchaUnavailableException if the verification service cannot answer
   */
  boolean verify(String token);
}
