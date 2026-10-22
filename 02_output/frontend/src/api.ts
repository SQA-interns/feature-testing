// The REST API of api-contract.md, used only under /api (AR-01).

export type RegistrationType = "EXTERNAL" | "STUDENT";
export type OptionCategory = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface ConferenceOption {
  id: string;
  name: string;
  category: OptionCategory;
  availableTo: RegistrationType[];
}

export interface ConsentDefinition {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  options: ConferenceOption[];
  consents: ConsentDefinition[];
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

export type SubmitResult =
  { kind: "created"; id: string } | { kind: "rejected"; errors: FieldError[] } | { kind: "failed" };

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

export interface SearchQuery {
  text: string;
  type: string;
  option: string;
  page: number;
}

export interface Credentials {
  username: string;
  password: string;
}

export type SearchOutcome =
  | { kind: "ok"; result: SearchResult }
  | { kind: "unauthorized" }
  | { kind: "rejected"; errors: FieldError[] }
  | { kind: "failed" };

export const PAGE_SIZE = 20;

export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error("form configuration unavailable");
  }
  return (await response.json()) as FormConfig;
}

async function errorsOf(response: Response): Promise<FieldError[]> {
  try {
    const body = (await response.json()) as { errors?: FieldError[] };
    return Array.isArray(body.errors) ? body.errors : [];
  } catch {
    return [];
  }
}

export async function submitRegistration(request: RegistrationRequest): Promise<SubmitResult> {
  try {
    const response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
    if (response.status === 201) {
      const body = (await response.json()) as { id: string };
      return { kind: "created", id: body.id };
    }
    if (response.status === 400) {
      return { kind: "rejected", errors: await errorsOf(response) };
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}

/** HTTP Basic value for UTF-8 credentials. */
export function basicAuth({ username, password }: Credentials): string {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return "Basic " + btoa(binary);
}

export async function searchRegistrations(
  credentials: Credentials,
  query: SearchQuery,
): Promise<SearchOutcome> {
  const params = new URLSearchParams();
  if (query.text.trim() !== "") params.set("text", query.text);
  if (query.type !== "") params.set("type", query.type);
  if (query.option !== "") params.set("option", query.option);
  params.set("page", String(query.page));
  try {
    const response = await fetch(`/api/registrations?${params.toString()}`, {
      headers: {
        Accept: "application/json",
        Authorization: basicAuth(credentials),
        "X-Requested-With": "XMLHttpRequest",
      },
    });
    if (response.status === 200) {
      return { kind: "ok", result: (await response.json()) as SearchResult };
    }
    if (response.status === 401) {
      return { kind: "unauthorized" };
    }
    if (response.status === 400) {
      return { kind: "rejected", errors: await errorsOf(response) };
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
