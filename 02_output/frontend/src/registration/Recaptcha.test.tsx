import { act, render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { Recaptcha } from "./Recaptcha";

type Parameters = {
  sitekey: string;
  callback: (token: string) => void;
  "expired-callback": () => void;
};

function fakeGrecaptcha() {
  const rendered: Parameters[] = [];
  const grecaptcha = {
    render: vi.fn((_container: HTMLElement, parameters: Parameters) => {
      rendered.push(parameters);
      return 7;
    }),
    reset: vi.fn(),
  };
  window.grecaptcha = grecaptcha;
  return { grecaptcha, rendered };
}

beforeEach(() => {
  delete window.grecaptcha;
});

afterEach(() => {
  delete window.grecaptcha;
});

describe("Recaptcha widget", () => {
  it("renders the widget with the site key and passes tokens on", async () => {
    const { grecaptcha, rendered } = fakeGrecaptcha();
    const onToken = vi.fn();

    render(<Recaptcha siteKey="site-key" onToken={onToken} resetCount={0} />);
    await vi.waitFor(() => expect(grecaptcha.render).toHaveBeenCalledTimes(1));

    expect(rendered[0].sitekey).toBe("site-key");
    rendered[0].callback("token-1");
    expect(onToken).toHaveBeenLastCalledWith("token-1");
    rendered[0]["expired-callback"]();
    expect(onToken).toHaveBeenLastCalledWith("");
    expect(grecaptcha.reset).not.toHaveBeenCalled();
  });

  it("resets the widget and clears the token when asked", async () => {
    const { grecaptcha } = fakeGrecaptcha();
    const onToken = vi.fn();
    const { rerender } = render(<Recaptcha siteKey="k" onToken={onToken} resetCount={0} />);
    await vi.waitFor(() => expect(grecaptcha.render).toHaveBeenCalled());

    rerender(<Recaptcha siteKey="k" onToken={onToken} resetCount={1} />);

    expect(grecaptcha.reset).toHaveBeenCalledWith(7);
    expect(onToken).toHaveBeenCalledWith("");
  });

  it("does not render after it was removed", async () => {
    const { grecaptcha } = fakeGrecaptcha();
    const { unmount } = render(<Recaptcha siteKey="k" onToken={vi.fn()} resetCount={0} />);
    unmount();
    await act(async () => {
      await Promise.resolve();
    });

    expect(grecaptcha.render).not.toHaveBeenCalled();
  });

  it("does not reset before the widget exists", () => {
    const onToken = vi.fn();
    render(<Recaptcha siteKey="k" onToken={onToken} resetCount={1} />);

    expect(onToken).not.toHaveBeenCalled();
  });
});
