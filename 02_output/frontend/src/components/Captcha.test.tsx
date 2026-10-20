import "@testing-library/jest-dom/vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Captcha, RECAPTCHA_SCRIPT, TEST_TOKEN } from "./Captcha";

afterEach(() => {
  cleanup();
  delete window.grecaptcha;
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

function fakeGrecaptcha() {
  const handlers: { callback?: (t: string) => void; expired?: () => void; sitekey?: string } = {};
  const api = {
    ready: vi.fn((callback: () => void) => callback()),
    render: vi.fn(
      (
        _container: HTMLElement,
        p: { sitekey: string; callback: (t: string) => void; "expired-callback": () => void },
      ) => {
        handlers.sitekey = p.sitekey;
        handlers.callback = p.callback;
        handlers.expired = p["expired-callback"];
        return 7;
      },
    ),
    reset: vi.fn(),
  };
  return { api, handlers };
}

describe("Captcha in test mode", () => {
  it("yields the test token when ticked and nothing when unticked", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha siteKey="" testMode checked={false} resetSignal={0} onToken={onToken} />,
    );

    fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));
    expect(onToken).toHaveBeenLastCalledWith(TEST_TOKEN);

    rerender(<Captcha siteKey="" testMode checked resetSignal={0} onToken={onToken} />);
    fireEvent.click(screen.getByRole("checkbox"));
    expect(onToken).toHaveBeenLastCalledWith(null);
  });

  it("shows its error linked to the checkbox", () => {
    render(
      <Captcha
        siteKey=""
        testMode
        checked={false}
        error="Confirm"
        resetSignal={0}
        onToken={vi.fn()}
      />,
    );
    expect(screen.getByRole("checkbox")).toHaveAccessibleDescription("Confirm");
    expect(screen.getByRole("alert")).toHaveTextContent("Confirm");
  });
});

describe("Captcha with Google reCAPTCHA", () => {
  it("renders the widget with the site key and passes tokens on", () => {
    const { api, handlers } = fakeGrecaptcha();
    window.grecaptcha = api;
    const onToken = vi.fn();

    render(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={0} onToken={onToken} />,
    );

    expect(api.render).toHaveBeenCalledTimes(1);
    expect(handlers.sitekey).toBe("site");
    handlers.callback?.("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    handlers.expired?.();
    expect(onToken).toHaveBeenLastCalledWith(null);
    expect(screen.queryByRole("checkbox")).toBeNull();
  });

  it("resets the widget when the reset signal changes", () => {
    const { api } = fakeGrecaptcha();
    window.grecaptcha = api;
    const { rerender } = render(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={0} onToken={vi.fn()} />,
    );
    expect(api.reset).not.toHaveBeenCalled();

    rerender(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={1} onToken={vi.fn()} />,
    );
    expect(api.reset).toHaveBeenCalledWith(7);
  });

  it("does not reset a widget that was never rendered", () => {
    const { rerender } = render(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={0} onToken={vi.fn()} />,
    );
    const { api } = fakeGrecaptcha();
    window.grecaptcha = api;
    rerender(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={2} onToken={vi.fn()} />,
    );
    expect(api.reset).not.toHaveBeenCalled();
  });

  it("loads the script once and renders after it loaded", () => {
    const { unmount } = render(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={0} onToken={vi.fn()} />,
    );
    const scripts = document.head.querySelectorAll(`script[src="${RECAPTCHA_SCRIPT}"]`);
    expect(scripts).toHaveLength(1);
    expect((scripts[0] as HTMLScriptElement).async).toBe(true);

    const { api } = fakeGrecaptcha();
    window.grecaptcha = api;
    act(() => {
      scripts[0].dispatchEvent(new Event("load"));
    });
    expect(api.render).toHaveBeenCalledTimes(1);
    act(() => {
      scripts[0].dispatchEvent(new Event("load"));
    });
    expect(api.render).toHaveBeenCalledTimes(1);
    unmount();

    render(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={0} onToken={vi.fn()} />,
    );
    expect(document.head.querySelectorAll(`script[src="${RECAPTCHA_SCRIPT}"]`)).toHaveLength(1);
  });

  it("ignores a load event without the reCAPTCHA API and after unmount", () => {
    const { unmount } = render(
      <Captcha siteKey="site" testMode={false} checked={false} resetSignal={0} onToken={vi.fn()} />,
    );
    const script = document.head.querySelector(
      `script[src="${RECAPTCHA_SCRIPT}"]`,
    ) as HTMLScriptElement;
    act(() => {
      script.dispatchEvent(new Event("load"));
    });
    unmount();
    const { api } = fakeGrecaptcha();
    window.grecaptcha = api;
    script.dispatchEvent(new Event("load"));
    expect(api.render).not.toHaveBeenCalled();
  });

  it("shows the error below the widget", () => {
    window.grecaptcha = fakeGrecaptcha().api;
    render(
      <Captcha
        siteKey="site"
        testMode={false}
        checked={false}
        error="Confirm"
        resetSignal={0}
        onToken={vi.fn()}
      />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Confirm");
  });
});
