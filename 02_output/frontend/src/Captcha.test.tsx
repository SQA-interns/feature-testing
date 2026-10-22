import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

interface RenderOptions {
  sitekey: string;
  callback: (token: string) => void;
  "expired-callback": () => void;
}

const loadRecaptcha = vi.fn();
vi.mock("./recaptcha", () => ({ loadRecaptcha: () => loadRecaptcha() }));

const { Captcha, TEST_TOKEN } = await import("./Captcha");

function fakeGrecaptcha() {
  const rendered: RenderOptions[] = [];
  const grecaptcha = {
    render: vi.fn((_element: HTMLElement, options: RenderOptions) => {
      rendered.push(options);
      return 7;
    }),
    reset: vi.fn(),
  };
  return { grecaptcha, rendered };
}

describe("Captcha", () => {
  beforeEach(() => {
    loadRecaptcha.mockReset();
  });
  afterEach(() => {
    cleanup();
    delete window.grecaptcha;
  });

  it("in test mode gives the test token without loading Google", () => {
    const onToken = vi.fn();

    render(<Captcha siteKey="" testMode onToken={onToken} resetSignal={0} />);

    expect(screen.getByText("reCAPTCHA test mode")).toBeInTheDocument();
    expect(onToken).toHaveBeenCalledWith(TEST_TOKEN);
    expect(TEST_TOKEN).toBe("test-pass");
    expect(loadRecaptcha).not.toHaveBeenCalled();
  });

  it("renders the widget with the site key and passes its tokens on", async () => {
    const { grecaptcha, rendered } = fakeGrecaptcha();
    loadRecaptcha.mockResolvedValue(grecaptcha);
    const onToken = vi.fn();

    const { container } = render(
      <Captcha siteKey="site-key" testMode={false} onToken={onToken} resetSignal={0} />,
    );
    await act(async () => {});

    expect(grecaptcha.render).toHaveBeenCalledTimes(1);
    expect(grecaptcha.render.mock.calls[0][0]).toBe(container.querySelector(".captcha"));
    expect(rendered[0].sitekey).toBe("site-key");
    rendered[0].callback("tok");
    expect(onToken).toHaveBeenLastCalledWith("tok");
    rendered[0]["expired-callback"]();
    expect(onToken).toHaveBeenLastCalledWith("");
    expect(screen.queryByText("reCAPTCHA test mode")).toBeNull();
  });

  it("resets the widget when the reset signal changes", async () => {
    const { grecaptcha } = fakeGrecaptcha();
    window.grecaptcha = grecaptcha;
    loadRecaptcha.mockResolvedValue(grecaptcha);
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha siteKey="k" testMode={false} onToken={onToken} resetSignal={0} />,
    );
    await act(async () => {});
    expect(grecaptcha.reset).not.toHaveBeenCalled();

    rerender(<Captcha siteKey="k" testMode={false} onToken={onToken} resetSignal={1} />);

    expect(grecaptcha.reset).toHaveBeenCalledWith(7);
    expect(onToken).toHaveBeenLastCalledWith("");
  });

  it("does not reset before the widget exists or in test mode", () => {
    loadRecaptcha.mockReturnValue(new Promise(() => {}));
    const { grecaptcha } = fakeGrecaptcha();
    window.grecaptcha = grecaptcha;
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha siteKey="k" testMode={false} onToken={onToken} resetSignal={0} />,
    );

    rerender(<Captcha siteKey="k" testMode={false} onToken={onToken} resetSignal={1} />);
    rerender(<Captcha siteKey="k" testMode onToken={onToken} resetSignal={2} />);

    expect(grecaptcha.reset).not.toHaveBeenCalled();
    expect(onToken).not.toHaveBeenCalledWith("");
  });

  it("gives no token when Google cannot be loaded", async () => {
    loadRecaptcha.mockRejectedValue(new Error("blocked"));
    const onToken = vi.fn();

    render(<Captcha siteKey="k" testMode={false} onToken={onToken} resetSignal={0} />);
    await act(async () => {});

    expect(onToken).toHaveBeenCalledWith("");
  });

  it("does not render after it was removed", async () => {
    const { grecaptcha } = fakeGrecaptcha();
    let resolve: (value: unknown) => void = () => {};
    loadRecaptcha.mockReturnValue(new Promise((r) => (resolve = r)));

    const { unmount } = render(
      <Captcha siteKey="k" testMode={false} onToken={vi.fn()} resetSignal={0} />,
    );
    unmount();
    await act(async () => resolve(grecaptcha));

    expect(grecaptcha.render).not.toHaveBeenCalled();
  });
});
