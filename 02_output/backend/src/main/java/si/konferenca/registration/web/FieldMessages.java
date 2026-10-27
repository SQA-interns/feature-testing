package si.konferenca.registration.web;

import si.konferenca.registration.domain.ErrorCode;

/** English messages of the field error codes (registration-form-ui.md, D-13). */
final class FieldMessages {

  private FieldMessages() {}

  static String of(String field, ErrorCode code) {
    return switch (code) {
      case REQUIRED -> "This field is required.";
      case INVALID_FORMAT -> "Enter a valid email address.";
      case TOO_LONG -> "This value is too long.";
      case NOT_ALLOWED -> "This field is not allowed for this registration type.";
      case UNKNOWN_OPTION, INACTIVE_OPTION, DUPLICATE_OPTION, OPTION_NOT_AVAILABLE ->
          "One or more selected options are not available.";
      case CONSENT_REQUIRED -> "This consent is required.";
      case UNKNOWN_CONSENT -> "This consent is not available.";
      case CAPTCHA_FAILED -> "Please confirm that you are not a robot.";
    };
  }
}
