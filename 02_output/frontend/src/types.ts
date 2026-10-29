/** Shapes of docs/02_contracts/openapi.yaml. */
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
  conferenceName: string;
  captcha: { mode: "RECAPTCHA" | "TEST"; siteKey?: string };
  options: FormOption[];
  consents: FormConsent[];
}

export interface FieldError {
  field: string;
  code: string;
  message: string;
}

export interface RegistrationAccepted {
  id: string;
  registeredAt: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  options: { id: string; name: string; category: OptionCategory }[];
}

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export interface RegistrationRequest {
  type: RegistrationType;
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
  optionIds: string[];
  consents: string[];
  recaptchaToken: string;
}

/** GET /api/organizer/registrations (openapi.yaml SearchResult, CR-001). */
export interface SearchRegistration {
  id: string;
  type: RegistrationType;
  registeredAt: string;
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
  options: { id: string; name: string; category: OptionCategory }[];
  consents: { id: string; givenAt: string }[];
}

export interface CategoryCounts {
  category: OptionCategory;
  options: { id: string; name: string; active: boolean; count: number }[];
}

export interface SearchResult {
  count: number;
  registrations: SearchRegistration[];
  optionCounts: CategoryCounts[];
}

/** The organizer's search; an empty value restricts nothing. */
export interface SearchQuery {
  search: string;
  type: string;
  workshop: string;
  event: string;
  meal: string;
  other: string;
}

export interface Credentials {
  username: string;
  password: string;
}
