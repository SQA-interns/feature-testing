// Shapes of the REST API (docs/02_contracts/openapi.yaml).

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
  captchaTestMode?: boolean;
  conferenceName?: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface RegistrationRequest {
  type: RegistrationType;
  firstName: string;
  lastName: string;
  email: string;
  organization: string | null;
  studyInstitution: string | null;
  studyProgramme: string | null;
  studentId: string | null;
  options: string[];
  consents: string[];
  captchaToken: string;
}

/** Text fields of the form, by request field name. */
export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export type FieldErrors = Partial<Record<string, string>>;
