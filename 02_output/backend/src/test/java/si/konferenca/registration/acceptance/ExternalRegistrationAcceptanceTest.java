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

/** US-001: external participant registration through {@code POST /api/registrations}. */
@AcceptanceTest
class ExternalRegistrationAcceptanceTest {

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();
  private final JsonCopies copies = TestStack.jsonCopies();
  private final Mailpit mailpit = TestStack.mailpit();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_001_01_validExternalSubmissionIsAcceptedWithNewId() throws Exception {
    Map<String, Object> body = Submissions.validExternal();

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn();

    String id = Submissions.id(result);
    assertThat(id).isNotBlank();
    assertThat(db.registration(id)).containsEntry("email", (String) body.get("email"));
  }

  @Test
  void ac_001_01_validExternalSubmissionWithoutOptionsIsAccepted() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("options", List.of());

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn();

    assertThat(Submissions.id(result)).isNotBlank();
  }

  static Stream<Arguments> requiredFieldsAndBadValues() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            f ->
                Stream.of(
                    Arguments.of(f, "missing"), Arguments.of(f, ""), Arguments.of(f, "  \t ")));
  }

  @ParameterizedTest(name = "{0} = [{1}]")
  @MethodSource("requiredFieldsAndBadValues")
  void ac_001_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String value)
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
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

  @Test
  void ac_001_03_leadingAndTrailingSpacesAreRemovedBeforeStoring() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    body.put("firstName", "  Ana  ");
    body.put("lastName", "\tNovak ");
    body.put("email", "  " + email + "  ");
    body.put("organization", "   Example d.o.o.   ");

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn();

    Map<String, String> row = db.registration(Submissions.id(result));
    assertThat(row)
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", email)
        .containsEntry("organization", "Example d.o.o.");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana",
        "ana@",
        "@example.org",
        "ana@@example.org",
        "ana novak@example.org",
        "ana@exa mple.org"
      })
  void ac_001_04_invalidEmailFormatIsRejected(String email) throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("email", email);

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("email");
    assertThat(db.countByEmail(email)).isZero();
  }

  static Stream<Arguments> badOptionLists() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-ai", "ws-unknown")),
        Arguments.of("inactive", List.of("ws-old")),
        Arguments.of("listed twice", List.of("ws-ai", "ev-gala", "ws-ai")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("badOptionLists")
  void ac_001_05_unknownInactiveOrDuplicateOptionIsRejected(String reason, List<String> options)
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("options", options);

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("options");
    assertNothingStoredOrSent(body);
  }

  static Stream<Arguments> missingMandatoryConsent() {
    return Stream.of(
        Arguments.of("empty list", List.of()),
        Arguments.of("only the optional consent", List.of("photos")),
        Arguments.of("absent", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("missingMandatoryConsent")
  void ac_001_06_missingMandatoryConsentIsRejected(String reason, List<String> consents)
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    if (consents == null) {
      body.remove("consents");
    } else {
      body.put("consents", consents);
    }

    MvcResult result =
        mvc.perform(Submissions.post(body)).andExpect(status().isBadRequest()).andReturn();

    assertThat(Submissions.errorFields(result)).contains("consents");
    assertNothingStoredOrSent(body);
  }

  @ParameterizedTest(name = "token [{0}]")
  @ValueSource(strings = {"missing", "", "wrong-token"})
  void ac_001_07_missingOrRejectedCaptchaTokenIsRejected(String token) throws Exception {
    Map<String, Object> body = Submissions.validExternal();
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
    String email = String.valueOf(body.get("email"));
    if (body.get("email") == null || email.isBlank()) {
      return;
    }
    Thread.sleep(500);
    assertThat(db.countByEmail(email.strip())).isZero();
    assertThat(copies.filesMentioning(email.strip())).isEmpty();
    assertThat(mailpit.messagesMentioning(email.strip())).isEmpty();
  }
}
