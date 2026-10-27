package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.FakeSiteverify;
import si.konferenca.registration.acceptance.support.Submissions;

/**
 * The production reCAPTCHA path (test mode off) against a mocked siteverify endpoint: AC-001-07,
 * AC-002-06 and DoD-P05.
 */
class RecaptchaVerificationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(
        registry,
        Map.of(
            "RECAPTCHA_TEST_MODE",
            "false",
            "RECAPTCHA_SITE_KEY",
            FakeSiteverify.SITE_KEY,
            "RECAPTCHA_SECRET_KEY",
            FakeSiteverify.SECRET,
            "RECAPTCHA_VERIFY_URL",
            FakeSiteverify.instance().url()));
  }

  @Test
  @DisplayName("AC-001-07 a token the verifier accepts lets a valid submission through")
  void ac_001_07_tokenAcceptedByVerifierIsAccepted() {
    Map<String, Object> body = Submissions.validExternal();
    body.put("recaptchaToken", FakeSiteverify.GOOD_TOKEN);

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    assertThat(FakeSiteverify.instance().requests())
        .as("the backend verified the token with its secret")
        .anySatisfy(
            form ->
                assertThat(form)
                    .containsEntry("response", FakeSiteverify.GOOD_TOKEN)
                    .containsEntry("secret", FakeSiteverify.SECRET));
  }

  @ParameterizedTest(name = "AC-001-07 verifier rejects token \"{0}\"")
  @ValueSource(strings = {"bad-token", FakeSiteverify.SERVER_ERROR_TOKEN})
  void ac_001_07_tokenRejectedByVerifierIsRejected(String token) {
    Map<String, Object> body = Submissions.validExternal();
    body.put("recaptchaToken", token);

    assertRejected(postRegistration(body), "recaptchaToken");
    assertThat(FakeSiteverify.instance().requests())
        .as("the backend asked the verifier")
        .anySatisfy(form -> assertThat(form).containsEntry("response", token));
  }

  @Test
  @DisplayName("AC-002-06 student submission with a token the verifier rejects is rejected")
  void ac_002_06_studentTokenRejectedByVerifierIsRejected() {
    Map<String, Object> body = Submissions.validStudent();
    body.put("recaptchaToken", "bad-student-token");

    assertRejected(postRegistration(body), "recaptchaToken");
  }

  @Test
  @DisplayName("AC-001-07 the test-mode token is not accepted when test mode is off")
  void ac_001_07_testModeTokenIsRejectedWhenTestModeIsOff() {
    assertRejected(postRegistration(Submissions.validExternal()), "recaptchaToken");
  }
}
