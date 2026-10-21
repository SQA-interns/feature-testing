package si.konferenca.registration.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import si.konferenca.registration.application.RegistrationRejectedException;
import si.konferenca.registration.application.StorageFailedException;
import si.konferenca.registration.domain.FieldError;

/**
 * Error bodies without internal details (SB-07, ES-07, {@code openapi.json}). Unknown paths and
 * methods never get here: Spring Security denies them first.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  /** The 400 body: one entry per invalid field. */
  public record ValidationErrors(List<FieldError> errors) {

    public ValidationErrors {
      errors = List.copyOf(errors);
    }
  }

  /** Every other error body. */
  public record GeneralError(String message) {}

  static final String STORAGE_FAILED =
      "The registration could not be stored. Please try again later.";
  static final String UNEXPECTED = "The request could not be processed. Please try again later.";
  static final String BODY_INVALID = "Request body is not valid JSON.";
  static final String TOO_LARGE = "The request is too large.";

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(RegistrationRejectedException.class)
  public ResponseEntity<ValidationErrors> rejected(RegistrationRejectedException e) {
    return ResponseEntity.badRequest().body(new ValidationErrors(e.errors()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Object> unreadable(HttpMessageNotReadableException e) {
    if (causedBy(e, PayloadTooLargeException.class)) {
      return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(new GeneralError(TOO_LARGE));
    }
    return ResponseEntity.badRequest()
        .body(new ValidationErrors(List.of(new FieldError("body", BODY_INVALID))));
  }

  @ExceptionHandler(StorageFailedException.class)
  public ResponseEntity<GeneralError> storageFailed(StorageFailedException e) {
    return ResponseEntity.internalServerError().body(new GeneralError(STORAGE_FAILED));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<GeneralError> mediaType(HttpMediaTypeNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(new GeneralError("Send the request as application/json."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<GeneralError> unexpected(Exception e) {
    String type = e.getClass().getName();
    LOG.error("Unexpected error: {}", type);
    return ResponseEntity.internalServerError().body(new GeneralError(UNEXPECTED));
  }

  private static boolean causedBy(Throwable e, Class<? extends Throwable> type) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }
}
