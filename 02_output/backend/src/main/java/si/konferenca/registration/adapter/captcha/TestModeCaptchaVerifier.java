package si.konferenca.registration.adapter.captcha;

import si.konferenca.registration.application.port.CaptchaVerifier;

/** Deterministic test mode (SR-02): no network call; only {@value #PASS} is accepted. */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  /** The one token test mode accepts. */
  public static final String PASS = "test-pass";

  @Override
  public boolean verify(String token) {
    return PASS.equals(token);
  }
}
