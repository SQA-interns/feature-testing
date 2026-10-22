import { useEffect, useRef } from "react";
import { loadRecaptcha } from "./recaptchaLoader";

interface Props {
  siteKey: string;
  onToken: (token: string) => void;
  /** Increment to reset the widget (after a rejected submission). */
  resetCount: number;
}

export function Recaptcha({ siteKey, onToken, resetCount }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    void loadRecaptcha().then(() => {
      if (cancelled || !container.current || !window.grecaptcha || widget.current !== null) {
        return;
      }
      widget.current = window.grecaptcha.render(container.current, {
        sitekey: siteKey,
        callback: onToken,
        "expired-callback": () => onToken(""),
      });
    });
    return () => {
      cancelled = true;
    };
  }, [siteKey, onToken]);

  useEffect(() => {
    if (resetCount > 0 && widget.current !== null && window.grecaptcha) {
      window.grecaptcha.reset(widget.current);
      onToken("");
    }
  }, [resetCount, onToken]);

  return <div ref={container} data-testid="recaptcha" />;
}
