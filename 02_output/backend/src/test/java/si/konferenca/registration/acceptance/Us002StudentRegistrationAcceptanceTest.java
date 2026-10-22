package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validStudent;
import static si.konferenca.registration.acceptance.support.Submissions.with;

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
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.HttpApi;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;

/** US-002 Student registration, through {@code POST /api/registrations}. */
class Us002StudentRegistrationAcceptanceTest extends StandardAcceptanceTest {

  @Test
  @DisplayName("AC-002-01 a valid student submission is accepted with the new registration id")
  void ac002_01_validStudentSubmissionIsAccepted() {
    HttpApi.Response response = register(validStudent());

    assertThat(response.status()).as("response %s", response).isEqualTo(201);
    String id = response.json().path("id").asString();
    assertThat(id).isNotBlank();
    Database.Row row = db.row(UUID.fromString(id));
    assertThat(row.type()).isEqualTo("STUDENT");
    assertThat(row.studyInstitution()).isEqualTo("Univerza v Ljubljani");
    assertThat(row.studyProgramme()).isEqualTo("Computer Science");
    assertThat(row.studentId()).isEqualTo("63210001");
  }

  static Stream<Arguments> requiredStudentFieldVariants() {
    return Stream.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, "empty"),
                    Arguments.of(field, "whitespace")));
  }

  @ParameterizedTest(name = "AC-002-02 {0} {1} is rejected")
  @MethodSource("requiredStudentFieldVariants")
  void ac002_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String variant) {
    Map<String, Object> submission = validStudent();
    switch (variant) {
      case "missing" -> submission.remove(field);
      case "empty" -> submission.put(field, "");
      default -> submission.put(field, "   ");
    }

    assertRejected(register(submission), field);
  }

  @ParameterizedTest(name = "AC-002-03 email \"{0}\" is rejected")
  @ValueSource(strings = {"luka.horvat", "luka@", "@student.example.org", "luka horvat@x.org"})
  void ac002_03_invalidEmailIsRejected(String email) {
    assertRejected(register(with(validStudent(), "email", email)), "email");
  }

  @Test
  @DisplayName("AC-002-04 an option not available to students is rejected")
  void ac002_04_optionNotAvailableToStudentsIsRejected() {
    assertRejected(
        register(with(validStudent(), "options", List.of("ws-ai", "ws-leadership"))), "options");
  }

  @Test
  @DisplayName("AC-002-05 a student submission without the mandatory consent is rejected")
  void ac002_05_missingMandatoryConsentIsRejected() {
    assertRejected(register(with(validStudent(), "consents", List.of())), "consents");
  }

  @Test
  @DisplayName("AC-002-06 a missing or rejected reCAPTCHA token is rejected for a student")
  void ac002_06_missingOrRejectedCaptchaTokenIsRejected() {
    Map<String, Object> submission = validStudent();
    submission.remove("captchaToken");
    assertRejected(register(submission), "captchaToken");

    assertRejected(register(with(validStudent(), "captchaToken", "wrong-token")), "captchaToken");
  }
}
