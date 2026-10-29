import { describe, expect, it, vi } from "vitest";
import {
  basicAuthorization,
  fetchFormConfig,
  searchRegistrations,
  submitRegistration,
} from "./api";
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

describe("searchRegistrations", () => {
  const login = { username: "organizer", password: "secret" };
  const empty = {
    search: "",
    type: "",
    workshop: "",
    event: "",
    meal: "",
    other: "",
  };

  it("sends no query string when nothing is set, with JSON and Basic headers", async () => {
    const fetch = respond(200, {
      count: 0,
      registrations: [],
      optionCounts: [],
    });
    vi.stubGlobal("fetch", fetch);

    await expect(searchRegistrations(login, empty)).resolves.toEqual({
      kind: "ok",
      result: { count: 0, registrations: [], optionCounts: [] },
    });
    expect(fetch).toHaveBeenCalledWith("/api/organizer/registrations", {
      headers: {
        Accept: "application/json",
        Authorization: "Basic " + btoa("organizer:secret"),
      },
    });
  });

  it("sends only the values that are set", async () => {
    const fetch = respond(200, {});
    vi.stubGlobal("fetch", fetch);

    await searchRegistrations(login, {
      ...empty,
      search: "koš",
      meal: "meal-lunch",
    });

    expect(fetch.mock.calls[0][0]).toBe(
      "/api/organizer/registrations?search=ko%C5%A1&meal=meal-lunch",
    );
  });

  it("reports 401 as unauthorized", async () => {
    vi.stubGlobal(
      "fetch",
      respond(401, { message: "Authentication required." }),
    );

    await expect(searchRegistrations(login, empty)).resolves.toEqual({
      kind: "unauthorized",
    });
  });

  it("returns the field errors of a 400", async () => {
    const fieldErrors = [{ field: "search", code: "TOO_LONG", message: "m" }];
    vi.stubGlobal("fetch", respond(400, { message: "x", fieldErrors }));

    await expect(searchRegistrations(login, empty)).resolves.toEqual({
      kind: "rejected",
      fieldErrors,
    });
  });

  it.each([
    [
      "a 400 without field errors",
      respond(400, { message: "x", fieldErrors: [] }),
    ],
    ["a 400 with no list", respond(400, { message: "x" })],
    ["a 403", respond(403, { message: "x" })],
    ["a 500", respond(500, { message: "x" })],
    ["a network error", vi.fn().mockRejectedValue(new TypeError("offline"))],
  ])("treats %s as failed", async (_case, fetch) => {
    vi.stubGlobal("fetch", fetch);

    await expect(searchRegistrations(login, empty)).resolves.toEqual({
      kind: "failed",
    });
  });
});

describe("basicAuthorization", () => {
  it("encodes the credentials as UTF-8", () => {
    expect(basicAuthorization({ username: "Žan", password: "č" })).toBe(
      "Basic xb1hbjrEjQ==",
    );
  });
});
