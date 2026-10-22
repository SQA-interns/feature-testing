package si.konferenca.registration.adapter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps failures to the error body without internal details (SB-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorBody> unreadable(HttpMessageNotReadableException e) {
    return error(HttpStatus.BAD_REQUEST, "body", "The request body is not valid JSON.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErrorBody> mediaType(HttpMediaTypeNotSupportedException e) {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "body", "The request body must be JSON.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorBody> method(HttpRequestMethodNotSupportedException e) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "request", "Method not allowed.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorBody> notFound(NoResourceFoundException e) {
    return error(HttpStatus.NOT_FOUND, "request", "Not found.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(Exception e) {
    LOG.error("Request failed with {}", e.getClass().getName());
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "server", "Internal error.");
  }

  static ResponseEntity<ErrorBody> error(HttpStatus status, String field, String message) {
    return ResponseEntity.status(status)
        .contentType(MediaType.parseMediaType(ErrorResponses.JSON_UTF8))
        .body(ErrorBody.of(field, message));
  }
}
