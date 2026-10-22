// Client-side checks before any request (NFR-03, AC-004-03); the backend checks again (SB-01).
import type { Consent, RegistrationType } from "./types";

export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  consent: "This consent is required.",
};

export type FieldName =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export type FormValues = Record<FieldName, string>;

export const COMMON_FIELDS: FieldName[] = ["firstName", "lastName", "email"];
export const TYPE_FIELDS: Record<RegistrationType, FieldName[]> = {
  EXTERNAL: ["organization"],
  STUDENT: ["studyInstitution", "studyProgramme", "studentId"],
};

const FORBIDDEN = /[\s,;<>()[\]\\":]/;

/** The email rule of the backend (BR-03): one @, a dotted domain, no list or space characters. */
export function isValidEmail(value: string): boolean {
  const at = value.indexOf("@");
  if (at <= 0 || at !== value.lastIndexOf("@") || FORBIDDEN.test(value)) return false;
  const domain = value.slice(at + 1);
  return domain.indexOf(".") > 0 && !domain.endsWith(".") && !domain.includes("..");
}

/** Field errors by field name; "consents" for a missing mandatory consent. */
export function validate(
  type: RegistrationType,
  values: FormValues,
  consents: Consent[],
  given: ReadonlySet<string>,
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of [...COMMON_FIELDS, ...TYPE_FIELDS[type]]) {
    if (!values[field].trim()) errors[field] = MESSAGES.required;
  }
  if (!errors.email && !isValidEmail(values.email.trim())) errors.email = MESSAGES.email;
  if (consents.some((c) => c.mandatory && !given.has(c.id))) errors.consents = MESSAGES.consent;
  return errors;
}
