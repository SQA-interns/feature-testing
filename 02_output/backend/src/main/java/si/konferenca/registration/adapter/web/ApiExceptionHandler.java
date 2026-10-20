package si.konferenca.registration.adapter.web;

import java.util.List;
import java.util.Map;
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
import si.konferenca.registration.application.StorageException;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.ValidationException;

/**
 * Error responses of the API: 400 {@code {"errors": [...]}} for rejected input, otherwise a general
 * message without internals (SB-07). Logs carry the exception class only, never input (ES-07).
 */
@RestControllerAdvice
class ApiExceptionHandler {

  static final String GENERAL = "The request could not be processed.";
  static final String UNREADABLE = "The request body is not valid JSON.";

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ValidationException.class)
  ResponseEntity<Map<String, List<FieldError>>> rejected(ValidationException e) {
    return ResponseEntity.badRequest().body(Map.of("errors", e.errors()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, List<FieldError>>> unreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("errors", List.of(new FieldError("body", UNREADABLE))));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Map<String, String>> unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
    return message(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Send the request as application/json.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Map<String, String>> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
    return message(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Map<String, String>> notFound(NoResourceFoundException e) {
    return message(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(StorageException.class)
  ResponseEntity<Map<String, String>> notStored(StorageException e) {
    return message(HttpStatus.INTERNAL_SERVER_ERROR, GENERAL);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> unexpected(Exception e) {
    LOG.error("request failed ({})", e.getClass().getName());
    return message(HttpStatus.INTERNAL_SERVER_ERROR, GENERAL);
  }

  private static ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
    return ResponseEntity.status(status).body(Map.of("message", text));
  }
}
