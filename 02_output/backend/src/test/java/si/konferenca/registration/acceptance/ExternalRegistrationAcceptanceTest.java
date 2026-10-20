package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.file.Files;
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
import tools.jackson.databind.JsonNode;

/** US-001 external participant registration: AC-001-01 … AC-001-07 (API level). */
class ExternalRegistrationAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  @DisplayName("AC-001-01 a valid external submission is accepted (201) with the new id")
  void ac_001_01_validExternalSubmissionIsAccepted() throws Exception {
    HttpResponse<String> response = postRegistration(validExternal());

    String id = createdId(response);
    assertThat(registrationRow(id).get("type")).isEqualTo("EXTERNAL");
    assertThat(optionIds(id)).containsExactly("ev-gala", "ws-ai");
  }

  @Test
  @DisplayName("AC-001-01 a valid external submission without options is accepted")
  void ac_001_01_validExternalSubmissionWithoutOptionsIsAccepted() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("options", List.of());

    String id = createdId(postRegistration(body));
    assertThat(optionIds(id)).isEmpty();
  }

  static Stream<Arguments> missingExternalFields() {
    List<Arguments> cases = new ArrayList<>();
    for (String field : List.of("firstName", "lastName", "email", "organization")) {
      for (String variant : List.of("missing", "null", "empty", "whitespace")) {
        cases.add(Arguments.of(field, variant));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "AC-001-02 {0} {1} is rejected with an error for {0}")
  @MethodSource("missingExternalFields")
  void ac_001_02_missingRequiredFieldIsRejected(String field, String variant) throws Exception {
    Map<String, Object> body = validExternal();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "null" -> body.put(field, null);
      case "empty" -> body.put(field, "");
      default -> body.put(field, "   \t ");
    }

    assertThat(errorFields(postRegistration(body))).contains(field);
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-001-03 leading and trailing spaces are removed from the stored values")
  void ac_001_03_textFieldsAreStoredTrimmed() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "  Ana  ");
    body.put("lastName", " Novak ");
    body.put("email", "  ana.novak@example.org ");
    body.put("organization", "   Example d.o.o.   ");

    String id = createdId(postRegistration(body));

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo("Novak");
    assertThat(row.get("email")).isEqualTo("ana.novak@example.org");
    assertThat(row.get("organization")).isEqualTo("Example d.o.o.");
    JsonNode copy = json.readTree(Files.readString(jsonCopy(id)));
    assertThat(copy.path("firstName").asString()).isEqualTo("Ana");
    assertThat(copy.path("lastName").asString()).isEqualTo("Novak");
    assertThat(copy.path("email").asString()).isEqualTo("ana.novak@example.org");
    assertThat(copy.path("organization").asString()).isEqualTo("Example d.o.o.");
  }

  @ParameterizedTest(name = "AC-001-04 email \"{0}\" is rejected with an error for email")
  @ValueSource(
      strings = {
        "not-an-email",
        "ana@",
        "@example.org",
        "ana@@example.org",
        "ana novak@example.org"
      })
  void ac_001_04_invalidEmailIsRejected(String email) throws Exception {
    Map<String, Object> body = validExternal();
    body.put("email", email);

    assertThat(errorFields(postRegistration(body))).contains("email");
    assertNothingStored();
  }

  static Stream<Arguments> invalidOptions() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-ai", "no-such-option")),
        Arguments.of("inactive", List.of("ws-old")),
        Arguments.of("listed twice", List.of("ws-ai", "ws-ai")));
  }

  @ParameterizedTest(name = "AC-001-05 an option that is {0} is rejected with an error for options")
  @MethodSource("invalidOptions")
  void ac_001_05_invalidOptionIsRejected(String reason, List<String> options) throws Exception {
    Map<String, Object> body = validExternal();
    body.put("options", options);

    assertThat(errorFields(postRegistration(body))).contains("options");
    assertNothingStored();
  }

  static Stream<Arguments> missingMandatoryConsent() {
    return Stream.of(
        Arguments.of("no consent", List.of()),
        Arguments.of("only the optional consent", List.of("photos")));
  }

  @ParameterizedTest(name = "AC-001-06 {0} given is rejected with an error for consents")
  @MethodSource("missingMandatoryConsent")
  void ac_001_06_mandatoryConsentMissingIsRejected(String reason, List<String> consents)
      throws Exception {
    Map<String, Object> body = validExternal();
    body.put("consents", consents);

    assertThat(errorFields(postRegistration(body))).contains("consents");
    assertNothingStored();
  }

  @Test
  @DisplayName("AC-001-06 a missing consents list is rejected with an error for consents")
  void ac_001_06_consentsAbsentIsRejected() throws Exception {
    Map<String, Object> body = validExternal();
    body.remove("consents");

    assertThat(errorFields(postRegistration(body))).contains("consents");
    assertNothingStored();
  }

  static Stream<Arguments> badCaptchaTokens() {
    return Stream.of(
        Arguments.of("missing", null), Arguments.of("empty", ""), Arguments.of("wrong", "nope"));
  }

  @ParameterizedTest(name = "AC-001-07 a {0} reCAPTCHA token is rejected and nothing is stored")
  @MethodSource("badCaptchaTokens")
  void ac_001_07_badCaptchaTokenIsRejected(String reason, String token) throws Exception {
    Map<String, Object> body = validExternal();
    if (token == null) {
      body.remove("captchaToken");
    } else {
      body.put("captchaToken", token);
    }

    assertThat(errorFields(postRegistration(body))).contains("captchaToken");
    assertNothingStored();
  }
}
