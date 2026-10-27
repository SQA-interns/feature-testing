import type {
  FieldError,
  FormConfig,
  RegistrationAccepted,
  RegistrationRequest,
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
