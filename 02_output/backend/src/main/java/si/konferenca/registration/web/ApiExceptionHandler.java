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
import si.konferenca.registration.application.CaptchaUnavailableException;
import si.konferenca.registration.application.StorageException;
import si.konferenca.registration.domain.FieldError;

/** Maps failures to the API's error bodies; nothing internal reaches the client (SB-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponses.FieldErrors> unreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponses.FieldErrors(
                List.of(new FieldError("body", "The request body is not valid."))));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponses.General> mediaType(HttpMediaTypeNotSupportedException e) {
    return general(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Send the request as application/json.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponses.General> method(HttpRequestMethodNotSupportedException e) {
    return general(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponses.General> notFound(NoResourceFoundException e) {
    return general(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<ErrorResponses.General> storage(StorageException e) {
    LOG.error("{}: {}", e.getMessage(), causeName(e));
    return general(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Your registration could not be stored. Please try again later.");
  }

  @ExceptionHandler(CaptchaUnavailableException.class)
  public ResponseEntity<ErrorResponses.General> captcha(CaptchaUnavailableException e) {
    LOG.warn("{}: {}", e.getMessage(), causeName(e));
    return general(
        HttpStatus.SERVICE_UNAVAILABLE,
        "The registration cannot be checked right now. Please try again later.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponses.General> unexpected(Exception e) {
    LOG.error("Unexpected failure: {}", e.getClass().getName());
    return general(
        HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
  }

  private static String causeName(Throwable e) {
    return e.getCause() == null ? "-" : e.getCause().getClass().getName();
  }

  private static ResponseEntity<ErrorResponses.General> general(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ErrorResponses.General(message));
  }
}
