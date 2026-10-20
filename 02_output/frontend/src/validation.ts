// Client-side checks before sending (NFR-03, AC-004-03); the backend validates again (SB-01).
import type { FieldErrors, FormConsent, RegistrationType, TextField } from "./types";

export const MAX_TEXT_LENGTH = 200;
export const MAX_EMAIL_LENGTH = 254;

// D-09: same rule as the backend RegistrationValidator.
const EMAIL =
  /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;

export const MESSAGES = {
  required: "This field is required.",
  tooLong: (max: number) => `This field must be at most ${max} characters long.`,
  email: "Enter a valid email address.",
  consent: "The required consent must be given.",
  captcha: "Please confirm that you are not a robot.",
};

export function fieldsOf(type: RegistrationType): TextField[] {
  return type === "EXTERNAL"
    ? ["firstName", "lastName", "email", "organization"]
    : ["firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId"];
}

export interface FormInput {
  type: RegistrationType;
  values: Partial<Record<TextField, string>>;
  consents: ReadonlySet<string>;
  captchaToken: string | null;
}

/** Errors by request field name; empty when the form may be sent. */
export function validate(input: FormInput, consents: FormConsent[]): FieldErrors {
  const errors: FieldErrors = {};
  for (const field of fieldsOf(input.type)) {
    const value = (input.values[field] ?? "").trim();
    const max = field === "email" ? MAX_EMAIL_LENGTH : MAX_TEXT_LENGTH;
    if (value === "") {
      errors[field] = MESSAGES.required;
    } else if (value.length > max) {
      errors[field] = MESSAGES.tooLong(max);
    } else if (field === "email" && !EMAIL.test(value)) {
      errors[field] = MESSAGES.email;
    }
  }
  if (consents.some((c) => c.mandatory && !input.consents.has(c.id))) {
    errors.consents = MESSAGES.consent;
  }
  if (!input.captchaToken) {
    errors.captchaToken = MESSAGES.captcha;
  }
  return errors;
}
