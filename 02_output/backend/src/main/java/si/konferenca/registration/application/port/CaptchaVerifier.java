package si.konferenca.registration.application.port;

/** Verifies an anti-automation token on the backend (SR-01). */
public interface CaptchaVerifier {

  /** Whether the token is accepted; any failure to verify counts as a rejection. */
  boolean verify(String token);
}
