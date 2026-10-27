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
  CAPTCHA_FAILED
}
