import { afterEach, describe, expect, it, vi } from "vitest";
import {
  basicAuth,
  fetchFormConfig,
  searchRegistrations,
  submitRegistration,
  type RegistrationRequest,
} from "./api";

const request: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.org",
  organization: "Org",
  studyInstitution: null,
  studyProgramme: null,
  studentId: null,
  options: [],
  consents: ["privacy"],
  captchaToken: "test-pass",
};

const credentials = { username: "organizer", password: "pässwörd" };
const query = { text: "", type: "", option: "", page: 1 };

function respond(status: number, body?: string) {
  const fetchMock = vi.fn(async () => new Response(body ?? null, { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("api", () => {
  it("loads the form configuration and fails on an error status", async () => {
    respond(200, JSON.stringify({ options: [], consents: [], captchaSiteKey: "k" }));
    await expect(fetchFormConfig()).resolves.toEqual({
      options: [],
      consents: [],
      captchaSiteKey: "k",
    });

    respond(503);
    await expect(fetchFormConfig()).rejects.toThrow("form configuration unavailable");
  });

  it("posts the registration as JSON and reads the new id", async () => {
    const fetchMock = respond(201, JSON.stringify({ id: "abc" }));

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "created", id: "abc" });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
  });

  it("returns the field errors of a rejected registration", async () => {
    respond(400, JSON.stringify({ errors: [{ field: "email", message: "bad" }] }));
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "rejected",
      errors: [{ field: "email", message: "bad" }],
    });

    respond(400, JSON.stringify({ errors: "not a list" }));
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "rejected", errors: [] });

    respond(400, "not json");
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "rejected", errors: [] });
  });

  it("reports other answers and network errors as failed", async () => {
    respond(500);
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });

    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });

  it("encodes UTF-8 credentials for HTTP Basic", () => {
    expect(basicAuth({ username: "a", password: "b" })).toBe("Basic YTpi");
    expect(basicAuth(credentials)).toBe("Basic b3JnYW5pemVyOnDDpHNzd8O2cmQ=");
  });

  it("sends only the given search parameters with the organizer headers", async () => {
    const fetchMock = respond(200, JSON.stringify({ total: 0, page: 2, items: [] }));

    const outcome = await searchRegistrations(credentials, {
      text: " Novak ",
      type: "STUDENT",
      option: "ws",
      page: 2,
    });

    expect(outcome).toEqual({ kind: "ok", result: { total: 0, page: 2, items: [] } });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/registrations?text=+Novak+&type=STUDENT&option=ws&page=2");
    expect(init.headers).toEqual({
      Accept: "application/json",
      Authorization: basicAuth(credentials),
      "X-Requested-With": "XMLHttpRequest",
    });

    await searchRegistrations(credentials, { ...query, text: "   " });
    expect((fetchMock.mock.calls[1] as unknown as [string])[0]).toBe("/api/registrations?page=1");
  });

  it("maps the search answers", async () => {
    respond(401);
    await expect(searchRegistrations(credentials, query)).resolves.toEqual({
      kind: "unauthorized",
    });

    respond(400, JSON.stringify({ errors: [{ field: "text", message: "long" }] }));
    await expect(searchRegistrations(credentials, query)).resolves.toEqual({
      kind: "rejected",
      errors: [{ field: "text", message: "long" }],
    });

    respond(403);
    await expect(searchRegistrations(credentials, query)).resolves.toEqual({ kind: "failed" });

    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    await expect(searchRegistrations(credentials, query)).resolves.toEqual({ kind: "failed" });
  });
});
