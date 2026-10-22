import { fireEvent, render, screen, within } from "@testing-library/react";
import { vi } from "vitest";
import { App } from "../../src/App";

/** The backend's form configuration as `GET /api/form-config` returns it (api-contract.md). */
export const formConfig = {
  options: [
    {
      id: "ws-ai",
      name: "AI workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-leadership",
      name: "Leadership workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ev-dinner",
      name: "Conference dinner",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ev-gala", name: "Gala evening", category: "EVENT", availableTo: ["EXTERNAL"] },
    { id: "meal-lunch", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    {
      id: "other-tour",
      name: "City tour",
      category: "OTHER",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
  ],
  consents: [
    {
      id: "privacy",
      text: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.",
      mandatory: true,
    },
    { id: "photos", text: "I agree to be photographed at the conference.", mandatory: false },
  ],
  captchaSiteKey: "test-site-key",
  captchaTestMode: true,
};

export const PRIVACY_CONSENT = /I agree that my personal data is processed/;

/** One request the page sent. */
export interface Call {
  method: string;
  path: string;
  query: URLSearchParams;
  headers: Headers;
  body: unknown;
}

type Handler = (call: Call) => Response | Promise<Response>;

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/**
 * Replaces `fetch` with a fake backend. `routes` maps `"METHOD /path"` to a handler; the form
 * configuration is served unless overridden. Returns every call made, in order.
 */
export function fakeBackend(routes: Record<string, Handler> = {}): Call[] {
  const calls: Call[] = [];
  const all: Record<string, Handler> = {
    "GET /api/form-config": () => json(200, formConfig),
    ...routes,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const request = input instanceof Request ? input : undefined;
      const url = new URL(
        typeof input === "string" ? input : input instanceof URL ? input.href : input.url,
        "http://localhost",
      );
      const method = (init?.method ?? request?.method ?? "GET").toUpperCase();
      const rawBody = init?.body ?? (request ? await request.text() : undefined);
      const call: Call = {
        method,
        path: url.pathname,
        query: url.searchParams,
        headers: new Headers(init?.headers ?? request?.headers),
        body: typeof rawBody === "string" && rawBody !== "" ? JSON.parse(rawBody) : undefined,
      };
      calls.push(call);
      const handler = all[`${method} ${url.pathname}`];
      if (!handler) {
        throw new TypeError(`unexpected request ${method} ${url.pathname}`);
      }
      return handler(call);
    }),
  );
  return calls;
}

/** Renders the whole application at `path` (`/` registration, `/organizer` organizer page). */
export function renderApp(path = "/") {
  window.history.pushState({}, "", path);
  return render(<App />);
}

export async function chooseType(label: "External participant" | "Student") {
  fireEvent.click(await screen.findByRole("radio", { name: label }));
}

export function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

export function check(name: string | RegExp) {
  fireEvent.click(screen.getByRole("checkbox", { name }));
}

export function textBoxNames(): string[] {
  return screen
    .queryAllByRole("textbox")
    .map((box) => box.getAttribute("aria-label") ?? labelOf(box as HTMLInputElement));
}

function labelOf(input: HTMLInputElement): string {
  return (input.labels?.[0]?.textContent ?? "").trim();
}

/** Names of the checkboxes inside the option group with the given legend. */
export function optionsInGroup(legend: string): string[] {
  const group = screen.getByRole("group", { name: legend });
  return within(group)
    .getAllByRole("checkbox")
    .map((box) => labelOf(box as HTMLInputElement));
}

export function fillExternal() {
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.org");
  type("Organization / institution", "Example d.o.o.");
}

export function fillStudent() {
  type("First name", "Luka");
  type("Last name", "Horvat");
  type("Email", "luka.horvat@student.example.org");
  type("Study institution", "Univerza v Ljubljani");
  type("Study programme", "Computer Science");
  type("Student ID", "63210001");
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

export function posts(calls: Call[]): Call[] {
  return calls.filter((c) => c.method === "POST");
}
