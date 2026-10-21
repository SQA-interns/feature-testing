import { FIELDS, type FormConfig, type FormValues, type RegistrationType } from "./types";

/** The request body of `POST /api/registrations`: trimmed values of the chosen type's fields. */
export function buildPayload(
  values: FormValues & { type: RegistrationType },
  config: FormConfig,
): Record<string, unknown> {
  const payload: Record<string, unknown> = { type: values.type };
  for (const field of FIELDS[values.type]) {
    payload[field.name] = values.text[field.name].trim();
  }
  payload.options = config.options.map((o) => o.id).filter((id) => values.options.includes(id));
  payload.consents = config.consents.map((c) => c.id).filter((id) => values.consents.includes(id));
  payload.captchaToken = values.captchaToken;
  return payload;
}
