import { describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration } from "./api";
import type { RegistrationRequest } from "./types";

const request: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.org",
  organization: "I",
  optionIds: [],
  consents: ["privacy"],
  recaptchaToken: "t",
};

const respond = (status: number, body: unknown) =>
  vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );

describe("fetchFormConfig", () => {
  it("gets the configuration from the relative API path", async () => {
    const fetch = respond(200, { conferenceName: "C" });
    vi.stubGlobal("fetch", fetch);

    await expect(fetchFormConfig()).resolves.toEqual({ conferenceName: "C" });
    expect(fetch).toHaveBeenCalledWith("/api/form-config", {
      headers: { Accept: "application/json" },
    });
  });

  it("fails on an error status", async () => {
    vi.stubGlobal("fetch", respond(503, {}));

    await expect(fetchFormConfig()).rejects.toThrow(
      "form configuration unavailable",
    );
  });
});

describe("submitRegistration", () => {
  it("posts JSON and returns the accepted registration", async () => {
    const fetch = respond(201, { id: "1" });
    vi.stubGlobal("fetch", fetch);

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "accepted",
      registration: { id: "1" },
    });
    expect(fetch).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(request),
    });
  });

  it("returns the field errors of a 400", async () => {
    const fieldErrors = [
      { field: "email", code: "INVALID_FORMAT", message: "m" },
    ];
    vi.stubGlobal("fetch", respond(400, { message: "x", fieldErrors }));

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "rejected",
      fieldErrors,
    });
  });

  it.each([
    ["a 400 without field errors", 400, { message: "x" }],
    ["a 400 with an empty list", 400, { fieldErrors: [] }],
    ["a 400 with a non-list", 400, { fieldErrors: "x" }],
    ["a 200", 200, { id: "1" }],
    ["a 429", 429, { message: "slow down" }],
    ["a 500", 500, { message: "x" }],
  ])("treats %s as failed", async (_, status, body) => {
    vi.stubGlobal("fetch", respond(status, body));

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
  });

  it("treats a network error as failed", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
  });
});
