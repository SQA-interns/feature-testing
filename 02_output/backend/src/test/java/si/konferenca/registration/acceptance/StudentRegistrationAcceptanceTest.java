package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.JsonCopies;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-002: student registration through {@code POST /api/registrations}. */
@AcceptanceTest
class StudentRegistrationAcceptanceTest {

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();
  private final JsonCopies copies = TestStack.jsonCopies();
  private final Mailpit mailpit = TestStack.mailpit();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_002_01_validStudentSubmissionIsAcceptedWithNewId() throws Exception {
    Map<String, Object> body = Submissions.validStudent();

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn();

    String id = Submissions.id(result);
    assertThat(id).isNotBlank();
    assertThat(db.registration(id))
        .containsEntry("type", "STUDENT")
        .containsEntry("email", (String) body.get("email"));
  }

  @Test
  void ac_002_01_organizationOfStudentSubmissionIsIgnored() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("organization", "Should be ignored d.o.o.");

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn();

    assertThat(db.registration(Submissions.id(result))).containsEntry("organization", null);
  }

  static Stream<Arguments> requiredFieldsAndBadValues() {
    return Stream.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")
        .flatMap(
            f ->
                Stream.of(Arguments.of(f, "missing"), Arguments.of(f, ""), Arguments.of(f, "   ")));
  }

  @ParameterizedTest(name = "{0} = [{1}]")
  @MethodSource("requiredFieldsAndBadValues")
  void ac_002_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String value)
      throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }
    long rowsBefore = "email".equals(field) ? db.countAll() : -1;

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains(field);
    if ("email".equals(field)) {
      assertThat(db.countAll()).isEqualTo(rowsBefore);
    }
    assertNothingStoredOrSent(body);
  }

  @ParameterizedTest
  @ValueSource(strings = {"luka", "luka@", "@student.uni-lj.si", "luka@@uni-lj.si", "lu ka@uni.si"})
  void ac_002_03_invalidEmailFormatIsRejected(String email) throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("email", email);

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("email");
    assertThat(db.countByEmail(email)).isZero();
  }

  @Test
  void ac_002_04_optionNotAvailableToStudentsIsRejected() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("options", List.of("ws-ai", "ev-tour"));

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("options");
    assertNothingStoredOrSent(body);
  }

  @Test
  void ac_002_04_sameOptionIsAcceptedForExternalParticipant() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("options", List.of("ws-ai", "ev-tour"));

    mvc.perform(Submissions.post(body)).andExpect(status().isCreated());
  }

  @ParameterizedTest(name = "consents {0}")
  @MethodSource("missingMandatoryConsent")
  void ac_002_05_missingMandatoryConsentIsRejected(List<String> consents) throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("consents", consents);

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("consents");
    assertNothingStoredOrSent(body);
  }

  static Stream<List<String>> missingMandatoryConsent() {
    return Stream.of(List.of(), List.of("photos"));
  }

  @ParameterizedTest(name = "token [{0}]")
  @ValueSource(strings = {"missing", "  ", "not-the-test-token"})
  void ac_002_06_missingOrRejectedCaptchaTokenIsRejected(String token) throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    if ("missing".equals(token)) {
      body.remove("captchaToken");
    } else {
      body.put("captchaToken", token);
    }

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("captchaToken");
    assertNothingStoredOrSent(body);
  }

  private void assertNothingStoredOrSent(Map<String, Object> body) throws InterruptedException {
    Object email = body.get("email");
    if (email == null || String.valueOf(email).isBlank()) {
      return;
    }
    String address = String.valueOf(email).strip();
    Thread.sleep(500);
    assertThat(db.countByEmail(address)).isZero();
    assertThat(copies.filesMentioning(address)).isEmpty();
    assertThat(mailpit.messagesMentioning(address)).isEmpty();
  }
}
