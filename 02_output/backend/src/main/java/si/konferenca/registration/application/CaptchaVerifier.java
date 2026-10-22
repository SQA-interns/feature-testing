package si.konferenca.registration.application;

/** Port: verifies a reCAPTCHA token on the trusted side (SR-01). */
public interface CaptchaVerifier {

  /** True only when the token is accepted; any failure to verify counts as rejection. */
  boolean verify(String token);
}
