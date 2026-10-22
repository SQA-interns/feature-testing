import { vi } from "vitest";

/** One request the page sent, as the backend would see it. */
export interface ApiCall {
  url: URL;
  method: string;
  headers: Headers;
  body: unknown;
}

export type ApiAnswer = { status: number; body?: unknown } | "network-error";
export type ApiHandler = (call: ApiCall) => ApiAnswer | undefined;

export const ORGANIZER = { username: "organizer", password: "s3cret-organizer-pw" };

/** The form configuration of the frontend acceptance tests (shape of GET /api/form-config). */
export const formConfig = {
  options: [
    {
      id: "ws-ai",
      name: "AI workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ws-cloud", name: "Cloud workshop", category: "WORKSHOP", availableTo: ["EXTERNAL"] },
    {
      id: "ev-gala",
      name: "Gala evening",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ev-night", name: "Student night", category: "EVENT", availableTo: ["STUDENT"] },
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
      text: "I agree that my personal data is processed for organizing the conference.",
      mandatory: true,
    },
    { id: "photo", text: "Photos of me may be published.", mandatory: false },
  ],
  captchaSiteKey: "",
  captchaTestMode: true,
};

export const PRIVACY_TEXT = formConfig.consents[0].text;
export const PHOTO_TEXT = formConfig.consents[1].text;

function jsonResponse(status: number, body: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function toCall(input: RequestInfo | URL, init?: RequestInit): Promise<ApiCall> {
  const request = input instanceof Request ? input : undefined;
  const rawUrl = request ? request.url : input.toString();
  const url = new URL(rawUrl, "http://localhost");
  const method = (init?.method ?? request?.method ?? "GET").toUpperCase();
  const headers = new Headers(init?.headers ?? request?.headers);
  let text: string | undefined;
  if (typeof init?.body === "string") text = init.body;
  else if (request && method !== "GET") text = await request.clone().text();
  let body: unknown = undefined;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  return { url, method, headers, body };
}

/**
 * Replaces fetch with the given handler. GET /api/form-config answers with {@link formConfig}
 * unless the handler answers it. Unhandled requests answer 404. Returns the recorded calls.
 */
export function mockApi(handler: ApiHandler = () => undefined): ApiCall[] {
  const calls: ApiCall[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const call = await toCall(input, init);
    calls.push(call);
    const answer =
      handler(call) ??
      (call.method === "GET" && call.url.pathname === "/api/form-config"
        ? { status: 200, body: formConfig }
        : { status: 404, body: { message: "Not found" } });
    if (answer === "network-error") throw new TypeError("Failed to fetch");
    return jsonResponse(answer.status, answer.body);
  });
  vi.stubGlobal("fetch", fetchMock);
  return calls;
}

export function registrationPosts(calls: ApiCall[]): ApiCall[] {
  return calls.filter((c) => c.method === "POST" && c.url.pathname === "/api/registrations");
}

export function basicHeader(username: string, password: string): string {
  return "Basic " + btoa(`${username}:${password}`);
}
