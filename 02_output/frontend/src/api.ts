// The only module that talks to the backend, and only under /api (AR-01).
import type {
  Credentials,
  FieldError,
  FormConfig,
  RegistrationRequest,
  SearchQuery,
  SearchResult,
} from "./types";

export type SubmitResult =
  { kind: "created"; id: string } | { kind: "rejected"; errors: FieldError[] } | { kind: "failed" };

export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error("form configuration unavailable");
  }
  return (await response.json()) as FormConfig;
}

/** Sends a registration; any outcome other than 201 or 400 with field errors is "failed". */
export async function submitRegistration(request: RegistrationRequest): Promise<SubmitResult> {
  try {
    const response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
    if (response.status === 201) {
      const body = (await response.json()) as { id?: unknown };
      return typeof body.id === "string" ? { kind: "created", id: body.id } : { kind: "failed" };
    }
    return response.status === 400 ? await rejection(response) : { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}

export type SearchOutcome =
  | { kind: "found"; result: SearchResult }
  | { kind: "unauthorized" }
  | { kind: "forbidden" }
  | { kind: "rejected"; errors: FieldError[] }
  | { kind: "failed" };

/**
 * The organizer's search (US-009): GET /api/registrations with HTTP Basic credentials. The
 * X-Requested-With header keeps the browser from opening its own login dialog on a 401.
 */
export async function searchRegistrations(
  credentials: Credentials,
  query: SearchQuery,
  page: number,
): Promise<SearchOutcome> {
  const params = new URLSearchParams();
  if (query.text.trim() !== "") params.set("text", query.text.trim());
  if (query.type !== "") params.set("type", query.type);
  if (query.option !== "") params.set("option", query.option);
  params.set("page", String(page));
  try {
    const response = await fetch(`/api/registrations?${params.toString()}`, {
      headers: {
        Accept: "application/json",
        Authorization: basicAuthorization(credentials),
        "X-Requested-With": "XMLHttpRequest",
      },
    });
    if (response.status === 200) {
      const body = (await response.json()) as Partial<SearchResult>;
      return typeof body.total === "number" && Array.isArray(body.items)
        ? {
            kind: "found",
            result: { total: body.total, page: body.page ?? page, items: body.items },
          }
        : { kind: "failed" };
    }
    if (response.status === 401) return { kind: "unauthorized" };
    if (response.status === 403) return { kind: "forbidden" };
    return response.status === 400 ? await rejection(response) : { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}

/** "Basic " + base64 of the UTF-8 bytes of "username:password". */
export function basicAuthorization({ username, password }: Credentials): string {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = "";
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return "Basic " + btoa(binary);
}

/** A 400 answer with field errors is "rejected"; without them it is "failed". */
async function rejection(
  response: Response,
): Promise<{ kind: "rejected"; errors: FieldError[] } | { kind: "failed" }> {
  const body = (await response.json()) as { errors?: unknown };
  const errors = Array.isArray(body.errors) ? body.errors.filter(isFieldError) : [];
  return errors.length > 0 ? { kind: "rejected", errors } : { kind: "failed" };
}

function isFieldError(value: unknown): value is FieldError {
  const candidate = value as FieldError | null;
  return (
    typeof candidate === "object" &&
    candidate !== null &&
    typeof candidate.field === "string" &&
    typeof candidate.message === "string"
  );
}
