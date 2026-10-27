import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Captcha, TEST_TOKEN } from "./Captcha";

const SCRIPT =
  "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit";

function fakeGrecaptcha() {
  const params: Record<string, unknown>[] = [];
  const grecaptcha = {
    ready: vi.fn((callback: () => void) => callback()),
    render: vi.fn((_el: HTMLElement, p: Record<string, unknown>) => {
      params.push(p);
      return 7;
    }),
    reset: vi.fn(),
  };
  return { grecaptcha, params };
}

afterEach(() => {
  delete window.grecaptcha;
  delete window.onRecaptchaLoad;
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

describe("Captcha in test mode", () => {
  it("sets and clears the test token", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha mode="TEST" token="" onToken={onToken} resetSignal={0} />,
    );
    const box = screen.getByRole("checkbox", {
      name: "I am not a robot (test mode)",
    });
    expect(box).not.toBeChecked();
    expect(box).not.toHaveAttribute("aria-invalid");
    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith(TEST_TOKEN);
    expect(TEST_TOKEN).toBe("test-pass");

    rerender(
      <Captcha
        mode="TEST"
        token={TEST_TOKEN}
        onToken={onToken}
        resetSignal={0}
      />,
    );
    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith("");
  });

  it("shows its error next to the checkbox", () => {
    render(
      <Captcha
        mode="TEST"
        token=""
        onToken={vi.fn()}
        resetSignal={0}
        error="Please confirm"
      />,
    );
    const box = screen.getByRole("checkbox");
    expect(box).toHaveAttribute("aria-invalid", "true");
    expect(box).toHaveAccessibleDescription("Please confirm");
  });
});

describe("Captcha with reCAPTCHA", () => {
  it("loads the script once and renders when it is ready", () => {
    const onToken = vi.fn();
    const first = render(
      <Captcha
        mode="RECAPTCHA"
        siteKey="site-key"
        token=""
        onToken={onToken}
        resetSignal={0}
      />,
    );
    render(
      <Captcha
        mode="RECAPTCHA"
        siteKey="site-key"
        token=""
        onToken={onToken}
        resetSignal={0}
      />,
    );
    const scripts = document.head.querySelectorAll(`script[src="${SCRIPT}"]`);
    expect(scripts).toHaveLength(1);
    expect((scripts[0] as HTMLScriptElement).async).toBe(true);
    expect(screen.queryByRole("checkbox")).toBeNull();

    const { grecaptcha, params } = fakeGrecaptcha();
    window.grecaptcha = grecaptcha;
    act(() => window.onRecaptchaLoad?.());
    expect(grecaptcha.render).toHaveBeenCalledTimes(1);
    expect(params[0].sitekey).toBe("site-key");
    act(() => window.onRecaptchaLoad?.());
    expect(grecaptcha.render).toHaveBeenCalledTimes(1);

    (params[0].callback as (v: string) => void)("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    (params[0]["expired-callback"] as () => void)();
    expect(onToken).toHaveBeenLastCalledWith("");
    first.unmount();
  });

  it("renders at once when the API is loaded and resets on request", () => {
    const { grecaptcha } = fakeGrecaptcha();
    window.grecaptcha = grecaptcha;
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha mode="RECAPTCHA" token="" onToken={onToken} resetSignal={0} />,
    );
    expect(grecaptcha.ready).toHaveBeenCalled();
    expect(grecaptcha.render).toHaveBeenCalledWith(
      expect.any(HTMLElement),
      expect.objectContaining({ sitekey: "" }),
    );
    expect(grecaptcha.reset).not.toHaveBeenCalled();
    expect(document.head.querySelector("script")).toBeNull();

    rerender(
      <Captcha mode="RECAPTCHA" token="" onToken={onToken} resetSignal={1} />,
    );
    expect(grecaptcha.reset).toHaveBeenCalledWith(7);
  });

  it("does not reset a widget that was never rendered", () => {
    const { rerender } = render(
      <Captcha mode="RECAPTCHA" token="" onToken={vi.fn()} resetSignal={0} />,
    );
    const { grecaptcha } = fakeGrecaptcha();
    window.grecaptcha = { ...grecaptcha, ready: vi.fn() };
    rerender(
      <Captcha mode="RECAPTCHA" token="" onToken={vi.fn()} resetSignal={1} />,
    );
    expect(grecaptcha.reset).not.toHaveBeenCalled();
  });
});
