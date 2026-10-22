// The only way the frontend talks to the backend: the REST API under /api (AR-01).
import type {
  Credentials,
  FieldError,
  FormConfig,
  RegistrationRequest,
  SearchFilters,
  SearchResult,
} from "./types";

export type SubmitResult =
  { kind: "created"; id: string } | { kind: "invalid"; errors: FieldError[] } | { kind: "failed" };

export type SearchOutcome =
  | { kind: "ok"; result: SearchResult }
  | { kind: "unauthorized" }
  | { kind: "invalid"; errors: FieldError[] }
  | { kind: "failed" };

export async function getFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error("form configuration unavailable");
  return (await response.json()) as FormConfig;
}

async function fieldErrors(response: Response): Promise<FieldError[] | null> {
  try {
    const body = (await response.json()) as { errors?: FieldError[] };
    return Array.isArray(body.errors) ? body.errors : null;
  } catch {
    return null;
  }
}

export async function submitRegistration(request: RegistrationRequest): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
  } catch {
    return { kind: "failed" };
  }
  if (response.status === 201) {
    try {
      const body = (await response.json()) as { id: string };
      return { kind: "created", id: body.id };
    } catch {
      return { kind: "failed" };
    }
  }
  if (response.status === 400) {
    const errors = await fieldErrors(response);
    if (errors) return { kind: "invalid", errors };
  }
  return { kind: "failed" };
}

/** HTTP Basic value; UTF-8 safe. */
export function basicAuthorization({ username, password }: Credentials): string {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = "";
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return "Basic " + btoa(binary);
}

export function searchQuery(filters: SearchFilters): string {
  const params = new URLSearchParams();
  if (filters.text.trim()) params.set("text", filters.text);
  if (filters.type) params.set("type", filters.type);
  if (filters.option) params.set("option", filters.option);
  params.set("page", String(filters.page));
  return params.toString();
}

export async function searchRegistrations(
  credentials: Credentials,
  filters: SearchFilters,
): Promise<SearchOutcome> {
  let response: Response;
  try {
    response = await fetch(`/api/registrations?${searchQuery(filters)}`, {
      headers: {
        Accept: "application/json",
        Authorization: basicAuthorization(credentials),
        "X-Requested-With": "XMLHttpRequest",
      },
    });
  } catch {
    return { kind: "failed" };
  }
  if (response.status === 401) return { kind: "unauthorized" };
  if (response.status === 400) {
    const errors = await fieldErrors(response);
    return errors ? { kind: "invalid", errors } : { kind: "failed" };
  }
  if (!response.ok) return { kind: "failed" };
  try {
    return { kind: "ok", result: (await response.json()) as SearchResult };
  } catch {
    return { kind: "failed" };
  }
}
