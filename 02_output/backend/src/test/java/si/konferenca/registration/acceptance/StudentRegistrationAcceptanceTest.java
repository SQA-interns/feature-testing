package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-002 Student registration, through POST /api/registrations. */
class StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  @Test
  @DisplayName("AC-002-01 valid student submission is accepted with a new id")
  void ac_002_01_validStudentSubmissionIsAcceptedWithNewId() {
    HttpResponse<String> response = postRegistration(Submissions.validStudent());

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    UUID id = UUID.fromString(json(response).path("id").asString());
    assertThat(queryLong("SELECT count(*) FROM registration WHERE id = ? AND type = 'STUDENT'", id))
        .isEqualTo(1);
  }

  static Stream<Arguments> missingStudentFields() {
    return Stream.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, "empty"),
                    Arguments.of(field, "whitespace")));
  }

  @ParameterizedTest(name = "AC-002-02 {0} {1}")
  @MethodSource("missingStudentFields")
  void ac_002_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String variant) {
    Map<String, Object> body = Submissions.validStudent();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "empty" -> body.put(field, "");
      default -> body.put(field, "   ");
    }

    assertRejected(postRegistration(body), field);
  }

  @ParameterizedTest(name = "AC-002-03 email \"{0}\"")
  @ValueSource(
      strings = {"luka", "luka@", "@student.example.org", "luka kranjc@x.org", "luka@student"})
  void ac_002_03_invalidEmailFormatIsRejected(String email) {
    Map<String, Object> body = Submissions.validStudent();
    body.put("email", email);

    assertRejected(postRegistration(body), "email");
  }

  @Test
  @DisplayName("AC-002-04 active option not available to students is rejected")
  void ac_002_04_optionNotAvailableToStudentsIsRejected() {
    Map<String, Object> body = Submissions.validStudent();
    body.put("optionIds", List.of("ws-testing", "ws-industry"));

    assertRejected(postRegistration(body), "optionIds");
  }

  static Stream<Arguments> missingMandatoryConsent() {
    return Stream.of(
        Arguments.of("consents absent", null),
        Arguments.of("no consent", List.of()),
        Arguments.of("only the optional consent", List.of("photos")));
  }

  @ParameterizedTest(name = "AC-002-05 {0}")
  @MethodSource("missingMandatoryConsent")
  void ac_002_05_missingMandatoryConsentIsRejected(String variant, List<String> consents) {
    Map<String, Object> body = Submissions.validStudent();
    if (consents == null) {
      body.remove("consents");
    } else {
      body.put("consents", consents);
    }

    assertRejected(postRegistration(body), "consents.privacy");
  }

  static Stream<Arguments> badCaptchaTokens() {
    return Stream.of(
        Arguments.of("missing", null),
        Arguments.of("blank", "   "),
        Arguments.of("rejected by the verifier", "test-fail"));
  }

  @ParameterizedTest(name = "AC-002-06 token {0}")
  @MethodSource("badCaptchaTokens")
  void ac_002_06_missingOrRejectedCaptchaTokenIsRejectedAndNothingStored(
      String variant, String token) {
    Map<String, Object> body = Submissions.validStudent();
    if (token == null) {
      body.remove("recaptchaToken");
    } else {
      body.put("recaptchaToken", token);
    }

    assertRejected(postRegistration(body), "recaptchaToken");
  }
}
