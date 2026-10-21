import { fireEvent, screen, within } from "@testing-library/react";
import { vi } from "vitest";

/** The form configuration served by the stubbed `GET /api/form-config` (openapi.json FormConfig). */
export const formConfig = {
  options: [
    {
      id: "ws-ai",
      name: "AI workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-thesis",
      name: "Thesis writing workshop",
      category: "WORKSHOP",
      availableTo: ["STUDENT"],
    },
    {
      id: "ev-gala",
      name: "Gala evening",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ev-tour", name: "Industry tour", category: "EVENT", availableTo: ["EXTERNAL"] },
    { id: "meal-lunch", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    {
      id: "other-cert",
      name: "Certificate of attendance",
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
  captchaSiteKey: "",
  captchaTestMode: true,
};

export const PRIVACY = formConfig.consents[0].text;
export const CAPTCHA = "I am not a robot (test mode)";
export const GENERAL_ERROR = "The registration could not be completed. Please try again later.";
export const GROUPS = ["Workshops", "Events", "Meals", "Other activities"];

export type PostAnswer = { status: number; body?: unknown } | "network-error";

export interface FetchStub {
  /** Bodies of every `POST /api/registrations`, parsed from JSON. */
  posts: Record<string, unknown>[];
}

function urlOf(input: unknown): string {
  if (typeof input === "string") return input;
  if (input instanceof URL) return input.href;
  return (input as { url: string }).url;
}

function json(status: number, body: unknown): Response {
  return new Response(body === undefined ? "" : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json;charset=UTF-8" },
  });
}

/** Stubs `fetch`: the form configuration above, and `answer` for every registration POST. */
export function stubFetch(answer: PostAnswer): FetchStub {
  const stub: FetchStub = { posts: [] };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: unknown, init?: RequestInit) => {
      const url = urlOf(input);
      const method = (init?.method ?? (input as Request)?.method ?? "GET").toUpperCase();
      if (url.endsWith("/api/form-config") && method === "GET") {
        return json(200, formConfig);
      }
      if (url.endsWith("/api/registrations") && method === "POST") {
        const raw = init?.body ?? (await (input as Request).text());
        stub.posts.push(JSON.parse(String(raw)) as Record<string, unknown>);
        if (answer === "network-error") throw new TypeError("Failed to fetch");
        return json(answer.status, answer.body);
      }
      return json(404, { message: "not stubbed" });
    }),
  );
  return stub;
}

export async function chooseType(label: "External participant" | "Student"): Promise<void> {
  const chooser = await screen.findByRole("radiogroup", { name: "Registration type" });
  fireEvent.click(within(chooser).getByRole("radio", { name: label }));
}

export function textbox(label: string): HTMLInputElement {
  return screen.getByRole("textbox", { name: label }) as HTMLInputElement;
}

export function type(label: string, value: string): void {
  fireEvent.change(textbox(label), { target: { value } });
}

export function check(name: string): void {
  fireEvent.click(screen.getByRole("checkbox", { name }));
}

export function submit(): void {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

/** Names of the visible textboxes. */
export function textboxNames(): string[] {
  return screen
    .queryAllByRole("textbox")
    .map((e) => e.getAttribute("name") ?? "")
    .sort();
}

/** Option group legends in page order, with the sorted values (option ids) of their checkboxes. */
export function optionGroups(): { legend: string; values: string[] }[] {
  return screen
    .queryAllByRole("group")
    .map((group) => ({ group, legend: group.querySelector("legend")?.textContent?.trim() ?? "" }))
    .filter(({ legend }) => GROUPS.includes(legend))
    .map(({ group, legend }) => ({
      legend,
      values: within(group)
        .getAllByRole("checkbox")
        .map((c) => (c as HTMLInputElement).value)
        .sort(),
    }));
}

/** The option checkbox with this accessible name inside the group with this legend. */
export function optionIn(legend: string, name: string): HTMLInputElement {
  const group = screen.getByRole("group", { name: legend });
  return within(group).getByRole("checkbox", { name }) as HTMLInputElement;
}

/** Fills a valid external form: fields, AI workshop and Gala evening, the privacy consent, captcha. */
export async function fillValidExternal(): Promise<void> {
  await chooseType("External participant");
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana@example.org");
  type("Organization / institution", "Example d.o.o.");
  check("AI workshop");
  check("Gala evening");
  check(PRIVACY);
  check(CAPTCHA);
}

/** The text of the element with id `<field>-error`, or null. */
export function fieldError(field: string): string | null {
  return document.getElementById(`${field}-error`)?.textContent?.trim() || null;
}
