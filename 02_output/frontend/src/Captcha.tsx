// reCAPTCHA v2 (SR-01). In test mode the backend accepts "test-pass" and no widget is loaded.
import { useEffect, useRef } from "react";
import { loadRecaptcha } from "./recaptcha";

export const TEST_TOKEN = "test-pass";

interface CaptchaProps {
  siteKey: string;
  testMode: boolean;
  /** Called with the current token ("" when none or expired). */
  onToken: (token: string) => void;
  /** Changing this number resets the widget (after every answer of the backend). */
  resetSignal: number;
}

export function Captcha({ siteKey, testMode, onToken, resetSignal }: CaptchaProps) {
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<number | null>(null);
  const tokenCallback = useRef(onToken);
  tokenCallback.current = onToken;

  useEffect(() => {
    if (testMode) {
      tokenCallback.current(TEST_TOKEN);
      return;
    }
    let active = true;
    loadRecaptcha()
      .then((grecaptcha) => {
        if (!active || !container.current || widget.current !== null) return;
        widget.current = grecaptcha.render(container.current, {
          sitekey: siteKey,
          callback: (token) => tokenCallback.current(token),
          "expired-callback": () => tokenCallback.current(""),
        });
      })
      .catch(() => tokenCallback.current(""));
    return () => {
      active = false;
    };
  }, [siteKey, testMode]);

  useEffect(() => {
    if (resetSignal === 0 || testMode || widget.current === null) return;
    window.grecaptcha?.reset(widget.current);
    tokenCallback.current("");
  }, [resetSignal, testMode]);

  if (testMode) return <p className="captcha-note">reCAPTCHA test mode</p>;
  return <div ref={container} className="captcha" />;
}
