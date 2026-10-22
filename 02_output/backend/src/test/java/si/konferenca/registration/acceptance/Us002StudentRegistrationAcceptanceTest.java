package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.node.ObjectNode;

/** US-002 Student registration (AC-002-01..06; AC-002-07 is a frontend test). */
class Us002StudentRegistrationAcceptanceTest extends AcceptanceTestBase {

  @DisplayName("AC-002-01")
  @Test
  void ac_002_01_validStudentSubmissionIsAcceptedWithNewId() {
    ObjectNode body = options(validStudent(), "ws-ai", "ev-gala", "meal-lunch", "other-tour");

    HttpResponse<byte[]> response = postRegistration(body);

    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
    assertThat(json(response).path("id").asString()).isNotBlank();
  }

  @DisplayName("AC-002-01")
  @Test
  void ac_002_01_organizationOfStudentIsIgnored() {
    ObjectNode body = validStudent();
    body.put("organization", "Ignored Ltd.");

    HttpResponse<byte[]> response = postRegistration(body);

    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
  }

  static Stream<Arguments> requiredStudentFields() {
    return Stream.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, "empty"),
                    Arguments.of(field, "whitespace")));
  }

  @DisplayName("AC-002-02")
  @ParameterizedTest(name = "AC-002-02 {0} {1}")
  @MethodSource("requiredStudentFields")
  void ac_002_02_missingRequiredFieldIsRejectedWithFieldError(String field, String variant) {
    ObjectNode body = validStudent();
    String email = body.path("email").asString();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "empty" -> body.put(field, "");
      default -> body.put(field, "  ");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, field);
    assertNothingCreated(before, email);
  }

  @DisplayName("AC-002-03")
  @ParameterizedTest(name = "AC-002-03 email \"{0}\"")
  @ValueSource(strings = {"luka.example.org", "luka@", "@uni-lj.si", "luka kranjc@uni-lj.si"})
  void ac_002_03_invalidEmailFormatIsRejectedWithEmailError(String invalidEmail) {
    ObjectNode body = validStudent();
    body.put("email", invalidEmail);
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "email");
    assertThat(registrationRowCount()).isEqualTo(before.rows());
    assertThat(jsonCopyCount()).isEqualTo(before.jsonCopies());
  }

  @DisplayName("AC-002-04")
  @Test
  void ac_002_04_optionNotAvailableToStudentsIsRejected() {
    ObjectNode body = options(validStudent(), "ws-ai", "ws-cloud");
    String email = body.path("email").asString();
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "options");
    assertNothingCreated(before, email);
  }

  @DisplayName("AC-002-04")
  @Test
  void ac_002_04_sameOptionIsAcceptedForExternalParticipant() {
    HttpResponse<byte[]> response = postRegistration(options(validExternal(), "ws-cloud"));

    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
  }

  @DisplayName("AC-002-05")
  @ParameterizedTest(name = "AC-002-05 consents {0}")
  @ValueSource(strings = {"empty", "missing"})
  void ac_002_05_missingMandatoryConsentIsRejectedWithConsentError(String variant) {
    ObjectNode body = validStudent();
    String email = body.path("email").asString();
    if (variant.equals("empty")) {
      body.putArray("consents");
    } else {
      body.remove("consents");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "consents");
    assertNothingCreated(before, email);
  }

  @DisplayName("AC-002-06")
  @ParameterizedTest(name = "AC-002-06 captcha {0}")
  @ValueSource(strings = {"missing", "rejected"})
  void ac_002_06_missingOrRejectedCaptchaIsRejectedAndNothingStored(String variant) {
    ObjectNode body = validStudent();
    String email = body.path("email").asString();
    if (variant.equals("missing")) {
      body.remove("captchaToken");
    } else {
      body.put("captchaToken", "TEST-PASS");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "captchaToken");
    assertNothingCreated(before, email);
  }
}
