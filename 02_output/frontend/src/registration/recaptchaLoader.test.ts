import { afterEach, describe, expect, it, vi } from "vitest";

afterEach(() => {
  delete window.grecaptcha;
  delete window.onRecaptchaLoad;
  document.head.querySelectorAll("script").forEach((s) => s.remove());
  vi.resetModules();
});

describe("reCAPTCHA loader", () => {
  it("resolves at once when the script is already there", async () => {
    window.grecaptcha = { render: vi.fn(), reset: vi.fn() };
    const { loadRecaptcha } = await import("./recaptchaLoader");

    await expect(loadRecaptcha()).resolves.toBeUndefined();
    expect(document.head.querySelector("script")).toBeNull();
  });

  it("adds the script once and resolves when Google calls back", async () => {
    const { loadRecaptcha, RECAPTCHA_SCRIPT } = await import("./recaptchaLoader");

    const first = loadRecaptcha();
    const second = loadRecaptcha();
    const scripts = document.head.querySelectorAll("script");

    expect(second).toBe(first);
    expect(scripts).toHaveLength(1);
    expect(scripts[0].src).toBe(RECAPTCHA_SCRIPT);
    expect(scripts[0].async).toBe(true);
    expect(scripts[0].defer).toBe(true);
    expect(RECAPTCHA_SCRIPT).toContain("render=explicit");
    window.onRecaptchaLoad?.();
    await expect(first).resolves.toBeUndefined();
  });
});
