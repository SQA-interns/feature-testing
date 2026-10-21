import { useState, type FormEvent } from "react";
import { submitRegistration } from "../api/client";
import { buildPayload } from "../domain/payload";
import {
  EMPTY_VALUES,
  FIELDS,
  optionsFor,
  type FieldError,
  type FormConfig,
  type FormValues,
  type RegistrationType,
  type TextField,
} from "../domain/types";
import { validate } from "../domain/validation";
import { Captcha } from "./Captcha";
import { OptionGroups } from "./OptionGroups";

/** What the confirmation shows after HTTP 201. */
export interface Registered {
  firstName: string;
  lastName: string;
  optionNames: string[];
}

interface Props {
  config: FormConfig;
  onRegistered: (registered: Registered) => void;
}

const TYPES: { value: RegistrationType; label: string }[] = [
  { value: "EXTERNAL", label: "External participant" },
  { value: "STUDENT", label: "Student" },
];

const ERROR_FIELDS = new Set([
  "type",
  "firstName",
  "lastName",
  "email",
  "organization",
  "studyInstitution",
  "studyProgramme",
  "studentId",
  "options",
  "consents",
  "captchaToken",
]);

export const GENERAL_ERROR = "The registration could not be completed. Please try again later.";

/** The backend's field errors that belong to a form field, first message per field. */
function fieldErrors(list: FieldError[]): Record<string, string> {
  const shown: Record<string, string> = {};
  for (const { field, message } of list) {
    if (ERROR_FIELDS.has(field) && !(field in shown)) shown[field] = message;
  }
  return shown;
}

function toggle(list: string[], id: string): string[] {
  return list.includes(id) ? list.filter((x) => x !== id) : [...list, id];
}

/** The registration form of `ui-registration-form.json`. */
export function RegistrationForm({ config, onRegistered }: Props) {
  const [values, setValues] = useState<FormValues>(EMPTY_VALUES);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [captchaResets, setCaptchaResets] = useState(0);
  const [generalError, setGeneralError] = useState(false);

  const type = values.type;
  const available = type === "" ? [] : optionsFor(config, type);

  const chooseType = (next: RegistrationType) => {
    const offered = optionsFor(config, next).map((o) => o.id);
    setValues((v) => ({
      ...v,
      type: next,
      options: v.options.filter((id) => offered.includes(id)),
    }));
  };

  const setText = (field: TextField, value: string) =>
    setValues((v) => ({ ...v, text: { ...v.text, [field]: value } }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (type === "" || submitting) return;
    const found = validate(values, config);
    setErrors(found);
    setGeneralError(false);
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    const result = await submitRegistration(buildPayload({ ...values, type }, config));
    setSubmitting(false);
    if (result.kind === "created") {
      onRegistered({
        firstName: values.text.firstName.trim(),
        lastName: values.text.lastName.trim(),
        optionNames: available.filter((o) => values.options.includes(o.id)).map((o) => o.name),
      });
      return;
    }
    if (result.kind === "invalid") {
      const shown = fieldErrors(result.errors);
      setErrors(shown);
      setGeneralError(Object.keys(shown).length < result.errors.length);
    } else {
      setGeneralError(true);
    }
    setCaptchaResets((n) => n + 1);
  };

  return (
    <form noValidate onSubmit={submit}>
      <div role="radiogroup" aria-labelledby="type-label" className="types">
        <span id="type-label" className="group-label">
          Registration type
        </span>
        {TYPES.map((t) => (
          <label key={t.value} className="choice">
            <input
              type="radio"
              name="type"
              value={t.value}
              checked={type === t.value}
              onChange={() => chooseType(t.value)}
            />
            {t.label}
          </label>
        ))}
        {errors.type && (
          <span id="type-error" className="error">
            {errors.type}
          </span>
        )}
      </div>

      {type !== "" && (
        <>
          {FIELDS[type].map((field) => (
            <div key={field.name} className="field">
              <label htmlFor={`field-${field.name}`}>{field.label}</label>
              <input
                id={`field-${field.name}`}
                name={field.name}
                type={field.inputType}
                value={values.text[field.name]}
                onChange={(e) => setText(field.name, e.target.value)}
                aria-invalid={errors[field.name] ? "true" : undefined}
                aria-describedby={errors[field.name] ? `${field.name}-error` : undefined}
              />
              {errors[field.name] && (
                <span id={`${field.name}-error`} className="error">
                  {errors[field.name]}
                </span>
              )}
            </div>
          ))}

          <OptionGroups
            options={available}
            selected={values.options}
            error={errors.options}
            onToggle={(id) => setValues((v) => ({ ...v, options: toggle(v.options, id) }))}
          />

          <fieldset
            className="consents"
            aria-describedby={errors.consents ? "consents-error" : undefined}
          >
            <legend>Consents</legend>
            {config.consents.map((consent) => (
              <label key={consent.id} className="choice">
                <input
                  type="checkbox"
                  name="consents"
                  value={consent.id}
                  checked={values.consents.includes(consent.id)}
                  onChange={() =>
                    setValues((v) => ({ ...v, consents: toggle(v.consents, consent.id) }))
                  }
                />
                {consent.text}
              </label>
            ))}
            {errors.consents && (
              <span id="consents-error" className="error">
                {errors.consents}
              </span>
            )}
          </fieldset>

          <Captcha
            testMode={config.captchaTestMode}
            siteKey={config.captchaSiteKey}
            token={values.captchaToken}
            resetCount={captchaResets}
            error={errors.captchaToken}
            onToken={(token) => setValues((v) => ({ ...v, captchaToken: token }))}
          />

          <button type="submit" disabled={submitting}>
            Register
          </button>
          {generalError && (
            <p role="alert" id="general-error" className="general-error">
              {GENERAL_ERROR}
            </p>
          )}
        </>
      )}
    </form>
  );
}
