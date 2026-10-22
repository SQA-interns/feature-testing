import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

async function freshModule() {
  vi.resetModules();
  return import("./recaptcha");
}

function scripts(): HTMLScriptElement[] {
  return Array.from(document.head.querySelectorAll("script"));
}

describe("loadRecaptcha", () => {
  beforeEach(() => {
    document.head.innerHTML = "";
    delete window.grecaptcha;
    delete window.onRecaptchaLoad;
  });
  afterEach(() => {
    delete window.grecaptcha;
  });

  it("uses an already loaded API", async () => {
    const api = { render: vi.fn(), reset: vi.fn() };
    window.grecaptcha = api;
    const { loadRecaptcha } = await freshModule();

    await expect(loadRecaptcha()).resolves.toBe(api);
    expect(scripts()).toHaveLength(0);
  });

  it("adds the Google script once and resolves when it calls back", async () => {
    const { loadRecaptcha } = await freshModule();

    const first = loadRecaptcha();
    const second = loadRecaptcha();

    expect(second).toBe(first);
    expect(scripts()).toHaveLength(1);
    expect(scripts()[0].src).toBe(
      "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit",
    );
    expect(scripts()[0].async).toBe(true);
    const api = { render: vi.fn(), reset: vi.fn() };
    window.grecaptcha = api;
    window.onRecaptchaLoad?.();
    await expect(first).resolves.toBe(api);
  });

  it("rejects when the script fails and tries again next time", async () => {
    const { loadRecaptcha } = await freshModule();

    const first = loadRecaptcha();
    scripts()[0].onerror?.(new Event("error"));

    await expect(first).rejects.toThrow("reCAPTCHA could not be loaded");
    const second = loadRecaptcha();
    expect(second).not.toBe(first);
    expect(scripts()).toHaveLength(2);
    second.catch(() => {});
  });
});
