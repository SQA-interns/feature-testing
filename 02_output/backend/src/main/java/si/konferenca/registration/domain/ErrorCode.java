package si.konferenca.registration.domain;

/** Codes of field errors (openapi.yaml FieldError). */
public enum ErrorCode {
  REQUIRED,
  INVALID_FORMAT,
  TOO_LONG,
  NOT_ALLOWED,
  UNKNOWN_OPTION,
  INACTIVE_OPTION,
  DUPLICATE_OPTION,
  OPTION_NOT_AVAILABLE,
  CONSENT_REQUIRED,
  UNKNOWN_CONSENT,
  CAPTCHA_FAILED,
  /** Organizer search: the registration type filter is not a known type (AC-009-22). */
  UNKNOWN_TYPE,
  /** Organizer search: a category filter names an option of another category (AC-009-22). */
  WRONG_CATEGORY
}
