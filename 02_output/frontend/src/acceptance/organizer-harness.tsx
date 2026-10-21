// Black-box harness for the US-009 organizer page acceptance tests: the whole app is rendered at
// /organizer and the backend is replaced at the HTTP boundary (GET /api/form-config and
// GET /api/registrations), as defined in docs/02_contracts/openapi.yaml and the "Organizer search
// page" section of docs/02_contracts/interfaces.md.
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, vi } from "vitest";
import App from "../App";
import { formConfig } from "./harness";

export const ORGANIZER = { username: "organizer", password: "organizer-pass-0001" };

export interface SearchItem {
  id: string;
  type: "EXTERNAL" | "STUDENT";
  firstName: string;
  lastName: string;
  email: string;
  organization: string | null;
  studyInstitution: string | null;
  options: string[];
  registeredAt: string;
}

export interface SearchCall {
  params: URLSearchParams;
  authorization: string | null;
}

export function item(n: number, overrides: Partial<SearchItem> = {}): SearchItem {
  return {
    id: `3f1c9a52-0000-4000-8000-${String(n).padStart(12, "0")}`,
    type: "EXTERNAL",
    firstName: `First${n}`,
    lastName: `Last${n}`,
    email: `person${n}@example.org`,
    organization: `Organization ${n}`,
    studyInstitution: null,
    options: ["ws-ai"],
    registeredAt: `2026-10-20T10:${String(n % 60).padStart(2, "0")}:00Z`,
    ...overrides,
  };
}

function basic(username: string, password: string): string {
  return "Basic " + btoa(username + ":" + password);
}

function headerOf(input: RequestInfo | URL, init: RequestInit | undefined, name: string) {
  const headers = new Headers(init?.headers ?? (input instanceof Request ? input.headers : {}));
  return headers.get(name);
}

/**
 * Replaces fetch. GET /api/registrations with the ORGANIZER credentials answers
 * {total, page, items} from `items` (20 per page, page from the query); other credentials get 401.
 */
export function mockOrganizerBackend(items: SearchItem[], total = items.length): SearchCall[] {
  const calls: SearchCall[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const raw = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
    const url = new URL(raw, "http://localhost");
    const method = (
      init?.method ?? (input instanceof Request ? input.method : "GET")
    ).toUpperCase();
    if (url.pathname === "/api/form-config" && method === "GET") {
      return jsonResponse(200, formConfig);
    }
    if (url.pathname === "/api/registrations" && method === "GET") {
      const authorization = headerOf(input, init, "Authorization");
      calls.push({ params: url.searchParams, authorization });
      if (authorization !== basic(ORGANIZER.username, ORGANIZER.password)) {
        return new Response("", { status: 401 });
      }
      const page = Number(url.searchParams.get("page") ?? "1");
      const slice = items.slice((page - 1) * 20, page * 20);
      return jsonResponse(200, { total, page, items: slice });
    }
    return jsonResponse(404, { message: "not found" });
  });
  vi.stubGlobal("fetch", fetchMock);
  return calls;
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.pushState({}, "", "/");
});

export function renderOrganizerPage(): void {
  window.history.pushState({}, "", "/organizer");
  render(<App />);
}

export async function signIn(username: string, password: string): Promise<void> {
  fireEvent.change(await screen.findByLabelText(/^Username/), { target: { value: username } });
  fireEvent.change(screen.getByLabelText(/^Password/), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

/** Selects the choice labelled `choice` in the select named `selectName`. */
export function choose(selectName: string, choice: string): void {
  const select = screen.getByRole("combobox", { name: selectName }) as HTMLSelectElement;
  const option = Array.from(select.options).find((o) => o.textContent?.trim() === choice);
  if (!option) {
    throw new Error(`no choice "${choice}" in ${selectName}`);
  }
  fireEvent.change(select, { target: { value: option.value } });
}
