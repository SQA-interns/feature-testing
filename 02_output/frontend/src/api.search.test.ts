import { afterEach, describe, expect, it, vi } from "vitest";
import { basicAuthorization, searchRegistrations } from "./api";
import type { SearchQuery } from "./types";

const credentials = { username: "organizer", password: "secret-pass" };
const noFilter: SearchQuery = { text: "", type: "", option: "" };

function respond(status: number, body: unknown) {
  const fetchMock = vi.fn(
    async () => new Response(body === undefined ? "" : JSON.stringify(body), { status }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function calledUrl(fetchMock: ReturnType<typeof respond>): URL {
  const [url] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
  return new URL(url, "http://localhost");
}

function calledHeaders(fetchMock: ReturnType<typeof respond>): Record<string, string> {
  const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
  return init.headers as Record<string, string>;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("basicAuthorization", () => {
  it("encodes username and password as UTF-8 base64", () => {
    expect(basicAuthorization({ username: "ana", password: "pass" })).toBe(
      "Basic " + btoa("ana:pass"),
    );
    expect(basicAuthorization({ username: "žiga", password: "čšž" })).toBe(
      "Basic " + Buffer.from("žiga:čšž", "utf8").toString("base64"),
    );
  });
});

describe("searchRegistrations", () => {
  it("sends the trimmed text, the filters and the page with organizer headers", async () => {
    const fetchMock = respond(200, { total: 0, page: 3, items: [] });

    await searchRegistrations(
      credentials,
      { text: "  Novak ", type: "STUDENT", option: "ws-ai" },
      3,
    );

    const url = calledUrl(fetchMock);
    expect(url.pathname).toBe("/api/registrations");
    expect(Object.fromEntries(url.searchParams)).toEqual({
      text: "Novak",
      type: "STUDENT",
      option: "ws-ai",
      page: "3",
    });
    expect(calledHeaders(fetchMock)).toEqual({
      Accept: "application/json",
      Authorization: basicAuthorization(credentials),
      "X-Requested-With": "XMLHttpRequest",
    });
  });

  it("leaves out an empty text and empty filters", async () => {
    const fetchMock = respond(200, { total: 0, page: 1, items: [] });

    await searchRegistrations(credentials, { text: "   ", type: "", option: "" }, 1);

    expect(Object.fromEntries(calledUrl(fetchMock).searchParams)).toEqual({ page: "1" });
  });

  it("returns the result of a 200 answer", async () => {
    const item = { id: "1", lastName: "Novak" };
    respond(200, { total: 21, page: 2, items: [item] });

    await expect(searchRegistrations(credentials, noFilter, 2)).resolves.toEqual({
      kind: "found",
      result: { total: 21, page: 2, items: [item] },
    });
  });

  it("uses the requested page when the answer has none", async () => {
    respond(200, { total: 0, items: [] });

    await expect(searchRegistrations(credentials, noFilter, 4)).resolves.toEqual({
      kind: "found",
      result: { total: 0, page: 4, items: [] },
    });
  });

  it.each([
    ["no total", { page: 1, items: [] }],
    ["no items", { total: 1, page: 1 }],
  ])("treats a 200 answer with %s as failed", async (_, body) => {
    respond(200, body);

    await expect(searchRegistrations(credentials, noFilter, 1)).resolves.toEqual({
      kind: "failed",
    });
  });

  it.each([
    [401, "unauthorized"],
    [403, "forbidden"],
    [429, "failed"],
    [500, "failed"],
  ])("maps HTTP %i to %s", async (status, kind) => {
    respond(status, undefined);

    await expect(searchRegistrations(credentials, noFilter, 1)).resolves.toEqual({ kind });
  });

  it("returns the field errors of a 400 answer", async () => {
    const errors = [{ field: "text", message: "Too long." }];
    respond(400, { errors: [...errors, { field: 1 }, null] });

    await expect(searchRegistrations(credentials, noFilter, 1)).resolves.toEqual({
      kind: "rejected",
      errors,
    });
  });

  it("treats a 400 answer without field errors as failed", async () => {
    respond(400, { errors: [] });

    await expect(searchRegistrations(credentials, noFilter, 1)).resolves.toEqual({
      kind: "failed",
    });
  });

  it("treats a 400 answer without an errors list as failed", async () => {
    respond(400, { message: "bad" });

    await expect(searchRegistrations(credentials, noFilter, 1)).resolves.toEqual({
      kind: "failed",
    });
  });

  it("treats a network error as failed", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );

    await expect(searchRegistrations(credentials, noFilter, 1)).resolves.toEqual({
      kind: "failed",
    });
  });
});
