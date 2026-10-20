package si.konferenca.registration.application;

/** Port: verification of a reCAPTCHA response token on the trusted side (SR-01). */
public interface CaptchaVerifier {

  boolean verify(String token);
}
