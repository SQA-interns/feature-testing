package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** US-002 student registration: AC-002-01 … AC-002-06 (API level, reCAPTCHA in test mode). */
class StudentRegistrationAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  @DisplayName("AC-002-01 a valid student submission is accepted (201) with the new id")
  void ac_002_01_validStudentSubmissionIsAccepted() throws Exception {
    String id = createdId(postRegistration(validStudent()));

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("type")).isEqualTo("STUDENT");
    assertThat(row.get("study_institution")).isEqualTo("University of Ljubljana");
    assertThat(row.get("study_programme")).isEqualTo("Computer Science");
    assertThat(row.get("student_id")).isEqualTo("63210001");
    assertThat(optionIds(id)).containsExactly("ev-student", "ws-ai");
  }

  @Test
  @DisplayName("AC-002-01 the organization field of a student submission is ignored")
  void ac_002_01_organizationOfStudentIsIgnored() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("organization", "Should be ignored");

    String id = createdId(postRegistration(body));
    assertThat(registrationRow(id).get("organization")).isNull();
  }

  static Stream<Arguments> missingStudentFields() {
    List<Arguments> cases = new ArrayList<>();
    for (String field :
        List.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")) {
      for (String variant : List.of("missing", "null", "empty", "whitespace")) {
        cases.add(Arguments.of(field, variant));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "AC-002-02 {0} {1} is rejected with an error for {0}")
  @MethodSource("missingStudentFields")
  void ac_002_02_missingRequiredFieldIsRejected(String field, String variant) throws Exception {
    Map<String, Object> body = validStudent();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "null" -> body.put(field, null);
      case "empty" -> body.put(field, "");
      default -> body.put(field, "  \t  ");
    }

    assertThat(errorFields(postRegistration(body))).contains(field);
    assertNothingStored();
  }

  @ParameterizedTest(name = "AC-002-03 email \"{0}\" is rejected with an error for email")
  @ValueSource(strings = {"luka.horvat", "luka@", "@student.example.org", "luka horvat@x.org"})
  void ac_002_03_invalidEmailIsRejected(String email) throws Exception {
    Map<String, Object> body = validStudent();
    body.put("email", email);

    assertThat(errorFields(postRegistration(body))).contains("email");
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-002-04 an option not available to students is rejected with an options error")
  void ac_002_04_optionNotAvailableToStudentsIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("options", List.of("ws-ai", "ws-pro"));

    assertThat(errorFields(postRegistration(body))).contains("options");
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-002-05 a student submission without the mandatory consent is rejected")
  void ac_002_05_mandatoryConsentMissingIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.put("consents", List.of("photos"));

    assertThat(errorFields(postRegistration(body))).contains("consents");
    assertNothingStored();
  }

  @ParameterizedTest(name = "AC-002-06 reCAPTCHA token \"{0}\" is rejected and nothing is stored")
  @ValueSource(strings = {"", "wrong-token"})
  void ac_002_06_badCaptchaTokenIsRejected(String token) throws Exception {
    Map<String, Object> body = validStudent();
    body.put("captchaToken", token);

    assertThat(errorFields(postRegistration(body))).contains("captchaToken");
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-002-06 a missing reCAPTCHA token is rejected and nothing is stored")
  void ac_002_06_missingCaptchaTokenIsRejected() throws Exception {
    Map<String, Object> body = validStudent();
    body.remove("captchaToken");

    assertThat(errorFields(postRegistration(body))).contains("captchaToken");
    assertNothingStored();
  }
}
