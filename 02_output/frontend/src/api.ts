// The only module that talks to the backend, and only under /api (AR-01).
import type { FieldError, FormConfig, RegistrationRequest } from "./types";

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
    if (response.status === 400) {
      const body = (await response.json()) as { errors?: unknown };
      const errors = Array.isArray(body.errors) ? body.errors.filter(isFieldError) : [];
      return errors.length > 0 ? { kind: "rejected", errors } : { kind: "failed" };
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
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
