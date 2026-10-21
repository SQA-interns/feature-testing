package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import si.konferenca.registration.application.RegistrationRejectedException;
import si.konferenca.registration.application.StorageFailedException;
import si.konferenca.registration.domain.FieldError;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private static HttpMessageNotReadableException unreadable(Throwable cause) {
    return new HttpMessageNotReadableException("x", cause, new MockHttpInputMessage(new byte[0]));
  }

  @Test
  void rejectedRegistrationIsA400WithTheFieldErrors() {
    List<FieldError> errors = List.of(new FieldError("email", "Enter a valid email address."));

    ResponseEntity<ApiExceptionHandler.ValidationErrors> response =
        handler.rejected(new RegistrationRejectedException(errors));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().errors()).isEqualTo(errors);
  }

  @Test
  void unreadableBodyIsA400ForTheBodyField() {
    ResponseEntity<Object> response = handler.unreadable(unreadable(new IOException("bad")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isEqualTo(
            new ApiExceptionHandler.ValidationErrors(
                List.of(new FieldError("body", ApiExceptionHandler.BODY_INVALID))));
  }

  @Test
  void bodyThatGrewTooLargeWhileReadingIsA413() {
    ResponseEntity<Object> response =
        handler.unreadable(unreadable(new IllegalStateException(new PayloadTooLargeException())));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    assertThat(response.getBody())
        .isEqualTo(new ApiExceptionHandler.GeneralError(ApiExceptionHandler.TOO_LARGE));
  }

  @Test
  void storageFailureIsA500WithoutDetails() {
    ResponseEntity<ApiExceptionHandler.GeneralError> response =
        handler.storageFailed(new StorageFailedException(new IllegalStateException("SQL")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().message()).isEqualTo(ApiExceptionHandler.STORAGE_FAILED);
  }

  @Test
  void wrongMediaTypeIsA415() {
    ResponseEntity<ApiExceptionHandler.GeneralError> response =
        handler.mediaType(new HttpMediaTypeNotSupportedException("text/plain"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(response.getBody().message()).isEqualTo("Send the request as application/json.");
  }

  @Test
  void anyOtherErrorIsA500WithAGenericMessage() {
    ResponseEntity<ApiExceptionHandler.GeneralError> response =
        handler.unexpected(new IllegalArgumentException("internal detail"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().message()).isEqualTo(ApiExceptionHandler.UNEXPECTED);
  }
}
