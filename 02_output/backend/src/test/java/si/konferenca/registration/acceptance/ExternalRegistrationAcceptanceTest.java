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

/** US-001 External participant registration, through POST /api/registrations. */
class ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  @Test
  @DisplayName("AC-001-01 valid external submission is accepted with a new id")
  void ac_001_01_validExternalSubmissionIsAcceptedWithNewId() {
    HttpResponse<String> response = postRegistration(Submissions.validExternal());

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    String id = json(response).path("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(queryLong("SELECT count(*) FROM registration WHERE id = ?", UUID.fromString(id)))
        .isEqualTo(1);
  }

  static Stream<Arguments> missingExternalFields() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, "empty"),
                    Arguments.of(field, "whitespace")));
  }

  @ParameterizedTest(name = "AC-001-02 {0} {1}")
  @MethodSource("missingExternalFields")
  void ac_001_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String variant) {
    Map<String, Object> body = Submissions.validExternal();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "empty" -> body.put(field, "");
      default -> body.put(field, " \t  ");
    }

    assertRejected(postRegistration(body), field);
  }

  @Test
  @DisplayName("AC-001-03 leading and trailing spaces are removed before storing")
  void ac_001_03_leadingAndTrailingSpacesAreRemovedBeforeStoring() {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "  Ana ");
    body.put("lastName", "\tNovak  ");
    body.put("email", "  ana.novak@example.org ");
    body.put("organization", "   Example Institute\t");

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    UUID id = UUID.fromString(json(response).path("id").asString());
    List<Map<String, Object>> rows =
        queryRows(
            "SELECT first_name, last_name, email, organization FROM registration WHERE id = ?", id);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0))
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", "ana.novak@example.org")
        .containsEntry("organization", "Example Institute");
  }

  @ParameterizedTest(name = "AC-001-04 email \"{0}\"")
  @ValueSource(
      strings = {
        "ana.novak",
        "ana.novak@",
        "@example.org",
        "ana novak@example.org",
        "ana@@example.org",
        "ana@example",
        "ana@example.org,eve@example.org",
        "ana@example.org\r\nBcc: eve@example.org"
      })
  void ac_001_04_invalidEmailFormatIsRejected(String email) {
    Map<String, Object> body = Submissions.validExternal();
    body.put("email", email);

    assertRejected(postRegistration(body), "email");
  }

  static Stream<Arguments> invalidOptions() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-testing", "no-such-option")),
        Arguments.of("inactive", List.of("ws-legacy")),
        Arguments.of("listed twice", List.of("ev-dinner", "ev-dinner")));
  }

  @ParameterizedTest(name = "AC-001-05 option {0}")
  @MethodSource("invalidOptions")
  void ac_001_05_unknownInactiveOrDuplicateOptionIsRejected(String kind, List<String> optionIds) {
    Map<String, Object> body = Submissions.validExternal();
    body.put("optionIds", optionIds);

    assertRejected(postRegistration(body), "optionIds");
  }

  static Stream<Arguments> missingMandatoryConsent() {
    return Stream.of(
        Arguments.of("consents absent", null),
        Arguments.of("no consent", List.of()),
        Arguments.of("only the optional consent", List.of("photos")));
  }

  @ParameterizedTest(name = "AC-001-06 {0}")
  @MethodSource("missingMandatoryConsent")
  void ac_001_06_missingMandatoryConsentIsRejected(String variant, List<String> consents) {
    Map<String, Object> body = Submissions.validExternal();
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
        Arguments.of("empty", ""),
        Arguments.of("rejected by the verifier", "test-fail"));
  }

  @ParameterizedTest(name = "AC-001-07 token {0}")
  @MethodSource("badCaptchaTokens")
  void ac_001_07_missingOrRejectedCaptchaTokenIsRejectedAndNothingStored(
      String variant, String token) {
    Map<String, Object> body = Submissions.validExternal();
    if (token == null) {
      body.remove("recaptchaToken");
    } else {
      body.put("recaptchaToken", token);
    }

    assertRejected(postRegistration(body), "recaptchaToken");
  }
}
