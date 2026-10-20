import { useEffect, useRef } from "react";

export const TEST_TOKEN = "test-pass";
export const RECAPTCHA_SCRIPT = "https://www.google.com/recaptcha/api.js?render=explicit";

interface ReCaptcha {
  ready(callback: () => void): void;
  render(
    container: HTMLElement,
    parameters: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
    },
  ): number;
  reset(widgetId?: number): void;
}

declare global {
  interface Window {
    grecaptcha?: ReCaptcha;
  }
}

interface CaptchaProps {
  siteKey: string;
  testMode: boolean;
  checked: boolean;
  error?: string;
  /** Changes whenever the widget must be reset (a token can be used once). */
  resetSignal: number;
  onToken: (token: string | null) => void;
}

/** reCAPTCHA v2 (SR-01); in test mode a checkbox that yields the test token (api-contract.md). */
export function Captcha({ siteKey, testMode, checked, error, resetSignal, onToken }: CaptchaProps) {
  const errorNode = error && (
    <p id="captcha-error" role="alert" className="error">
      {error}
    </p>
  );
  if (testMode) {
    return (
      <div className="field captcha">
        <div className="choice">
          <input
            type="checkbox"
            id="captcha-test"
            checked={checked}
            aria-describedby={error ? "captcha-error" : undefined}
            onChange={(event) => onToken(event.target.checked ? TEST_TOKEN : null)}
          />
          <label htmlFor="captcha-test">I am not a robot (test mode)</label>
        </div>
        {errorNode}
      </div>
    );
  }
  return (
    <div className="field captcha">
      <GoogleWidget siteKey={siteKey} resetSignal={resetSignal} onToken={onToken} />
      {errorNode}
    </div>
  );
}

function GoogleWidget({
  siteKey,
  resetSignal,
  onToken,
}: Pick<CaptchaProps, "siteKey" | "resetSignal" | "onToken">) {
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<number | null>(null);
  const tokenHandler = useRef(onToken);
  tokenHandler.current = onToken;

  useEffect(() => {
    const render = () => {
      const api = window.grecaptcha;
      if (!api || !container.current || widget.current !== null) return;
      api.ready(() => {
        if (container.current && widget.current === null) {
          widget.current = api.render(container.current, {
            sitekey: siteKey,
            callback: (token) => tokenHandler.current(token),
            "expired-callback": () => tokenHandler.current(null),
          });
        }
      });
    };
    if (window.grecaptcha) {
      render();
      return;
    }
    let script = document.querySelector<HTMLScriptElement>(`script[src="${RECAPTCHA_SCRIPT}"]`);
    if (!script) {
      script = document.createElement("script");
      script.src = RECAPTCHA_SCRIPT;
      script.async = true;
      document.head.appendChild(script);
    }
    script.addEventListener("load", render);
    return () => script?.removeEventListener("load", render);
  }, [siteKey]);

  useEffect(() => {
    if (resetSignal > 0 && widget.current !== null) {
      window.grecaptcha?.reset(widget.current);
    }
  }, [resetSignal]);

  return <div ref={container} className="g-recaptcha" />;
}
