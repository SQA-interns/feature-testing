/** Types of the REST API (`docs/02_contracts/openapi.json`) and the form (`ui-registration-form.json`). */

export type RegistrationType = "EXTERNAL" | "STUDENT";
export type OptionCategory = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface FormOption {
  id: string;
  name: string;
  category: OptionCategory;
  availableTo: RegistrationType[];
}

export interface FormConsent {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  options: FormOption[];
  consents: FormConsent[];
  captchaSiteKey: string;
  captchaTestMode: boolean;
}

export interface FieldError {
  field: string;
  message: string;
}

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export interface FieldDefinition {
  name: TextField;
  label: string;
  inputType: "text" | "email";
}

const common: FieldDefinition[] = [
  { name: "firstName", label: "First name", inputType: "text" },
  { name: "lastName", label: "Last name", inputType: "text" },
  { name: "email", label: "Email", inputType: "email" },
];

/** Exactly the fields of each registration type (BR-01). */
export const FIELDS: Record<RegistrationType, FieldDefinition[]> = {
  EXTERNAL: [
    ...common,
    { name: "organization", label: "Organization / institution", inputType: "text" },
  ],
  STUDENT: [
    ...common,
    { name: "studyInstitution", label: "Study institution", inputType: "text" },
    { name: "studyProgramme", label: "Study programme", inputType: "text" },
    { name: "studentId", label: "Student ID", inputType: "text" },
  ],
};

/** Option groups in display order (BR-04). */
export const CATEGORIES: { category: OptionCategory; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

/** What the participant has entered so far. */
export interface FormValues {
  type: RegistrationType | "";
  text: Record<TextField, string>;
  options: string[];
  consents: string[];
  captchaToken: string;
}

export const EMPTY_VALUES: FormValues = {
  type: "",
  text: {
    firstName: "",
    lastName: "",
    email: "",
    organization: "",
    studyInstitution: "",
    studyProgramme: "",
    studentId: "",
  },
  options: [],
  consents: [],
  captchaToken: "",
};

/** The active options offered to a registration type, in configuration order (OQ-01). */
export function optionsFor(config: FormConfig, type: RegistrationType): FormOption[] {
  return config.options.filter((o) => o.availableTo.includes(type));
}
