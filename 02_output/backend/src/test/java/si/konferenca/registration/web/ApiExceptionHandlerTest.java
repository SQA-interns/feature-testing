package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.konferenca.registration.application.CaptchaUnavailableException;
import si.konferenca.registration.application.StorageException;
import si.konferenca.registration.domain.FieldError;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private static void assertGeneral(
      ResponseEntity<ErrorResponses.General> response, int status, String message) {
    assertThat(response.getStatusCode().value()).isEqualTo(status);
    assertThat(response.getBody()).isEqualTo(new ErrorResponses.General(message));
  }

  @Test
  void unreadableBodyIsAFieldErrorOnBody() {
    ResponseEntity<ErrorResponses.FieldErrors> response =
        handler.unreadable(
            new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])));

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody().errors())
        .isEqualTo(List.of(new FieldError("body", "The request body is not valid.")));
  }

  @Test
  void protocolErrorsHaveGeneralMessages() {
    assertGeneral(
        handler.mediaType(new HttpMediaTypeNotSupportedException("text/plain")),
        415,
        "Send the request as application/json.");
    assertGeneral(
        handler.method(new HttpRequestMethodNotSupportedException("PUT")),
        405,
        "Method not allowed.");
    assertGeneral(
        handler.notFound(new NoResourceFoundException(HttpMethod.GET, "/x", "x")),
        404,
        "Not found.");
  }

  @Test
  void failuresHideTheirInternals() {
    assertGeneral(
        handler.storage(
            new StorageException("Registration 1 not stored", new RuntimeException("x"))),
        500,
        "Your registration could not be stored. Please try again later.");
    assertGeneral(
        handler.storage(new StorageException("Registration 1 not stored", null)),
        500,
        "Your registration could not be stored. Please try again later.");
    assertGeneral(
        handler.captcha(new CaptchaUnavailableException("down", null)),
        503,
        "The registration cannot be checked right now. Please try again later.");
    assertGeneral(
        handler.unexpected(new IllegalStateException("secret detail")),
        500,
        "Something went wrong. Please try again later.");
  }
}
