package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.node.ObjectNode;

/** US-001 External participant registration (AC-001-01..07; AC-001-08 is a frontend test). */
class Us001ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @DisplayName("AC-001-01")
  @Test
  void ac_001_01_validExternalSubmissionIsAcceptedWithNewId() {
    ObjectNode body = options(validExternal(), "ws-ai", "ev-gala", "meal-lunch", "other-tour");

    HttpResponse<byte[]> response = postRegistration(body);

    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
    String id = json(response).path("id").asString();
    assertThat(id).isNotBlank();

    HttpResponse<byte[]> second = postRegistration(validExternal());
    assertThat(second.statusCode()).isEqualTo(201);
    assertThat(json(second).path("id").asString()).isNotBlank().isNotEqualTo(id);
  }

  @DisplayName("AC-001-01")
  @Test
  void ac_001_01_validExternalSubmissionWithoutOptionsIsAccepted() {
    HttpResponse<byte[]> response = postRegistration(options(validExternal()));

    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
    assertThat(json(response).path("id").asString()).isNotBlank();
  }

  static Stream<Arguments> requiredExternalFields() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, "empty"),
                    Arguments.of(field, "whitespace")));
  }

  @DisplayName("AC-001-02")
  @ParameterizedTest(name = "AC-001-02 {0} {1}")
  @MethodSource("requiredExternalFields")
  void ac_001_02_missingRequiredFieldIsRejectedWithFieldError(String field, String variant) {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    switch (variant) {
      case "missing" -> body.remove(field);
      case "empty" -> body.put(field, "");
      default -> body.put(field, "   \t ");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, field);
    assertNothingCreated(before, email);
  }

  @DisplayName("AC-001-03")
  @Test
  void ac_001_03_leadingAndTrailingSpacesAreRemovedBeforeStoring() throws Exception {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    body.put("firstName", "  Ana ");
    body.put("lastName", "\tNovak  ");
    body.put("email", "  " + email + " ");
    body.put("organization", "  Example d.o.o.   ");

    String id = register(body);

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo("Novak");
    assertThat(row.get("email")).isEqualTo(email);
    assertThat(row.get("organization")).isEqualTo("Example d.o.o.");
  }

  @DisplayName("AC-001-04")
  @ParameterizedTest(name = "AC-001-04 email \"{0}\"")
  @ValueSource(
      strings = {
        "ana.example.org",
        "ana@",
        "@example.org",
        "ana novak@example.org",
        "ana@@example.org",
        "ana@exa mple.org"
      })
  void ac_001_04_invalidEmailFormatIsRejectedWithEmailError(String invalidEmail) {
    ObjectNode body = validExternal();
    body.put("email", invalidEmail);
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "email");
    assertThat(registrationRowCount()).isEqualTo(before.rows());
    assertThat(jsonCopyCount()).isEqualTo(before.jsonCopies());
  }

  @DisplayName("AC-001-05")
  @ParameterizedTest(name = "AC-001-05 options {0}")
  @ValueSource(strings = {"unknown", "inactive", "duplicate"})
  void ac_001_05_unknownInactiveOrDuplicateOptionIsRejected(String variant) {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    switch (variant) {
      case "unknown" -> options(body, "ws-ai", "no-such-option");
      case "inactive" -> options(body, "meal-veg");
      default -> options(body, "ws-ai", "ws-ai");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "options");
    assertNothingCreated(before, email);
  }

  @DisplayName("AC-001-06")
  @ParameterizedTest(name = "AC-001-06 consents {0}")
  @ValueSource(strings = {"empty", "missing", "optional-only"})
  void ac_001_06_missingMandatoryConsentIsRejectedWithConsentError(String variant) {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    switch (variant) {
      case "empty" -> body.putArray("consents");
      case "missing" -> body.remove("consents");
      default -> body.putArray("consents").add("photo");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "consents");
    assertNothingCreated(before, email);
  }

  @DisplayName("AC-001-07")
  @ParameterizedTest(name = "AC-001-07 captcha {0}")
  @ValueSource(strings = {"missing", "empty", "rejected"})
  void ac_001_07_missingOrRejectedCaptchaIsRejectedAndNothingStored(String variant) {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    switch (variant) {
      case "missing" -> body.remove("captchaToken");
      case "empty" -> body.put("captchaToken", "");
      default -> body.put("captchaToken", "not-a-valid-token");
    }
    StoreSnapshot before = snapshot();

    HttpResponse<byte[]> response = postRegistration(body);

    assertFieldError(response, "captchaToken");
    assertNothingCreated(before, email);
  }
}
