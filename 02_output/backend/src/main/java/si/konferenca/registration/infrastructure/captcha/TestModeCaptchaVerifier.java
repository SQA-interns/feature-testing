package si.konferenca.registration.infrastructure.captcha;

import si.konferenca.registration.application.CaptchaVerifier;

/**
 * Deterministic captcha for local and test environments (api-contract.md): only {@code test-pass}
 * is accepted; no network call.
 */
public class TestModeCaptchaVerifier implements CaptchaVerifier {

  static final String ACCEPTED_TOKEN = "test-pass";

  @Override
  public boolean verify(String token) {
    return ACCEPTED_TOKEN.equals(token);
  }
}
