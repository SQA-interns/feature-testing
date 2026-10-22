package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validStudent;
import static si.konferenca.registration.acceptance.support.Submissions.with;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.CaptchaVerifierStubAcceptanceTest;

/** US-002 with the production reCAPTCHA code path against a mocked verify endpoint (DoD-P05). */
class Us002CaptchaVerificationAcceptanceTest extends CaptchaVerifierStubAcceptanceTest {

  @BeforeEach
  void forgetCalls() {
    VERIFY_CALLS.clear();
  }

  @Test
  @DisplayName("AC-002-06 a student token the verify endpoint rejects is rejected")
  void ac002_06_tokenRejectedByVerifierIsRejected() {
    assertRejected(
        register(with(validStudent(), "captchaToken", "token-the-verifier-rejects")),
        "captchaToken");
    assertThat(VERIFY_CALLS).hasSize(1);
  }

  @Test
  @DisplayName("AC-002-06 a student token the verify endpoint accepts lets the submission through")
  void ac002_06_tokenAcceptedByVerifierIsAccepted() {
    registerAccepted(with(validStudent(), "captchaToken", GOOD_TOKEN));
    assertThat(db.countRegistrations()).isEqualTo(1);
  }
}
