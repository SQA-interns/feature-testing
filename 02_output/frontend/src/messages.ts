/** Texts of registration-form-ui.md (English, D-13). */
export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  tooLong: "This value is too long.",
  optionsUnavailable: "One or more selected options are not available.",
  fieldNotAllowed: "This field is not allowed for this registration type.",
  consentRequired: "This consent is required.",
  consentUnknown: "This consent is not available.",
  captcha: "Please confirm that you are not a robot.",
  general: "Your registration could not be sent. Please try again later.",
  configUnavailable:
    "The registration form is not available right now. Please try again later.",
};

/** Texts of organizer-search-ui.md (CR-001). */
export const ORGANIZER_MESSAGES = {
  title: "Organizer registrations",
  wrongCredentials: "Wrong username or password.",
  loadFailed: "The registrations could not be loaded. Please try again later.",
  searchTooLong: "The search text can have at most 100 characters.",
  noMatch: "No registrations match",
};

const OPTION_CODES = [
  "UNKNOWN_OPTION",
  "INACTIVE_OPTION",
  "DUPLICATE_OPTION",
  "OPTION_NOT_AVAILABLE",
];

/** The message shown for a backend field error, by its code (not the backend's text). */
export function messageFor(field: string, code: string): string {
  if (field === "optionIds" || OPTION_CODES.includes(code)) {
    return MESSAGES.optionsUnavailable;
  }
  switch (code) {
    case "REQUIRED":
      return MESSAGES.required;
    case "INVALID_FORMAT":
      return MESSAGES.email;
    case "TOO_LONG":
      return MESSAGES.tooLong;
    case "NOT_ALLOWED":
      return MESSAGES.fieldNotAllowed;
    case "CONSENT_REQUIRED":
      return MESSAGES.consentRequired;
    case "UNKNOWN_CONSENT":
      return MESSAGES.consentUnknown;
    case "CAPTCHA_FAILED":
      return MESSAGES.captcha;
    default:
      return MESSAGES.general;
  }
}
