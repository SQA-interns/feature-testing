import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration } from "./client";

function answer(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), { status });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("fetchFormConfig", () => {
  it("returns the configuration from the own origin", async () => {
    const fetchMock = vi.fn(async () => answer(200, { options: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchFormConfig()).resolves.toEqual({ options: [] });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", {
      headers: { Accept: "application/json" },
    });
  });

  it("fails on any other status", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => answer(401)),
    );

    await expect(fetchFormConfig()).rejects.toThrow("Form configuration answered 401");
  });
});

describe("submitRegistration", () => {
  it("posts JSON and returns the new id on 201", async () => {
    const fetchMock = vi.fn(async () => answer(201, { id: "abc" }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(submitRegistration({ type: "EXTERNAL" })).resolves.toEqual({
      kind: "created",
      id: "abc",
    });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: '{"type":"EXTERNAL"}',
    });
  });

  it("returns an empty id when the 201 body has none", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => answer(201, {})),
    );

    await expect(submitRegistration({})).resolves.toEqual({ kind: "created", id: "" });
  });

  it("returns the field errors of a 400", async () => {
    const errors = [{ field: "email", message: "Bad" }];
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => answer(400, { errors })),
    );

    await expect(submitRegistration({})).resolves.toEqual({ kind: "invalid", errors });
  });

  it.each([
    { errors: [] },
    { errors: "x" },
    { errors: [{ field: 1, message: "m" }] },
    { errors: [{ field: "f", message: 2 }] },
    { errors: [null] },
    { message: "no list" },
  ])("treats a 400 without usable field errors as a failure: %j", async (body) => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => answer(400, body)),
    );

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });

  it.each([413, 429, 500, 503])("treats status %i as a failure", async (status) => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => answer(status, { message: "x" })),
    );

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });

  it("treats a network error or an unreadable body as a failure", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );
    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });

    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("not json", { status: 201 })),
    );
    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });
});
