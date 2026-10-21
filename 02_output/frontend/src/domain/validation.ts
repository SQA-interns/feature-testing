import { FIELDS, type FormConfig, type FormValues } from "./types";

/** `EMAIL_PATTERN` of `docs/02_specification.md` section 4, the same rule as the backend. */
export const EMAIL_PATTERN =
  /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;

export const MAX_TEXT_LENGTH = 200;
export const MAX_EMAIL_LENGTH = 254;

export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  tooLong: "This value is too long.",
  consent: "This consent is required.",
  captcha: "Please confirm that you are not a robot.",
};

/** Field name to message for every rule the values break; empty when they may be sent (NFR-03). */
export function validate(values: FormValues, config: FormConfig): Record<string, string> {
  const errors: Record<string, string> = {};
  if (values.type === "") {
    errors.type = MESSAGES.required;
    return errors;
  }
  for (const field of FIELDS[values.type]) {
    const value = values.text[field.name].trim();
    const max = field.name === "email" ? MAX_EMAIL_LENGTH : MAX_TEXT_LENGTH;
    if (value === "") errors[field.name] = MESSAGES.required;
    else if ([...value].length > max) errors[field.name] = MESSAGES.tooLong;
    else if (field.name === "email" && !EMAIL_PATTERN.test(value)) errors.email = MESSAGES.email;
  }
  const missingConsent = config.consents.some(
    (c) => c.mandatory && !values.consents.includes(c.id),
  );
  if (missingConsent) errors.consents = MESSAGES.consent;
  if (values.captchaToken === "") errors.captchaToken = MESSAGES.captcha;
  return errors;
}
