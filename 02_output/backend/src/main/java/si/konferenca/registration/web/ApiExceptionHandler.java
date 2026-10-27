package si.konferenca.registration.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.konferenca.registration.domain.ErrorCode;
import si.konferenca.registration.domain.RegistrationRejectedException;

/** Maps failures to the error bodies of openapi.yaml, never with internals (SB-07, ES-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  static final String GENERAL_ERROR = "The request could not be processed.";
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(RegistrationRejectedException.class)
  ResponseEntity<ApiResponses.ValidationError> rejected(RegistrationRejectedException e) {
    return ResponseEntity.badRequest()
        .body(
            new ApiResponses.ValidationError(
                "The registration was rejected.",
                e.errors().stream()
                    .map(
                        f ->
                            new ApiResponses.FieldErrorBody(
                                f.field(), f.code().name(), FieldMessages.of(f.field(), f.code())))
                    .toList()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Object> unreadable(HttpMessageNotReadableException e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof PayloadTooLargeException) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "The request is too large.");
      }
    }
    return ResponseEntity.badRequest()
        .body(
            new ApiResponses.ValidationError(
                "The request could not be read.",
                List.of(
                    new ApiResponses.FieldErrorBody(
                        "type",
                        ErrorCode.REQUIRED.name(),
                        FieldMessages.of("type", ErrorCode.REQUIRED)))));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Object> mediaType() {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request must be JSON.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Object> method() {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "This method is not allowed.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Object> notFound() {
    return error(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpected(Exception e) {
    LOG.error("request failed: {}", e.getClass().getName());
    return error(HttpStatus.INTERNAL_SERVER_ERROR, GENERAL_ERROR);
  }

  private static ResponseEntity<Object> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ApiResponses.Error(message));
  }
}
