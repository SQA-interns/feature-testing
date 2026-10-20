package si.konferenca.registration.adapter.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/**
 * Deterministic reCAPTCHA test mode (api-contract.md): only {@code test-pass} is accepted. Enabled
 * only by configuration, never in production (SR-02).
 */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  public static final String TEST_TOKEN = "test-pass";

  @Override
  public boolean verify(String token) {
    return TEST_TOKEN.equals(token);
  }
}
