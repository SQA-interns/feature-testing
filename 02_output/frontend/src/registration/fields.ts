// The fixed participant fields per registration type (BR-01) and the client-side checks
// that mirror the backend rules V-01 … V-03 and the mandatory consents (NFR-03).
import type { ConsentDefinition, RegistrationType } from "../api";

export type FieldName =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export interface FieldDefinition {
  name: FieldName;
  label: string;
  inputType: "text" | "email";
}

const COMMON: FieldDefinition[] = [
  { name: "firstName", label: "First name", inputType: "text" },
  { name: "lastName", label: "Last name", inputType: "text" },
  { name: "email", label: "Email", inputType: "email" },
];

export const FIELDS: Record<RegistrationType, FieldDefinition[]> = {
  EXTERNAL: [
    ...COMMON,
    { name: "organization", label: "Organization / institution", inputType: "text" },
  ],
  STUDENT: [
    ...COMMON,
    { name: "studyInstitution", label: "Study institution", inputType: "text" },
    { name: "studyProgramme", label: "Study programme", inputType: "text" },
    { name: "studentId", label: "Student ID", inputType: "text" },
  ],
};

export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  consent: "This consent is required.",
};

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export type Values = Partial<Record<FieldName, string>>;

/** Field name (or "consents") to message; empty when the form may be sent. */
export function validate(
  type: RegistrationType,
  values: Values,
  givenConsents: string[],
  consents: ConsentDefinition[],
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of FIELDS[type]) {
    const value = (values[field.name] ?? "").trim();
    if (value === "") {
      errors[field.name] = MESSAGES.required;
    } else if (field.name === "email" && !EMAIL.test(value)) {
      errors[field.name] = MESSAGES.email;
    }
  }
  if (consents.some((c) => c.mandatory && !givenConsents.includes(c.id))) {
    errors.consents = MESSAGES.consent;
  }
  return errors;
}
