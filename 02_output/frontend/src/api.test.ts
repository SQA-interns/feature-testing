import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration } from "./api";
import type { RegistrationRequest } from "./types";

const request: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "a@b.org",
  organization: "Org",
  studyInstitution: null,
  studyProgramme: null,
  studentId: null,
  options: ["ws"],
  consents: ["privacy"],
  captchaToken: "t",
};

function respond(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("fetchFormConfig", () => {
  it("reads the form configuration from /api/form-config", async () => {
    const fetchMock = respond(200, { options: [], consents: [], captchaSiteKey: "" });

    await expect(fetchFormConfig()).resolves.toEqual({
      options: [],
      consents: [],
      captchaSiteKey: "",
    });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", {
      headers: { Accept: "application/json" },
    });
  });

  it("fails when the backend does not answer 200", async () => {
    respond(502, {});
    await expect(fetchFormConfig()).rejects.toThrow("form configuration unavailable");
  });
});

describe("submitRegistration", () => {
  it("posts the request as JSON and returns the new id", async () => {
    const fetchMock = respond(201, { id: "abc" });

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "created", id: "abc" });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
  });

  it("treats a 201 without an id as a failure", async () => {
    respond(201, { id: 5 });
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });

  it("returns the valid field errors of a 400", async () => {
    respond(400, {
      errors: [
        { field: "email", message: "bad" },
        { field: "options" },
        null,
        "text",
        { field: 1, message: "x" },
      ],
    });
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "rejected",
      errors: [{ field: "email", message: "bad" }],
    });
  });

  it("treats a 400 without field errors as a failure", async () => {
    respond(400, { errors: [] });
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
    respond(400, { message: "x" });
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });

  it("treats other statuses and network errors as failures", async () => {
    respond(500, { message: "internal" });
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
    respond(429, { message: "slow down" });
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );
    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });
});
