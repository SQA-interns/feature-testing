package si.konferenca.registration.adapter.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/**
 * Deterministic test mode ({@code CAPTCHA_TEST_MODE=true}, never in production, SR-02): only
 * {@value #TEST_TOKEN} is accepted and Google is never called.
 */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String TEST_TOKEN = "test-pass";

  @Override
  public boolean verify(String token) {
    return TEST_TOKEN.equals(token);
  }
}
