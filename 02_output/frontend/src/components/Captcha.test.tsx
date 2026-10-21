import "@testing-library/jest-dom/vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Captcha, TEST_TOKEN } from "./Captcha";

type RenderOptions = {
  sitekey: string;
  callback: (token: string) => void;
  "expired-callback": () => void;
};

function fakeGrecaptcha() {
  const rendered: RenderOptions[] = [];
  const api = {
    render: vi.fn((_element: HTMLElement, options: RenderOptions) => {
      rendered.push(options);
      return 7;
    }),
    reset: vi.fn(),
  };
  return { api, rendered };
}

afterEach(() => {
  delete window.grecaptcha;
  delete window.onRecaptchaLoad;
  document.getElementById("recaptcha-script")?.remove();
});

describe("Captcha in test mode", () => {
  it("yields the test token while checked and nothing otherwise", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha testMode siteKey="" token="" resetCount={0} onToken={onToken} />,
    );
    const box = screen.getByRole("checkbox", { name: "I am not a robot (test mode)" });
    expect(box).not.toBeChecked();

    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith(TEST_TOKEN);

    rerender(<Captcha testMode siteKey="" token={TEST_TOKEN} resetCount={0} onToken={onToken} />);
    expect(box).toBeChecked();
    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith("");
  });

  it("shows its error next to the check", () => {
    render(
      <Captcha testMode siteKey="" token="" resetCount={0} error="Please" onToken={() => {}} />,
    );

    expect(document.getElementById("captchaToken-error")).toHaveTextContent("Please");
    expect(screen.getByRole("checkbox")).toHaveAttribute("aria-describedby", "captchaToken-error");
  });

  it("has no description without an error", () => {
    render(<Captcha testMode siteKey="" token="" resetCount={0} onToken={() => {}} />);

    expect(screen.getByRole("checkbox")).not.toHaveAttribute("aria-describedby");
    expect(document.getElementById("captchaToken-error")).toBeNull();
  });
});

describe("Captcha with Google reCAPTCHA", () => {
  it("renders the widget with the site key and passes tokens on", () => {
    const { api, rendered } = fakeGrecaptcha();
    window.grecaptcha = api;
    const onToken = vi.fn();

    render(<Captcha testMode={false} siteKey="site-1" token="" resetCount={0} onToken={onToken} />);

    expect(api.render).toHaveBeenCalledTimes(1);
    expect(api.render.mock.calls[0][0]).toBe(document.getElementById("recaptcha"));
    expect(rendered[0].sitekey).toBe("site-1");
    rendered[0].callback("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    rendered[0]["expired-callback"]();
    expect(onToken).toHaveBeenLastCalledWith("");
    expect(document.getElementById("recaptcha-script")).toBeNull();
  });

  it("loads the script once and renders when it has loaded", () => {
    const onToken = vi.fn();
    const { unmount } = render(
      <Captcha testMode={false} siteKey="site-2" token="" resetCount={0} onToken={onToken} />,
    );
    const script = document.getElementById("recaptcha-script") as HTMLScriptElement;
    expect(script.src).toBe(
      "https://www.google.com/recaptcha/api.js?render=explicit&onload=onRecaptchaLoad",
    );
    expect(script.async).toBe(true);
    expect(script.defer).toBe(true);
    unmount();
    render(<Captcha testMode={false} siteKey="site-2" token="" resetCount={0} onToken={onToken} />);
    expect(document.querySelectorAll("#recaptcha-script")).toHaveLength(1);

    const { api, rendered } = fakeGrecaptcha();
    window.grecaptcha = api;
    window.onRecaptchaLoad?.();
    window.onRecaptchaLoad?.();

    expect(api.render).toHaveBeenCalledTimes(1);
    expect(rendered[0].sitekey).toBe("site-2");
  });

  it("resets the widget and the token after a failed submission", () => {
    const { api } = fakeGrecaptcha();
    window.grecaptcha = api;
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha testMode={false} siteKey="k" token="t" resetCount={0} onToken={onToken} />,
    );
    expect(api.reset).not.toHaveBeenCalled();

    rerender(<Captcha testMode={false} siteKey="k" token="t" resetCount={1} onToken={onToken} />);

    expect(api.reset).toHaveBeenCalledWith(7);
    expect(onToken).toHaveBeenLastCalledWith("");
  });

  it("does not reset before the widget exists", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha testMode={false} siteKey="k" token="" resetCount={0} onToken={onToken} />,
    );

    rerender(<Captcha testMode={false} siteKey="k" token="" resetCount={1} onToken={onToken} />);

    expect(onToken).not.toHaveBeenCalled();
  });
});

describe("Captcha reset before the widget was rendered", () => {
  it("does not reset while the script has loaded but not yet rendered", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha testMode={false} siteKey="k" token="" resetCount={0} onToken={onToken} />,
    );
    const { api } = fakeGrecaptcha();
    window.grecaptcha = api;

    rerender(<Captcha testMode={false} siteKey="k" token="" resetCount={1} onToken={onToken} />);

    expect(api.reset).not.toHaveBeenCalled();
    expect(onToken).not.toHaveBeenCalled();
  });
});
