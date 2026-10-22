package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
import static si.konferenca.registration.acceptance.support.Submissions.with;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.CaptchaVerifierStubAcceptanceTest;

/** US-001 with the production reCAPTCHA code path against a mocked verify endpoint (DoD-P05). */
class Us001CaptchaVerificationAcceptanceTest extends CaptchaVerifierStubAcceptanceTest {

  @BeforeEach
  void forgetCalls() {
    VERIFY_CALLS.clear();
  }

  @Test
  @DisplayName("AC-001-07 a token the verify endpoint rejects is rejected and nothing is stored")
  void ac001_07_tokenRejectedByVerifierIsRejected() {
    assertRejected(
        register(with(validExternal(), "captchaToken", "token-the-verifier-rejects")),
        "captchaToken");

    assertThat(VERIFY_CALLS).hasSize(1);
    assertThat(VERIFY_CALLS.getFirst())
        .containsEntry("secret", SECRET)
        .containsEntry("response", "token-the-verifier-rejects");
  }

  @Test
  @DisplayName("AC-001-07 the test-mode token is rejected when test mode is off")
  void ac001_07_testTokenIsRejectedWithoutTestMode() {
    assertRejected(register(with(validExternal(), "captchaToken", "test-pass")), "captchaToken");
  }

  @Test
  @DisplayName("AC-001-07 a token the verify endpoint accepts lets the submission through")
  void ac001_07_tokenAcceptedByVerifierIsAccepted() {
    registerAccepted(with(validExternal(), "captchaToken", GOOD_TOKEN));

    assertThat(db.countRegistrations()).isEqualTo(1);
    assertThat(VERIFY_CALLS).hasSize(1);
    assertThat(VERIFY_CALLS.getFirst()).containsEntry("response", GOOD_TOKEN);
  }
}
