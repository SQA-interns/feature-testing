package si.konferenca.registration.application;

/** Port: verifies a reCAPTCHA token on the trusted side (SR-01). */
public interface CaptchaVerifier {

  boolean verify(String token);
}
