// The registration form (US-001, US-002) and its confirmation (US-004).
import { useEffect, useState, type FormEvent } from "react";
import { getFormConfig, submitRegistration } from "./api";
import { Captcha } from "./Captcha";
import type { FieldError, FormConfig, OptionCategory, RegistrationType } from "./types";
import { TYPE_FIELDS, validate, type FieldName, type FormValues } from "./validation";

export const GENERAL_FAILURE = "Your registration could not be completed. Please try again later.";
const LOAD_FAILURE = "The registration form could not be loaded. Please try again later.";

const LABELS: Record<FieldName, string> = {
  firstName: "First name",
  lastName: "Last name",
  email: "Email",
  organization: "Organization / institution",
  studyInstitution: "Study institution",
  studyProgramme: "Study programme",
  studentId: "Student ID",
};

const GROUPS: { category: OptionCategory; title: string }[] = [
  { category: "WORKSHOP", title: "Workshops" },
  { category: "EVENT", title: "Events" },
  { category: "MEAL", title: "Meals" },
  { category: "OTHER", title: "Other activities" },
];

const EMPTY: FormValues = {
  firstName: "",
  lastName: "",
  email: "",
  organization: "",
  studyInstitution: "",
  studyProgramme: "",
  studentId: "",
};

interface Confirmation {
  name: string;
  options: string[];
}

function ErrorText({ id, message }: { id: string; message?: string }) {
  if (!message) return null;
  return (
    <p id={id} className="field-error">
      {message}
    </p>
  );
}

export function RegistrationPage() {
  const [config, setConfig] = useState<FormConfig | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<FormValues>(EMPTY);
  const [selected, setSelected] = useState<string[]>([]);
  const [given, setGiven] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [token, setToken] = useState("");
  const [resetSignal, setResetSignal] = useState(0);
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);

  useEffect(() => {
    getFormConfig()
      .then(setConfig)
      .catch(() => setLoadFailed(true));
  }, []);

  const available =
    config && type ? config.options.filter((o) => o.availableTo.includes(type)) : [];

  function chooseType(next: RegistrationType) {
    setType(next);
    setErrors({});
    setFormError(null);
    setSelected((current) =>
      current.filter((id) =>
        config?.options.some((o) => o.id === id && o.availableTo.includes(next)),
      ),
    );
  }

  function toggle(list: string[], id: string): string[] {
    return list.includes(id) ? list.filter((x) => x !== id) : [...list, id];
  }

  function showBackendErrors(fieldErrors: FieldError[]) {
    const next: Record<string, string> = {};
    const general: string[] = [];
    for (const { field, message } of fieldErrors) {
      if (field in LABELS || field === "options" || field === "consents") next[field] ??= message;
      else general.push(message);
    }
    setErrors(next);
    setFormError(general.length ? general.join(" ") : null);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!config || !type || submitting) return;
    const clientErrors = validate(type, values, config.consents, new Set(given));
    setErrors(clientErrors);
    setFormError(null);
    if (Object.keys(clientErrors).length > 0) return;
    const value = (field: FieldName) =>
      TYPE_FIELDS[type].includes(field) ? values[field].trim() : null;
    const options = config.options.filter((o) => selected.includes(o.id));
    setSubmitting(true);
    const result = await submitRegistration({
      type,
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim(),
      organization: value("organization"),
      studyInstitution: value("studyInstitution"),
      studyProgramme: value("studyProgramme"),
      studentId: value("studentId"),
      options: options.map((o) => o.id),
      consents: config.consents.filter((c) => given.includes(c.id)).map((c) => c.id),
      captchaToken: token,
    });
    setSubmitting(false);
    setResetSignal((n) => n + 1);
    if (result.kind === "created") {
      setConfirmation({
        name: `${values.firstName.trim()} ${values.lastName.trim()}`,
        options: options.map((o) => o.name),
      });
    } else if (result.kind === "invalid") {
      showBackendErrors(result.errors);
    } else {
      setFormError(GENERAL_FAILURE);
    }
  }

  if (confirmation) {
    return (
      <main>
        <h1>Conference registration</h1>
        <section className="confirmation">
          <h2>Registration received</h2>
          <p>Thank you, {confirmation.name}.</p>
          {confirmation.options.length > 0 ? (
            <ul>
              {confirmation.options.map((name) => (
                <li key={name}>{name}</li>
              ))}
            </ul>
          ) : (
            <p>No options selected.</p>
          )}
        </section>
      </main>
    );
  }

  return (
    <main>
      <h1>Conference registration</h1>
      {loadFailed && <p role="alert">{LOAD_FAILURE}</p>}
      {config && (
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
              {(["firstName", "lastName", "email", ...TYPE_FIELDS[type]] as FieldName[]).map(
                (field) => (
                  <div className="field" key={field}>
                    <label htmlFor={field}>{LABELS[field]}</label>
                    <input
                      id={field}
                      name={field}
                      type={field === "email" ? "email" : "text"}
                      value={values[field]}
                      aria-invalid={errors[field] ? "true" : undefined}
                      aria-describedby={errors[field] ? `${field}-error` : undefined}
                      onChange={(e) => setValues({ ...values, [field]: e.target.value })}
                    />
                    <ErrorText id={`${field}-error`} message={errors[field]} />
                  </div>
                ),
              )}
              <div className="options">
                {GROUPS.map(({ category, title }) => {
                  const inGroup = available.filter((o) => o.category === category);
                  if (inGroup.length === 0) return null;
                  return (
                    <fieldset key={category}>
                      <legend>{title}</legend>
                      {inGroup.map((option) => (
                        <label key={option.id}>
                          <input
                            type="checkbox"
                            checked={selected.includes(option.id)}
                            onChange={() => setSelected(toggle(selected, option.id))}
                          />{" "}
                          {option.name}
                        </label>
                      ))}
                    </fieldset>
                  );
                })}
                <ErrorText id="options-error" message={errors.options} />
              </div>
              <fieldset>
                <legend>Consents</legend>
                {config.consents.map((consent) => (
                  <label key={consent.id}>
                    <input
                      type="checkbox"
                      checked={given.includes(consent.id)}
                      onChange={() => setGiven(toggle(given, consent.id))}
                    />{" "}
                    {consent.text}
                  </label>
                ))}
                <ErrorText id="consents-error" message={errors.consents} />
              </fieldset>
              <Captcha
                siteKey={config.captchaSiteKey}
                testMode={config.captchaTestMode === true}
                onToken={setToken}
                resetSignal={resetSignal}
              />
              {formError && (
                <div role="alert" id="form-error">
                  {formError}
                </div>
              )}
              <button type="submit" disabled={submitting}>
                Register
              </button>
            </>
          )}
        </form>
      )}
    </main>
  );
}
