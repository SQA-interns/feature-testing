import { useEffect, useRef } from "react";

declare global {
  interface Window {
    grecaptcha?: {
      ready(callback: () => void): void;
      render(element: HTMLElement, parameters: Record<string, unknown>): number;
      reset(widgetId?: number): void;
    };
    onRecaptchaLoad?: () => void;
  }
}

export const TEST_TOKEN = "test-pass";
const SCRIPT_URL =
  "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit";

interface Props {
  mode: "RECAPTCHA" | "TEST";
  siteKey?: string;
  token: string;
  onToken: (token: string) => void;
  /** Incremented to ask the Google widget for a fresh challenge. */
  resetSignal: number;
  error?: string;
}

/** The captcha of registration-form-ui.md: Google reCAPTCHA v2, or the test-mode checkbox. */
export function Captcha({
  mode,
  siteKey,
  token,
  onToken,
  resetSignal,
  error,
}: Props) {
  const errorId = "recaptchaToken-error";
  return (
    <div className="field captcha">
      {mode === "TEST" ? (
        <label>
          <input
            type="checkbox"
            checked={token !== ""}
            onChange={(e) => onToken(e.target.checked ? TEST_TOKEN : "")}
            aria-invalid={error ? "true" : undefined}
            aria-describedby={error ? errorId : undefined}
          />{" "}
          I am not a robot (test mode)
        </label>
      ) : (
        <RecaptchaWidget
          siteKey={siteKey ?? ""}
          onToken={onToken}
          resetSignal={resetSignal}
        />
      )}
      {error && (
        <p id={errorId} className="error">
          {error}
        </p>
      )}
    </div>
  );
}

function RecaptchaWidget({
  siteKey,
  onToken,
  resetSignal,
}: {
  siteKey: string;
  onToken: (token: string) => void;
  resetSignal: number;
}) {
  const container = useRef<HTMLDivElement>(null);
  const widgetId = useRef<number | null>(null);

  useEffect(() => {
    const render = () => {
      if (container.current && window.grecaptcha && widgetId.current === null) {
        widgetId.current = window.grecaptcha.render(container.current, {
          sitekey: siteKey,
          callback: (value: string) => onToken(value),
          "expired-callback": () => onToken(""),
        });
      }
    };
    if (window.grecaptcha) {
      window.grecaptcha.ready(render);
    } else {
      window.onRecaptchaLoad = render;
      if (!document.querySelector(`script[src="${SCRIPT_URL}"]`)) {
        const script = document.createElement("script");
        script.src = SCRIPT_URL;
        script.async = true;
        document.head.appendChild(script);
      }
    }
  }, [siteKey, onToken]);

  useEffect(() => {
    if (resetSignal > 0 && widgetId.current !== null) {
      window.grecaptcha?.reset(widgetId.current);
    }
  }, [resetSignal]);

  return <div ref={container} />;
}
