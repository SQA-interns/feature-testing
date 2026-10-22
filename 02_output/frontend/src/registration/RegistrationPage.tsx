import { type FormEvent, useCallback, useEffect, useState } from "react";
import {
  type ConferenceOption,
  type FormConfig,
  type OptionCategory,
  type RegistrationType,
  fetchFormConfig,
  submitRegistration,
} from "../api";
import { FIELDS, type FieldName, type Values, validate } from "./fields";
import { Recaptcha } from "./Recaptcha";

export const GENERAL_ERROR = "Your registration could not be sent. Please try again later.";
export const TEST_TOKEN = "test-pass";

const GROUPS: { category: OptionCategory; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

interface Confirmed {
  name: string;
  optionNames: string[];
}

function ErrorText({ id, message }: { id: string; message?: string }) {
  return message ? (
    <span id={id} className="field-error">
      {message}
    </span>
  ) : null;
}

export function RegistrationPage() {
  const [config, setConfig] = useState<FormConfig | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Values>({});
  const [options, setOptions] = useState<string[]>([]);
  const [consents, setConsents] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [captchaToken, setCaptchaToken] = useState("");
  const [captchaResets, setCaptchaResets] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [confirmed, setConfirmed] = useState<Confirmed | null>(null);

  useEffect(() => {
    fetchFormConfig()
      .then(setConfig)
      .catch(() => setLoadFailed(true));
  }, []);

  const onToken = useCallback((token: string) => setCaptchaToken(token), []);

  if (confirmed) {
    return (
      <section aria-labelledby="confirmation-heading">
        <h2 id="confirmation-heading">Registration received</h2>
        <p>Thank you, {confirmed.name}.</p>
        {confirmed.optionNames.length > 0 ? (
          <ul>
            {confirmed.optionNames.map((name) => (
              <li key={name}>{name}</li>
            ))}
          </ul>
        ) : (
          <p>No options selected</p>
        )}
      </section>
    );
  }
  if (loadFailed) {
    return <p role="alert">The registration form could not be loaded. Please try again later.</p>;
  }
  if (!config) {
    return <p>Loading…</p>;
  }

  const offered = (option: ConferenceOption) => type !== null && option.availableTo.includes(type);
  const testMode = config.captchaTestMode === true;

  function chooseType(next: RegistrationType) {
    setType(next);
    setErrors({});
    setOptions((selected) =>
      selected.filter((id) =>
        config?.options.some((o) => o.id === id && o.availableTo.includes(next)),
      ),
    );
  }

  function change(name: FieldName, value: string) {
    setValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => {
      const next = { ...current };
      delete next[name];
      return next;
    });
  }

  function toggle(list: string[], id: string): string[] {
    return list.includes(id) ? list.filter((x) => x !== id) : [...list, id];
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!config || !type) return;
    setGeneralError(null);
    const clientErrors = validate(type, values, consents, config.consents);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }
    const value = (name: FieldName) =>
      FIELDS[type].some((f) => f.name === name) ? (values[name] ?? "").trim() : null;
    const selected = config.options.filter((o) => options.includes(o.id) && offered(o));
    setSubmitting(true);
    const result = await submitRegistration({
      type,
      firstName: value("firstName") ?? "",
      lastName: value("lastName") ?? "",
      email: value("email") ?? "",
      organization: value("organization"),
      studyInstitution: value("studyInstitution"),
      studyProgramme: value("studyProgramme"),
      studentId: value("studentId"),
      options: selected.map((o) => o.id),
      consents: config.consents.filter((c) => consents.includes(c.id)).map((c) => c.id),
      captchaToken: testMode ? TEST_TOKEN : captchaToken,
    });
    setSubmitting(false);
    if (result.kind === "created") {
      setConfirmed({
        name: `${value("firstName")} ${value("lastName")}`,
        optionNames: selected.map((o) => o.name),
      });
      return;
    }
    if (!testMode) setCaptchaResets((n) => n + 1);
    if (result.kind === "rejected") {
      const shown = new Set<string>([
        ...FIELDS[type].map((f) => f.name),
        "options",
        "consents",
        "captchaToken",
      ]);
      const byField: Record<string, string> = {};
      const other: string[] = [];
      for (const error of result.errors) {
        if (shown.has(error.field)) byField[error.field] ??= error.message;
        else other.push(error.message);
      }
      setErrors(byField);
      setGeneralError(other.length > 0 ? other.join(" ") : null);
      return;
    }
    setGeneralError(GENERAL_ERROR);
  }

  return (
    <form noValidate onSubmit={submit} aria-label="Registration">
      <fieldset>
        <legend>Registration type</legend>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "EXTERNAL"}
            onChange={() => chooseType("EXTERNAL")}
          />
          External participant
        </label>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "STUDENT"}
            onChange={() => chooseType("STUDENT")}
          />
          Student
        </label>
      </fieldset>

      {type && (
        <>
          {FIELDS[type].map((field) => (
            <div className="field" key={field.name}>
              <label htmlFor={field.name}>{field.label}</label>
              <input
                id={field.name}
                name={field.name}
                type={field.inputType}
                value={values[field.name] ?? ""}
                aria-invalid={errors[field.name] ? true : undefined}
                aria-describedby={errors[field.name] ? `${field.name}-error` : undefined}
                onChange={(e) => change(field.name, e.target.value)}
              />
              <ErrorText id={`${field.name}-error`} message={errors[field.name]} />
            </div>
          ))}

          {GROUPS.map(({ category, legend }) => {
            const inGroup = config.options.filter((o) => o.category === category && offered(o));
            if (inGroup.length === 0) return null;
            return (
              <fieldset key={category}>
                <legend>{legend}</legend>
                {inGroup.map((option) => (
                  <label key={option.id} className="choice">
                    <input
                      type="checkbox"
                      checked={options.includes(option.id)}
                      aria-describedby={errors.options ? "options-error" : undefined}
                      onChange={() => setOptions((list) => toggle(list, option.id))}
                    />
                    {option.name}
                  </label>
                ))}
              </fieldset>
            );
          })}
          <ErrorText id="options-error" message={errors.options} />

          <fieldset>
            <legend>Consents</legend>
            {config.consents.map((consent) => (
              <label key={consent.id} className="choice">
                <input
                  type="checkbox"
                  checked={consents.includes(consent.id)}
                  aria-describedby={errors.consents ? "consents-error" : undefined}
                  onChange={() => setConsents((list) => toggle(list, consent.id))}
                />
                {consent.text}
                {consent.mandatory ? " (required)" : ""}
              </label>
            ))}
            <ErrorText id="consents-error" message={errors.consents} />
          </fieldset>

          <div className="field">
            {testMode ? (
              <p>reCAPTCHA test mode</p>
            ) : (
              <Recaptcha
                siteKey={config.captchaSiteKey}
                onToken={onToken}
                resetCount={captchaResets}
              />
            )}
            <ErrorText id="captchaToken-error" message={errors.captchaToken} />
          </div>

          <button type="submit" disabled={submitting}>
            Register
          </button>
        </>
      )}
      {generalError && <p role="alert">{generalError}</p>}
    </form>
  );
}
