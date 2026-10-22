// Google reCAPTCHA v2 checkbox, rendered explicitly (docs/02_contracts/recaptcha.md).
interface Grecaptcha {
  render(
    container: HTMLElement,
    parameters: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
    },
  ): number;
  reset(widget: number): void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
    onRecaptchaLoad?: () => void;
  }
}

export const RECAPTCHA_SCRIPT =
  "https://www.google.com/recaptcha/api.js?render=explicit&onload=onRecaptchaLoad";

let loading: Promise<void> | null = null;

export function loadRecaptcha(): Promise<void> {
  if (window.grecaptcha) {
    return Promise.resolve();
  }
  if (!loading) {
    loading = new Promise((resolve) => {
      window.onRecaptchaLoad = () => resolve();
      const script = document.createElement("script");
      script.src = RECAPTCHA_SCRIPT;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    });
  }
  return loading;
}
