package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.ErrorCode;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRejectedException;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

class ApiMappingTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private static HttpMessageNotReadableException unreadable(Throwable cause) {
    return new HttpMessageNotReadableException("bad", cause, new MockHttpInputMessage(new byte[0]));
  }

  @Test
  void rejectionBecomesAValidationErrorWithMessages() {
    ResponseEntity<ApiResponses.ValidationError> response =
        handler.rejected(
            new RegistrationRejectedException(
                List.of(
                    new FieldError("email", ErrorCode.INVALID_FORMAT),
                    new FieldError("consents.privacy", ErrorCode.CONSENT_REQUIRED))));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isEqualTo(
            new ApiResponses.ValidationError(
                "The registration was rejected.",
                List.of(
                    new ApiResponses.FieldErrorBody(
                        "email", "INVALID_FORMAT", "Enter a valid email address."),
                    new ApiResponses.FieldErrorBody(
                        "consents.privacy", "CONSENT_REQUIRED", "This consent is required."))));
  }

  @Test
  void unreadableBodyIsA400OnTheType() {
    ResponseEntity<Object> response =
        handler.unreadable(unreadable(new IllegalStateException("x")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isEqualTo(
            new ApiResponses.ValidationError(
                "The request could not be read.",
                List.of(
                    new ApiResponses.FieldErrorBody(
                        "type", "REQUIRED", "This field is required."))));
  }

  @Test
  void bodyOverTheLimitIsA413WhereverTheCauseIs() {
    ResponseEntity<Object> direct = handler.unreadable(unreadable(new PayloadTooLargeException()));
    ResponseEntity<Object> nested =
        handler.unreadable(unreadable(new IllegalStateException(new PayloadTooLargeException())));

    assertThat(direct.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    assertThat(direct.getBody()).isEqualTo(new ApiResponses.Error("The request is too large."));
    assertThat(nested.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
  }

  @Test
  void otherFailuresHaveFixedBodies() {
    assertThat(handler.mediaType().getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(handler.mediaType().getBody())
        .isEqualTo(new ApiResponses.Error("The request must be JSON."));
    assertThat(handler.method().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(handler.method().getBody())
        .isEqualTo(new ApiResponses.Error("This method is not allowed."));
    assertThat(handler.notFound().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(handler.notFound().getBody()).isEqualTo(new ApiResponses.Error("Not found."));
    ResponseEntity<Object> unexpected =
        handler.unexpected(new IllegalStateException("secret internals"));
    assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(unexpected.getBody())
        .isEqualTo(new ApiResponses.Error("The request could not be processed."));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "REQUIRED|This field is required.",
        "INVALID_FORMAT|Enter a valid email address.",
        "TOO_LONG|This value is too long.",
        "NOT_ALLOWED|This field is not allowed for this registration type.",
        "UNKNOWN_OPTION|One or more selected options are not available.",
        "INACTIVE_OPTION|One or more selected options are not available.",
        "DUPLICATE_OPTION|One or more selected options are not available.",
        "OPTION_NOT_AVAILABLE|One or more selected options are not available.",
        "CONSENT_REQUIRED|This consent is required.",
        "UNKNOWN_CONSENT|This consent is not available.",
        "CAPTCHA_FAILED|Please confirm that you are not a robot.",
      })
  void everyCodeHasAMessage(ErrorCode code, String message) {
    assertThat(FieldMessages.of("any", code)).isEqualTo(message);
  }

  private static FormConfiguration configuration(boolean testMode) {
    return new FormConfiguration(
        "Conf",
        testMode,
        "site-key",
        List.of(
            new ConferenceOption(
                "ws-a", "A", OptionCategory.WORKSHOP, true, EnumSet.allOf(RegistrationType.class)),
            new ConferenceOption(
                "ev-b", "B", OptionCategory.EVENT, true, Set.of(RegistrationType.STUDENT))),
        List.of(new ConsentDefinition("privacy", "P", true)));
  }

  @Test
  void formConfigInTestModeHasNoSiteKey() {
    ApiResponses.FormConfig body = ApiResponses.FormConfig.of(configuration(true));

    assertThat(body.conferenceName()).isEqualTo("Conf");
    assertThat(body.captcha()).isEqualTo(new ApiResponses.Captcha("TEST", null));
    assertThat(body.options())
        .containsExactly(
            new ApiResponses.FormOption("ws-a", "A", "WORKSHOP", List.of("EXTERNAL", "STUDENT")),
            new ApiResponses.FormOption("ev-b", "B", "EVENT", List.of("STUDENT")));
    assertThat(body.consents()).containsExactly(new ApiResponses.FormConsent("privacy", "P", true));
  }

  @Test
  void formConfigWithRecaptchaHasTheSiteKey() {
    assertThat(ApiResponses.FormConfig.of(configuration(false)).captcha())
        .isEqualTo(new ApiResponses.Captcha("RECAPTCHA", "site-key"));
  }

  @Test
  void acceptedBodyNamesTheRegistration() {
    UUID id = UUID.randomUUID();
    Registration registration =
        new Registration(
            id,
            RegistrationType.STUDENT,
            Instant.parse("2026-10-27T10:00:00.120Z"),
            new Participant("Ana", "Novak", "a@x.si", null, "U", "P", "1"),
            List.of(new SelectedOption("ws-a", "A", OptionCategory.WORKSHOP)),
            List.of());

    assertThat(ApiResponses.Accepted.of(registration))
        .isEqualTo(
            new ApiResponses.Accepted(
                id.toString(),
                "2026-10-27T10:00:00.120Z",
                "STUDENT",
                "Ana",
                "Novak",
                List.of(new ApiResponses.Option("ws-a", "A", "WORKSHOP"))));
  }

  @Test
  void requestKeepsNullsAndTurnsMissingListsIntoEmptyOnes() {
    List<String> ids = new ArrayList<>();
    ids.add(null);
    RegistrationRequest request =
        new RegistrationRequest("STUDENT", "A", "N", "e", "o", "i", "p", "s", ids, null, "t");
    ids.add("late");

    assertThat(request.optionIds()).containsExactly((String) null);
    assertThat(request.consents()).isEmpty();
    assertThat(request.toSubmission())
        .isEqualTo(
            new si.konferenca.registration.domain.Submission(
                "STUDENT", "A", "N", "e", "o", "i", "p", "s", ids.subList(0, 1), List.of(), "t"));
  }
}
