import { MESSAGES } from "./messages";
import type { FormConsent, RegistrationType, TextField } from "./types";

/** The fields of each registration type, with their labels and maximum lengths (BR-01, D-14). */
export const FIELDS: Record<TextField, { label: string; max: number }> = {
  firstName: { label: "First name", max: 200 },
  lastName: { label: "Last name", max: 200 },
  email: { label: "Email", max: 254 },
  organization: { label: "Organization / institution", max: 200 },
  studyInstitution: { label: "Study institution", max: 200 },
  studyProgramme: { label: "Study programme", max: 200 },
  studentId: { label: "Student ID", max: 50 },
};

export const FIELDS_BY_TYPE: Record<RegistrationType, TextField[]> = {
  EXTERNAL: ["firstName", "lastName", "email", "organization"],
  STUDENT: [
    "firstName",
    "lastName",
    "email",
    "studyInstitution",
    "studyProgramme",
    "studentId",
  ],
};

const LOCAL_PART =
  /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*$/;
const DOMAIN = /^(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/;

/** The email rule of specification S4.2, the same as the backend's. */
export function isValidEmail(value: string): boolean {
  const at = value.indexOf("@");
  if (at <= 0 || at !== value.lastIndexOf("@")) {
    return false;
  }
  const local = value.slice(0, at);
  return (
    local.length <= 64 &&
    LOCAL_PART.test(local) &&
    DOMAIN.test(value.slice(at + 1))
  );
}

export interface FormState {
  type: RegistrationType;
  values: Partial<Record<TextField, string>>;
  consents: string[];
  captchaToken: string;
}

/** Field errors by field key (as the backend names them); empty when the form may be sent. */
export function validate(
  state: FormState,
  consents: FormConsent[],
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of FIELDS_BY_TYPE[state.type]) {
    const value = (state.values[field] ?? "").trim();
    if (value === "") {
      errors[field] = MESSAGES.required;
    } else if ([...value].length > FIELDS[field].max) {
      errors[field] = MESSAGES.tooLong;
    } else if (field === "email" && !isValidEmail(value)) {
      errors[field] = MESSAGES.email;
    }
  }
  for (const consent of consents) {
    if (consent.mandatory && !state.consents.includes(consent.id)) {
      errors[`consents.${consent.id}`] = MESSAGES.consentRequired;
    }
  }
  if (state.captchaToken === "") {
    errors.recaptchaToken = MESSAGES.captcha;
  }
  return errors;
}
