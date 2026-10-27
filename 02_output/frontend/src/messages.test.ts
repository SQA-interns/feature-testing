import { describe, expect, it } from "vitest";
import { MESSAGES, messageFor } from "./messages";

describe("messageFor", () => {
  it.each([
    ["firstName", "REQUIRED", MESSAGES.required],
    ["email", "INVALID_FORMAT", MESSAGES.email],
    ["lastName", "TOO_LONG", MESSAGES.tooLong],
    ["organization", "NOT_ALLOWED", MESSAGES.fieldNotAllowed],
    ["consents.privacy", "CONSENT_REQUIRED", MESSAGES.consentRequired],
    ["consents.x", "UNKNOWN_CONSENT", MESSAGES.consentUnknown],
    ["recaptchaToken", "CAPTCHA_FAILED", MESSAGES.captcha],
    ["x", "UNKNOWN_OPTION", MESSAGES.optionsUnavailable],
    ["x", "INACTIVE_OPTION", MESSAGES.optionsUnavailable],
    ["x", "DUPLICATE_OPTION", MESSAGES.optionsUnavailable],
    ["x", "OPTION_NOT_AVAILABLE", MESSAGES.optionsUnavailable],
    ["optionIds", "REQUIRED", MESSAGES.optionsUnavailable],
    ["firstName", "SOMETHING_NEW", MESSAGES.general],
  ])("%s %s", (field, code, message) => {
    expect(messageFor(field, code)).toBe(message);
  });

  it("uses the texts of registration-form-ui.md", () => {
    expect(MESSAGES).toMatchObject({
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
    });
  });
});
