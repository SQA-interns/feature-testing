// Shapes of the REST API (docs/02_contracts/openapi.json).

export type RegistrationType = "EXTERNAL" | "STUDENT";
export type OptionCategory = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface ConferenceOption {
  id: string;
  name: string;
  category: OptionCategory;
  availableTo: RegistrationType[];
}

export interface Consent {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  options: ConferenceOption[];
  consents: Consent[];
  captchaSiteKey: string;
  captchaTestMode?: boolean;
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

export interface FieldError {
  field: string;
  message: string;
}

export interface SearchItem {
  id: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  email: string;
  organization: string | null;
  studyInstitution: string | null;
  options: string[];
  registeredAt: string;
}

export interface SearchResult {
  total: number;
  page: number;
  items: SearchItem[];
}

export interface SearchFilters {
  text: string;
  type: "" | RegistrationType;
  option: string;
  page: number;
}

export interface Credentials {
  username: string;
  password: string;
}
