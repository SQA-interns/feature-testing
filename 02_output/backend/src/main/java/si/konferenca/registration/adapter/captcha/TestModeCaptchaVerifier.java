package si.konferenca.registration.adapter.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/** Deterministic test mode: only {@code test-pass} is accepted, nothing leaves the machine. */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String TEST_TOKEN = "test-pass";

  @Override
  public boolean verify(String token) {
    return TEST_TOKEN.equals(token);
  }
}
