import { useEffect, useRef } from "react";

export const TEST_TOKEN = "test-pass";
const SCRIPT_ID = "recaptcha-script";
const SCRIPT_URL = "https://www.google.com/recaptcha/api.js?render=explicit&onload=onRecaptchaLoad";

interface Grecaptcha {
  render: (
    element: HTMLElement,
    options: { sitekey: string; callback: (token: string) => void; "expired-callback": () => void },
  ) => number;
  reset: (widget: number) => void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
    onRecaptchaLoad?: () => void;
  }
}

interface Props {
  testMode: boolean;
  siteKey: string;
  token: string;
  /** Changes after every failed submission, so the production widget asks again. */
  resetCount: number;
  error?: string;
  onToken: (token: string) => void;
}

/** The test-mode check or the Google reCAPTCHA v2 widget (SR-01, AR-07). */
export function Captcha({ testMode, siteKey, token, resetCount, error, onToken }: Props) {
  return (
    <div className="captcha">
      {testMode ? (
        <label className="choice">
          <input
            type="checkbox"
            checked={token === TEST_TOKEN}
            onChange={(e) => onToken(e.target.checked ? TEST_TOKEN : "")}
            aria-describedby={error ? "captchaToken-error" : undefined}
          />
          I am not a robot (test mode)
        </label>
      ) : (
        <GoogleWidget siteKey={siteKey} resetCount={resetCount} onToken={onToken} />
      )}
      {error && (
        <span id="captchaToken-error" className="error">
          {error}
        </span>
      )}
    </div>
  );
}

function GoogleWidget({
  siteKey,
  resetCount,
  onToken,
}: {
  siteKey: string;
  resetCount: number;
  onToken: (token: string) => void;
}) {
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<number | null>(null);
  const callback = useRef(onToken);
  callback.current = onToken;

  useEffect(() => {
    const render = () => {
      if (container.current && window.grecaptcha && widget.current === null) {
        widget.current = window.grecaptcha.render(container.current, {
          sitekey: siteKey,
          callback: (t) => callback.current(t),
          "expired-callback": () => callback.current(""),
        });
      }
    };
    if (window.grecaptcha) {
      render();
    } else {
      window.onRecaptchaLoad = render;
      if (!document.getElementById(SCRIPT_ID)) {
        const script = document.createElement("script");
        script.id = SCRIPT_ID;
        script.src = SCRIPT_URL;
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);
      }
    }
  }, [siteKey]);

  useEffect(() => {
    if (resetCount > 0 && widget.current !== null && window.grecaptcha) {
      window.grecaptcha.reset(widget.current);
      callback.current("");
    }
  }, [resetCount]);

  return <div id="recaptcha" ref={container} />;
}
