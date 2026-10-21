import type { FieldError, FormConfig } from "../domain/types";

/** The outcome of a submission as the form needs it. */
export type SubmitResult =
  { kind: "created"; id: string } | { kind: "invalid"; errors: FieldError[] } | { kind: "failed" };

/** `GET /api/form-config` on the frontend's own origin (AR-01). */
export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error(`Form configuration answered ${response.status}`);
  return (await response.json()) as FormConfig;
}

function isFieldErrorList(value: unknown): value is FieldError[] {
  return (
    Array.isArray(value) &&
    value.every(
      (e) =>
        typeof e === "object" &&
        e !== null &&
        typeof (e as FieldError).field === "string" &&
        typeof (e as FieldError).message === "string",
    )
  );
}

/** `POST /api/registrations`; every answer other than 201 or a well-formed 400 is a failure. */
export async function submitRegistration(payload: Record<string, unknown>): Promise<SubmitResult> {
  try {
    const response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
    });
    if (response.status === 201) {
      const body = (await response.json()) as { id?: unknown };
      return { kind: "created", id: String(body.id ?? "") };
    }
    if (response.status === 400) {
      const body = (await response.json()) as { errors?: unknown };
      if (isFieldErrorList(body.errors) && body.errors.length > 0) {
        return { kind: "invalid", errors: body.errors };
      }
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
