import { fireEvent, screen, within } from "@testing-library/react";
import { vi } from "vitest";

/** Option counts as GET /api/organizer/registrations returns them (openapi.yaml CategoryCounts). */
export const OPTION_COUNTS = [
  {
    category: "WORKSHOP",
    options: [
      {
        id: "ws-testing",
        name: "Workshop: Testing in practice",
        active: true,
        count: 2,
      },
      {
        id: "ws-legacy",
        name: "Workshop: Legacy systems",
        active: false,
        count: 0,
      },
    ],
  },
  {
    category: "EVENT",
    options: [
      { id: "ev-dinner", name: "Conference dinner", active: true, count: 1 },
    ],
  },
  {
    category: "MEAL",
    options: [
      { id: "meal-lunch", name: "Lunch (vegetarian)", active: true, count: 2 },
    ],
  },
  {
    category: "OTHER",
    options: [{ id: "other-tour", name: "City tour", active: true, count: 0 }],
  },
];

export const EXTERNAL = {
  id: "3f2b8c1e-7a4d-4e6b-9f10-2c5d8e9a0b11",
  type: "EXTERNAL",
  registeredAt: "2026-10-27T10:05:00Z",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana.novak@example.org",
  organization: "Example Institute",
  options: [
    {
      id: "ws-testing",
      name: "Workshop: Testing in practice",
      category: "WORKSHOP",
    },
    { id: "ev-dinner", name: "Conference dinner", category: "EVENT" },
    { id: "meal-lunch", name: "Lunch (vegetarian)", category: "MEAL" },
  ],
  consents: [{ id: "privacy", givenAt: "2026-10-27T10:05:00Z" }],
};

export const STUDENT = {
  id: "9a1c2d3e-4f50-4617-8293-a4b5c6d7e8f9",
  type: "STUDENT",
  registeredAt: "2026-10-27T09:00:00Z",
  firstName: "Luka",
  lastName: "Kranjc",
  email: "luka.kranjc@student.example.org",
  studyInstitution: "University of Ljubljana",
  studyProgramme: "Computer Science",
  studentId: "63200001",
  options: [
    {
      id: "ws-testing",
      name: "Workshop: Testing in practice",
      category: "WORKSHOP",
    },
    { id: "meal-lunch", name: "Lunch (vegetarian)", category: "MEAL" },
  ],
  consents: [{ id: "privacy", givenAt: "2026-10-27T09:00:00Z" }],
};

/** A SearchResult body with the given rows, newest first as the API orders them. */
export function result(
  registrations: object[],
  optionCounts: object[] = OPTION_COUNTS,
) {
  return { count: registrations.length, registrations, optionCounts };
}

export interface SearchCall {
  params: URLSearchParams;
  authorization: string | null;
}

export type SearchReply = { status: number; body: unknown };

export const USERNAME = "organizer";
export const PASSWORD = "organizer-test-password-1234";
export const BASIC = "Basic " + btoa(`${USERNAME}:${PASSWORD}`);

/** A query parameter of a call; an absent parameter reads as "". */
export function param(call: SearchCall, name: string): string {
  return call.params.get(name) ?? "";
}

/**
 * Replaces global fetch: GET /api/organizer/registrations answers with `answer(call)`; anything
 * else is 404. Every search call is recorded.
 */
export function stubSearchApi(answer: (call: SearchCall) => SearchReply) {
  const calls: SearchCall[] = [];
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const raw =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.href
            : input.url;
      const url = new URL(raw, "http://localhost");
      const headers = new Headers(
        init?.headers ?? (input instanceof Request ? input.headers : undefined),
      );
      if (url.pathname === "/api/organizer/registrations") {
        const call = {
          params: url.searchParams,
          authorization: headers.get("Authorization"),
        };
        calls.push(call);
        const reply = answer(call);
        return new Response(JSON.stringify(reply.body), {
          status: reply.status,
          headers: { "Content-Type": "application/json" },
        });
      }
      return new Response(JSON.stringify({ message: "not found" }), {
        status: 404,
        headers: { "Content-Type": "application/json" },
      });
    },
  );
  vi.stubGlobal("fetch", fetchMock);
  return { calls, fetch: fetchMock };
}

export function logIn(username = USERNAME, password = PASSWORD) {
  fireEvent.change(screen.getByLabelText("Username"), {
    target: { value: username },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: password },
  });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
}

/** Selects the option with the visible name `optionName` in the select labelled `label`. */
export function choose(label: string, optionName: string) {
  const select = screen.getByLabelText(label);
  const option = within(select).getByRole("option", { name: optionName });
  fireEvent.change(select, {
    target: { value: (option as HTMLOptionElement).value },
  });
}

/** Label → value of each `dt`/`dd` pair in a listed registration. */
export function entries(item: HTMLElement): Record<string, string> {
  const map: Record<string, string> = {};
  item.querySelectorAll("dt").forEach((dt) => {
    const dd = dt.nextElementSibling;
    map[dt.textContent ?? ""] = dd?.textContent ?? "";
  });
  return map;
}

export async function registrationItems(): Promise<HTMLElement[]> {
  const list = await screen.findByRole("list", { name: "Registrations" });
  return within(list).getAllByRole("listitem");
}

export const ERROR_TOO_LONG =
  "The search text can have at most 100 characters.";
