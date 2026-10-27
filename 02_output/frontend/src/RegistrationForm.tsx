import { useCallback, useState, type FormEvent } from "react";
import { submitRegistration } from "./api";
import { Captcha } from "./Captcha";
import { Confirmation } from "./Confirmation";
import { MESSAGES, messageFor } from "./messages";
import type {
  FormConfig,
  OptionCategory,
  RegistrationAccepted,
  RegistrationType,
  TextField,
} from "./types";
import { FIELDS, FIELDS_BY_TYPE, validate } from "./validation";

const CATEGORIES: [OptionCategory, string][] = [
  ["WORKSHOP", "Workshops"],
  ["EVENT", "Events"],
  ["MEAL", "Meals"],
  ["OTHER", "Other activities"],
];

/** The registration form of registration-form-ui.md. */
export function RegistrationForm({ config }: { config: FormConfig }) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [selected, setSelected] = useState<string[]>([]);
  const [consents, setConsents] = useState<string[]>([]);
  const [token, setToken] = useState("");
  const [captchaReset, setCaptchaReset] = useState(0);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [accepted, setAccepted] = useState<RegistrationAccepted | null>(null);
  const onToken = useCallback((value: string) => setToken(value), []);

  if (accepted) {
    return <Confirmation registration={accepted} />;
  }

  const chooseType = (next: RegistrationType) => {
    setType(next);
    setErrors({});
    setGeneralError(false);
    setSelected((ids) =>
      ids.filter((id) =>
        config.options.some((o) => o.id === id && o.availableTo.includes(next)),
      ),
    );
  };

  const toggle = (list: string[], id: string) =>
    list.includes(id) ? list.filter((x) => x !== id) : [...list, id];

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!type) {
      return;
    }
    const localErrors = validate(
      { type, values, consents, captchaToken: token },
      config.consents,
    );
    setErrors(localErrors);
    setGeneralError(false);
    if (Object.keys(localErrors).length > 0) {
      return;
    }
    setSubmitting(true);
    const fields = Object.fromEntries(
      FIELDS_BY_TYPE[type].map((f) => [f, values[f] ?? ""]),
    );
    const result = await submitRegistration({
      type,
      firstName: values.firstName ?? "",
      lastName: values.lastName ?? "",
      email: values.email ?? "",
      ...fields,
      optionIds: config.options
        .filter((o) => selected.includes(o.id))
        .map((o) => o.id),
      consents: config.consents
        .filter((c) => consents.includes(c.id))
        .map((c) => c.id),
      recaptchaToken: token,
    });
    setSubmitting(false);
    if (result.kind === "accepted") {
      setAccepted(result.registration);
      return;
    }
    if (config.captcha.mode === "RECAPTCHA") {
      setToken("");
      setCaptchaReset((n) => n + 1);
    }
    if (result.kind === "rejected") {
      const shown = new Set<string>([
        ...FIELDS_BY_TYPE[type],
        "optionIds",
        "recaptchaToken",
        ...config.consents.map((c) => `consents.${c.id}`),
      ]);
      const backendErrors: Record<string, string> = {};
      for (const e of result.fieldErrors) {
        backendErrors[e.field] = messageFor(e.field, e.code);
      }
      setErrors(backendErrors);
      setGeneralError(result.fieldErrors.some((e) => !shown.has(e.field)));
    } else {
      setGeneralError(true);
    }
  };

  return (
    <form noValidate onSubmit={onSubmit}>
      <fieldset>
        <legend>Registration type</legend>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "EXTERNAL"}
            onChange={() => chooseType("EXTERNAL")}
          />{" "}
          External participant
        </label>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "STUDENT"}
            onChange={() => chooseType("STUDENT")}
          />{" "}
          Student
        </label>
      </fieldset>

      {type && (
        <>
          {FIELDS_BY_TYPE[type].map((field) => (
            <div className="field" key={field}>
              <label htmlFor={`field-${field}`}>{FIELDS[field].label}</label>
              <input
                id={`field-${field}`}
                name={field}
                type={field === "email" ? "email" : "text"}
                value={values[field] ?? ""}
                onChange={(e) =>
                  setValues((v) => ({ ...v, [field]: e.target.value }))
                }
                aria-invalid={errors[field] ? "true" : undefined}
                aria-describedby={errors[field] ? `${field}-error` : undefined}
              />
              {errors[field] && (
                <p id={`${field}-error`} className="error">
                  {errors[field]}
                </p>
              )}
            </div>
          ))}

          <div className="options">
            {CATEGORIES.map(([category, legend]) => {
              const options = config.options.filter(
                (o) => o.category === category && o.availableTo.includes(type),
              );
              return options.length === 0 ? null : (
                <fieldset key={category}>
                  <legend>{legend}</legend>
                  {options.map((o) => (
                    <label key={o.id}>
                      <input
                        type="checkbox"
                        checked={selected.includes(o.id)}
                        onChange={() => setSelected((ids) => toggle(ids, o.id))}
                      />{" "}
                      {o.name}
                    </label>
                  ))}
                </fieldset>
              );
            })}
            {errors.optionIds && (
              <p id="optionIds-error" className="error">
                {errors.optionIds}
              </p>
            )}
          </div>

          <fieldset>
            <legend>Consents</legend>
            {config.consents.map((c) => {
              const key = `consents.${c.id}`;
              return (
                <div className="field" key={c.id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={consents.includes(c.id)}
                      onChange={() => setConsents((ids) => toggle(ids, c.id))}
                      aria-invalid={errors[key] ? "true" : undefined}
                      aria-describedby={
                        errors[key] ? `consent-${c.id}-error` : undefined
                      }
                    />{" "}
                    {c.text}
                    {c.mandatory && " (required)"}
                  </label>
                  {errors[key] && (
                    <p id={`consent-${c.id}-error`} className="error">
                      {errors[key]}
                    </p>
                  )}
                </div>
              );
            })}
          </fieldset>

          <Captcha
            mode={config.captcha.mode}
            siteKey={config.captcha.siteKey}
            token={token}
            onToken={onToken}
            resetSignal={captchaReset}
            error={errors.recaptchaToken}
          />

          {generalError && (
            <p role="alert" className="alert">
              {MESSAGES.general}
            </p>
          )}

          <button type="submit" disabled={submitting}>
            Register
          </button>
        </>
      )}
    </form>
  );
}
