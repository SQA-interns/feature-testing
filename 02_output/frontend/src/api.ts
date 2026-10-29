import type {
  Credentials,
  FieldError,
  FormConfig,
  RegistrationAccepted,
  RegistrationRequest,
  SearchQuery,
  SearchResult,
} from "./types";

/** The only module that talks to the backend, and only through relative /api paths (AR-01). */
export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error("form configuration unavailable");
  }
  return (await response.json()) as FormConfig;
}

export type SubmitResult =
  | { kind: "accepted"; registration: RegistrationAccepted }
  | { kind: "rejected"; fieldErrors: FieldError[] }
  | { kind: "failed" };

export async function submitRegistration(
  request: RegistrationRequest,
): Promise<SubmitResult> {
  try {
    const response = await fetch("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(request),
    });
    if (response.status === 201) {
      return {
        kind: "accepted",
        registration: (await response.json()) as RegistrationAccepted,
      };
    }
    if (response.status === 400) {
      const body = (await response.json()) as { fieldErrors?: FieldError[] };
      if (Array.isArray(body.fieldErrors) && body.fieldErrors.length > 0) {
        return { kind: "rejected", fieldErrors: body.fieldErrors };
      }
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}

export type SearchOutcome =
  | { kind: "ok"; result: SearchResult }
  | { kind: "unauthorized" }
  | { kind: "rejected"; fieldErrors: FieldError[] }
  | { kind: "failed" };

/** HTTP Basic with the credentials as UTF-8 (SR-09: they are only passed in, never stored). */
export function basicAuthorization({
  username,
  password,
}: Credentials): string {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  return "Basic " + btoa(String.fromCharCode(...bytes));
}

/** The organizer search (openapi.yaml searchRegistrations); empty values are not sent. */
export async function searchRegistrations(
  credentials: Credentials,
  query: SearchQuery,
): Promise<SearchOutcome> {
  const params = new URLSearchParams();
  for (const [name, value] of Object.entries(query)) {
    if (value !== "") {
      params.set(name, value);
    }
  }
  const encoded = params.toString();
  const suffix = encoded === "" ? "" : `?${encoded}`;
  try {
    const response = await fetch(`/api/organizer/registrations${suffix}`, {
      headers: {
        Accept: "application/json",
        Authorization: basicAuthorization(credentials),
      },
    });
    if (response.status === 200) {
      return { kind: "ok", result: (await response.json()) as SearchResult };
    }
    if (response.status === 401) {
      return { kind: "unauthorized" };
    }
    if (response.status === 400) {
      const body = (await response.json()) as { fieldErrors?: FieldError[] };
      if (Array.isArray(body.fieldErrors) && body.fieldErrors.length > 0) {
        return { kind: "rejected", fieldErrors: body.fieldErrors };
      }
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
