// Loads the Google reCAPTCHA v2 script once (SR-01); the CSP allows its origins.
const SCRIPT_URL = "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit";

export interface Grecaptcha {
  render(
    element: HTMLElement,
    options: { sitekey: string; callback: (token: string) => void; "expired-callback": () => void },
  ): number;
  reset(widgetId?: number): void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
    onRecaptchaLoad?: () => void;
  }
}

let loading: Promise<Grecaptcha> | undefined;

/** Loads the Google script once; resolves when its API is ready. */
export function loadRecaptcha(): Promise<Grecaptcha> {
  if (window.grecaptcha) return Promise.resolve(window.grecaptcha);
  if (!loading) {
    loading = new Promise((resolve, reject) => {
      window.onRecaptchaLoad = () => resolve(window.grecaptcha as Grecaptcha);
      const script = document.createElement("script");
      script.src = SCRIPT_URL;
      script.async = true;
      script.onerror = () => {
        loading = undefined;
        reject(new Error("reCAPTCHA could not be loaded"));
      };
      document.head.appendChild(script);
    });
  }
  return loading;
}
