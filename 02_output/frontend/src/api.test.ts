import { afterEach, describe, expect, it, vi } from "vitest";
import {
  basicAuthorization,
  getFormConfig,
  searchQuery,
  searchRegistrations,
  submitRegistration,
} from "./api";
import type { RegistrationRequest, SearchFilters } from "./types";

const request: RegistrationRequest = {
  type: "STUDENT",
  firstName: "Luka",
  lastName: "Kranjc",
  email: "l@uni.si",
  organization: null,
  studyInstitution: "Uni",
  studyProgramme: "CS",
  studentId: "1",
  options: ["a"],
  consents: ["privacy"],
  captchaToken: "t",
};

const filters: SearchFilters = { text: "", type: "", option: "", page: 1 };
const account = { username: "organizer", password: "pw" };

function respond(status: number, body?: string) {
  const fetchMock = vi.fn(async () => new Response(body ?? null, { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function fail() {
  vi.stubGlobal(
    "fetch",
    vi.fn(async () => {
      throw new TypeError("Failed to fetch");
    }),
  );
}

afterEach(() => vi.unstubAllGlobals());

describe("getFormConfig", () => {
  it("returns the configuration", async () => {
    const fetchMock = respond(200, '{"options":[],"consents":[],"captchaSiteKey":"k"}');

    await expect(getFormConfig()).resolves.toEqual({
      options: [],
      consents: [],
      captchaSiteKey: "k",
    });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", {
      headers: { Accept: "application/json" },
    });
  });

  it("rejects when the backend fails", async () => {
    respond(500);

    await expect(getFormConfig()).rejects.toThrow("form configuration unavailable");
  });
});

describe("submitRegistration", () => {
  it("posts JSON and returns the new id", async () => {
    const fetchMock = respond(201, '{"id":"abc"}');

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "created", id: "abc" });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
  });

  it("returns the field errors of a 400", async () => {
    respond(400, '{"errors":[{"field":"email","message":"bad"}]}');

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "invalid",
      errors: [{ field: "email", message: "bad" }],
    });
  });

  it.each([
    [400, '{"message":"x"}'],
    [400, "not json"],
    [201, "not json"],
    [500, '{"errors":[]}'],
    [503, ""],
  ])("treats %i %s as a failure", async (status, body) => {
    respond(status, body);

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });

  it("treats a network error as a failure", async () => {
    fail();

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });
});

describe("organizer search", () => {
  it("encodes Basic credentials as UTF-8", () => {
    expect(basicAuthorization(account)).toBe("Basic " + btoa("organizer:pw"));
    expect(basicAuthorization({ username: "č", password: "ž" })).toBe("Basic xI06xb4=");
  });

  it("builds the query from the set filters only", () => {
    expect(searchQuery(filters)).toBe("page=1");
    expect(searchQuery({ text: " Ana Ž ", type: "STUDENT", option: "ws-ai", page: 3 })).toBe(
      "text=+Ana+%C5%BD+&type=STUDENT&option=ws-ai&page=3",
    );
    expect(searchQuery({ ...filters, text: "   " })).toBe("page=1");
  });

  it("sends the credentials and returns the result", async () => {
    const fetchMock = respond(200, '{"total":1,"page":2,"items":[]}');

    await expect(searchRegistrations(account, { ...filters, page: 2 })).resolves.toEqual({
      kind: "ok",
      result: { total: 1, page: 2, items: [] },
    });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations?page=2", {
      headers: {
        Accept: "application/json",
        Authorization: basicAuthorization(account),
        "X-Requested-With": "XMLHttpRequest",
      },
    });
  });

  it("reports wrong credentials", async () => {
    respond(401, '{"message":"Authentication required"}');

    await expect(searchRegistrations(account, filters)).resolves.toEqual({ kind: "unauthorized" });
  });

  it("returns parameter errors", async () => {
    respond(400, '{"errors":[{"field":"page","message":"bad"}]}');

    await expect(searchRegistrations(account, filters)).resolves.toEqual({
      kind: "invalid",
      errors: [{ field: "page", message: "bad" }],
    });
  });

  it.each([
    [400, "{}"],
    [403, '{"message":"https"}'],
    [500, ""],
    [200, "not json"],
  ])("treats %i %s as a failure", async (status, body) => {
    respond(status, body);

    await expect(searchRegistrations(account, filters)).resolves.toEqual({ kind: "failed" });
  });

  it("treats a network error as a failure", async () => {
    fail();

    await expect(searchRegistrations(account, filters)).resolves.toEqual({ kind: "failed" });
  });
});
