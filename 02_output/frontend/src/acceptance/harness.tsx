/* eslint-disable react-refresh/only-export-components -- test harness, never hot-reloaded */
// Black-box harness for the frontend acceptance tests: the whole app is rendered and the backend is
// replaced at the HTTP boundary (fetch to /api/*), as defined in docs/02_contracts/openapi.yaml and
// the "Registration form" section of docs/02_contracts/interfaces.md.
import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, vi } from "vitest";
import App from "../App";

export const formConfig = {
  conferenceName: "Konferenca Test 2026",
  captchaSiteKey: "",
  captchaTestMode: true,
  options: [
    {
      id: "ws-ai",
      name: "AI workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-pro",
      name: "Professional practice workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    { id: "ev-gala", name: "Gala dinner", category: "EVENT", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "ev-student", name: "Student meetup", category: "EVENT", availableTo: ["STUDENT"] },
    { id: "ml-lunch", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "ot-tour", name: "City tour", category: "OTHER", availableTo: ["EXTERNAL", "STUDENT"] },
  ],
  consents: [
    {
      id: "privacy",
      text: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.",
      mandatory: true,
    },
    { id: "photos", text: "I agree to be photographed during the conference.", mandatory: false },
  ],
};

export const LABELS = {
  firstName: "First name",
  lastName: "Last name",
  email: "Email",
  organization: "Organization / institution",
  studyInstitution: "Study institution",
  studyProgramme: "Study programme",
  studentId: "Student ID",
} as const;

export const GROUPS = {
  WORKSHOP: "Workshops",
  EVENT: "Events",
  MEAL: "Meals",
  OTHER: "Other activities",
} as const;

export type RegistrationAnswer = { status: number; body: unknown } | { networkError: true };

export interface FetchCall {
  url: string;
  method: string;
  body: unknown;
}

/** Replaces fetch: GET /api/form-config answers formConfig, POST /api/registrations answers `answer`. */
export function mockBackend(answer: RegistrationAnswer): FetchCall[] {
  const calls: FetchCall[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
    const method = (
      init?.method ?? (input instanceof Request ? input.method : "GET")
    ).toUpperCase();
    const rawBody = init?.body;
    calls.push({ url, method, body: typeof rawBody === "string" ? JSON.parse(rawBody) : rawBody });
    if (url.endsWith("/api/form-config") && method === "GET") {
      return jsonResponse(200, formConfig);
    }
    if (url.endsWith("/api/registrations") && method === "POST") {
      if ("networkError" in answer) {
        throw new TypeError("Failed to fetch");
      }
      return jsonResponse(answer.status, answer.body);
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
});

export function postCalls(calls: FetchCall[]): FetchCall[] {
  return calls.filter((c) => c.method === "POST");
}

export async function renderAndChoose(type: "External participant" | "Student"): Promise<void> {
  render(<App />);
  fireEvent.click(await screen.findByRole("radio", { name: type }));
}

/** The input labelled `label` (a trailing required marker such as " *" is allowed). */
export function field(label: string): HTMLInputElement {
  return screen.getByLabelText(new RegExp("^" + escapeRegExp(label) + "\\b")) as HTMLInputElement;
}

export function queryField(label: string): HTMLElement | null {
  return screen.queryByLabelText(new RegExp("^" + escapeRegExp(label) + "\\b"));
}

export function group(name: string): HTMLElement {
  return screen.getByRole("group", { name: new RegExp("^" + escapeRegExp(name)) });
}

export function optionNamesIn(groupName: string): string[] {
  return within(group(groupName))
    .getAllByRole("checkbox")
    .map((box) => accessibleLabel(box));
}

function accessibleLabel(element: HTMLElement): string {
  const id = element.getAttribute("id");
  const byFor = id ? document.querySelector(`label[for="${id}"]`) : null;
  const label = byFor ?? element.closest("label");
  return (label?.textContent ?? element.getAttribute("aria-label") ?? "").trim();
}

export function type(label: string, value: string): void {
  fireEvent.change(field(label), { target: { value } });
}

/** Clicks the checkbox whose accessible name starts with `name` (or matches the RegExp). */
export function tick(name: string | RegExp): void {
  const matcher = typeof name === "string" ? new RegExp("^" + escapeRegExp(name)) : name;
  fireEvent.click(screen.getByRole("checkbox", { name: matcher }));
}

export function tickCaptcha(): void {
  tick(/test mode/i);
}

export function submit(): void {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

/** Fills a valid external form: Ana Novak, AI workshop and Lunch, privacy consent, captcha. */
export async function fillValidExternal(): Promise<void> {
  await renderAndChoose("External participant");
  type(LABELS.firstName, "Ana");
  type(LABELS.lastName, "Novak");
  type(LABELS.email, "ana.novak@example.org");
  type(LABELS.organization, "Example d.o.o.");
  tick("AI workshop");
  tick("Lunch");
  tick(formConfig.consents[0].text);
  tickCaptcha();
}

export function expectFormStillShown(): void {
  expect(screen.getByRole("button", { name: "Register" })).toBeInTheDocument();
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\/]/g, "\\$&");
}
