import { useState, type FormEvent } from "react";
import { submitRegistration } from "../api";
import type {
  FieldErrors,
  FormConfig,
  RegistrationRequest,
  RegistrationType,
  TextField,
} from "../types";
import { fieldsOf, validate } from "../validation";
import { Captcha } from "./Captcha";
import type { Confirmed } from "./Confirmation";
import { OptionGroups } from "./OptionGroups";
import { TextInput } from "./TextInput";

export const GENERAL_ERROR = "Registration could not be completed. Please try again later.";

const LABELS: Record<TextField, string> = {
  firstName: "First name",
  lastName: "Last name",
  email: "Email",
  organization: "Organization / institution",
  studyInstitution: "Study institution",
  studyProgramme: "Study programme",
  studentId: "Student ID",
};

const SHOWN_FIELDS = new Set<string>([
  ...Object.keys(LABELS),
  "options",
  "consents",
  "captchaToken",
]);

interface RegistrationFormProps {
  config: FormConfig;
  onRegistered: (confirmed: Confirmed) => void;
}

/** The registration form (interfaces.md "Registration form", US-001, US-002, US-004). */
export function RegistrationForm({ config, onRegistered }: RegistrationFormProps) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [options, setOptions] = useState<ReadonlySet<string>>(new Set());
  const [consents, setConsents] = useState<ReadonlySet<string>>(new Set());
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [captchaReset, setCaptchaReset] = useState(0);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const available = type ? config.options.filter((o) => o.availableTo.includes(type)) : [];

  function chooseType(next: RegistrationType) {
    setType(next);
    setErrors({});
    setGeneralError(null);
    setOptions(
      (current) =>
        new Set(
          [...current].filter((id) =>
            config.options.some((o) => o.id === id && o.availableTo.includes(next)),
          ),
        ),
    );
  }

  function toggle(set: ReadonlySet<string>, id: string): ReadonlySet<string> {
    const next = new Set(set);
    if (!next.delete(id)) {
      next.add(id);
    }
    return next;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!type || submitting) return;
    const found = validate({ type, values, consents, captchaToken }, config.consents);
    setErrors(found);
    setGeneralError(null);
    if (Object.keys(found).length > 0) return;

    const text = (field: TextField) =>
      fieldsOf(type).includes(field) ? (values[field] ?? "") : null;
    const request: RegistrationRequest = {
      type,
      firstName: values.firstName ?? "",
      lastName: values.lastName ?? "",
      email: values.email ?? "",
      organization: text("organization"),
      studyInstitution: text("studyInstitution"),
      studyProgramme: text("studyProgramme"),
      studentId: text("studentId"),
      options: available.filter((o) => options.has(o.id)).map((o) => o.id),
      consents: config.consents.filter((c) => consents.has(c.id)).map((c) => c.id),
      captchaToken: captchaToken ?? "",
    };
    setSubmitting(true);
    const result = await submitRegistration(request);
    setSubmitting(false);
    if (result.kind === "created") {
      onRegistered({
        firstName: request.firstName.trim(),
        lastName: request.lastName.trim(),
        optionNames: available.filter((o) => options.has(o.id)).map((o) => o.name),
      });
      return;
    }
    if (!config.captchaTestMode) {
      setCaptchaToken(null);
      setCaptchaReset((n) => n + 1);
    }
    if (result.kind === "rejected") {
      const byField: FieldErrors = {};
      const other: string[] = [];
      for (const error of result.errors) {
        if (SHOWN_FIELDS.has(error.field)) {
          byField[error.field] = error.message;
        } else {
          other.push(error.message);
        }
      }
      setErrors(byField);
      setGeneralError(other.length > 0 ? other.join(" ") : null);
      return;
    }
    setGeneralError(GENERAL_ERROR);
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <fieldset>
        <legend>Registration type</legend>
        {(
          [
            ["EXTERNAL", "External participant"],
            ["STUDENT", "Student"],
          ] as const
        ).map(([value, label]) => (
          <div className="choice" key={value}>
            <input
              type="radio"
              id={`type-${value}`}
              name="type"
              checked={type === value}
              onChange={() => chooseType(value)}
            />
            <label htmlFor={`type-${value}`}>{label}</label>
          </div>
        ))}
      </fieldset>

      {type && (
        <>
          {fieldsOf(type).map((field) => (
            <TextInput
              key={field}
              id={field}
              label={LABELS[field]}
              type={field === "email" ? "email" : "text"}
              value={values[field] ?? ""}
              error={errors[field]}
              onChange={(value) => setValues((current) => ({ ...current, [field]: value }))}
            />
          ))}

          <OptionGroups
            options={available}
            selected={options}
            error={errors.options}
            onToggle={(id) => setOptions((current) => toggle(current, id))}
          />

          <fieldset>
            <legend>Consents</legend>
            {config.consents.map((consent) => (
              <div className="choice" key={consent.id}>
                <input
                  type="checkbox"
                  id={`consent-${consent.id}`}
                  checked={consents.has(consent.id)}
                  onChange={() => setConsents((current) => toggle(current, consent.id))}
                />
                <label htmlFor={`consent-${consent.id}`}>{consent.text}</label>
              </div>
            ))}
            {errors.consents && (
              <p id="consents-error" role="alert" className="error">
                {errors.consents}
              </p>
            )}
          </fieldset>

          <Captcha
            siteKey={config.captchaSiteKey}
            testMode={config.captchaTestMode === true}
            checked={captchaToken !== null}
            error={errors.captchaToken}
            resetSignal={captchaReset}
            onToken={setCaptchaToken}
          />

          {generalError && (
            <p role="alert" className="error general">
              {generalError}
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
