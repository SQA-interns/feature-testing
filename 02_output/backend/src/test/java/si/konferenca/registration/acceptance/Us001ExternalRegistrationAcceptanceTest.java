package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
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

/** US-001 External participant registration, through {@code POST /api/registrations}. */
class Us001ExternalRegistrationAcceptanceTest extends StandardAcceptanceTest {

  @Test
  @DisplayName("AC-001-01 a valid external submission is accepted with the new registration id")
  void ac001_01_validExternalSubmissionIsAccepted() {
    HttpApi.Response response = register(validExternal());

    assertThat(response.status()).as("response %s", response).isEqualTo(201);
    String id = response.json().path("id").asString();
    assertThat(id).isNotBlank();
    assertThat(db.rows()).extracting(Database.Row::id).containsExactly(UUID.fromString(id));
  }

  static Stream<Arguments> requiredExternalFieldVariants() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, "empty"),
                    Arguments.of(field, "whitespace")));
  }

  @ParameterizedTest(name = "AC-001-02 {0} {1} is rejected")
  @MethodSource("requiredExternalFieldVariants")
  void ac001_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String variant) {
    Map<String, Object> submission = validExternal();
    switch (variant) {
      case "missing" -> submission.remove(field);
      case "empty" -> submission.put(field, "");
      default -> submission.put(field, " \t  ");
    }

    assertRejected(register(submission), field);
  }

  @Test
  @DisplayName("AC-001-03 leading and trailing spaces are removed from the stored values")
  void ac001_03_storedValuesAreTrimmed() {
    UUID id =
        registerAccepted(
            with(
                validExternal(),
                "firstName",
                "  Ana ",
                "lastName",
                " Novak  ",
                "email",
                "  ana.novak@example.org ",
                "organization",
                "   Example d.o.o.   "));

    Database.Row row = db.row(id);
    assertThat(row.firstName()).isEqualTo("Ana");
    assertThat(row.lastName()).isEqualTo("Novak");
    assertThat(row.email()).isEqualTo("ana.novak@example.org");
    assertThat(row.organization()).isEqualTo("Example d.o.o.");
  }

  @ParameterizedTest(name = "AC-001-04 email \"{0}\" is rejected")
  @ValueSource(
      strings = {
        "ana.novak",
        "ana.novak@",
        "@example.org",
        "ana novak@example.org",
        "ana@@example.org",
        "ana@example"
      })
  void ac001_04_invalidEmailIsRejected(String email) {
    assertRejected(register(with(validExternal(), "email", email)), "email");
  }

  @ParameterizedTest(name = "AC-001-05 options {0} are rejected")
  @MethodSource("invalidOptionSelections")
  void ac001_05_unknownInactiveOrDuplicateOptionIsRejected(List<String> options) {
    assertRejected(register(with(validExternal(), "options", options)), "options");
  }

  static Stream<List<String>> invalidOptionSelections() {
    return Stream.of(
        List.of("ws-ai", "no-such-option"),
        List.of("ws-retired"),
        List.of("ws-ai", "ev-dinner", "ws-ai"));
  }

  @Test
  @DisplayName("AC-001-06 a submission without the mandatory consent is rejected")
  void ac001_06_missingMandatoryConsentIsRejected() {
    assertRejected(register(with(validExternal(), "consents", List.of("photos"))), "consents");
    assertRejected(register(with(validExternal(), "consents", List.of())), "consents");
  }

  @Test
  @DisplayName("AC-001-07 a missing reCAPTCHA token is rejected and nothing is stored")
  void ac001_07_missingCaptchaTokenIsRejected() {
    Map<String, Object> submission = validExternal();
    submission.remove("captchaToken");
    assertRejected(register(submission), "captchaToken");

    assertRejected(register(with(validExternal(), "captchaToken", "")), "captchaToken");
  }

  @Test
  @DisplayName("AC-001-07 a token the verifier rejects is rejected and nothing is stored")
  void ac001_07_rejectedCaptchaTokenIsRejected() {
    assertRejected(
        register(with(validExternal(), "captchaToken", "not-the-test-token")), "captchaToken");
  }
}
