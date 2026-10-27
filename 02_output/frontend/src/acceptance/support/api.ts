import { vi } from "vitest";

/** Form configuration as GET /api/form-config returns it (openapi.yaml FormConfig). */
export const formConfig = {
  conferenceName: "Test Conference 2026",
  captcha: { mode: "TEST" },
  options: [
    {
      id: "ws-testing",
      name: "Workshop: Testing in practice",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-industry",
      name: "Workshop: Industry partnerships",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ev-dinner",
      name: "Conference dinner",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "meal-lunch",
      name: "Lunch (vegetarian)",
      category: "MEAL",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
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
  ],
};

export type RegistrationReply =
  { kind: "status"; status: number; body: unknown } | { kind: "network-error" };

export interface ApiStub {
  /** Bodies of every POST /api/registrations the page sent, parsed. */
  registrations: unknown[];
  fetch: ReturnType<typeof vi.fn>;
}

/** Replaces global fetch: form-config answers with {@link formConfig}, registrations with `reply`. */
export function stubApi(
  reply: RegistrationReply = {
    kind: "status",
    status: 500,
    body: { message: "x" },
  },
): ApiStub {
  const registrations: unknown[] = [];
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const url =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.href
            : input.url;
      const method = (
        init?.method ?? (input instanceof Request ? input.method : "GET")
      ).toUpperCase();
      if (url.endsWith("/api/form-config") && method === "GET") {
        return jsonResponse(200, formConfig);
      }
      if (url.endsWith("/api/registrations") && method === "POST") {
        const raw =
          init?.body ??
          (input instanceof Request ? await input.text() : undefined);
        registrations.push(typeof raw === "string" ? JSON.parse(raw) : raw);
        if (reply.kind === "network-error") {
          throw new TypeError("Failed to fetch");
        }
        return jsonResponse(reply.status, reply.body);
      }
      return jsonResponse(404, { message: "not found" });
    },
  );
  vi.stubGlobal("fetch", fetchMock);
  return { registrations, fetch: fetchMock };
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
