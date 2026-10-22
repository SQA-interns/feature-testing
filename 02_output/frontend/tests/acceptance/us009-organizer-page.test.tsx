import { fireEvent, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { type Call, fakeBackend, json, renderApp, type } from "./support";

afterEach(() => {
  vi.unstubAllGlobals();
});

const VALID_AUTH = "Basic " + btoa("organizer:correct-password-2026");

const items = [
  {
    id: "0a1b2c3d-0000-4000-8000-000000000001",
    type: "STUDENT",
    firstName: "Peter",
    lastName: "Zupan",
    email: "peter@uni.example.org",
    organization: null,
    studyInstitution: "Univerza v Mariboru",
    options: ["meal-lunch"],
    registeredAt: "2026-10-20T10:16:00Z",
  },
  {
    id: "0a1b2c3d-0000-4000-8000-000000000002",
    type: "EXTERNAL",
    firstName: "Mojca",
    lastName: "Kranjc",
    email: "mojca.kranjc@alpha.si",
    organization: "Alpha Consulting",
    studyInstitution: null,
    options: ["ws-ai"],
    registeredAt: "2026-10-20T10:15:00Z",
  },
];

function organizerBackend(result: (call: Call) => unknown) {
  return fakeBackend({
    "GET /api/registrations": (call) =>
      call.headers.get("Authorization") === VALID_AUTH
        ? json(200, result(call))
        : json(401, { errors: [{ field: "credentials", message: "Unauthorized" }] }),
  });
}

function signIn(password: string) {
  type("Username", "organizer");
  type("Password", password);
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

function searches(calls: Call[]): Call[] {
  return calls.filter((c) => c.path === "/api/registrations");
}

describe("US-009 organizer search page", () => {
  it("AC-009-12 wrong credentials show an error and no data; valid credentials show the list and controls", async () => {
    const calls = organizerBackend(() => ({ total: 2, page: 1, items }));
    renderApp("/organizer");

    await screen.findByLabelText("Username");
    signIn("wrong-password");

    expect(await screen.findByRole("alert")).toHaveTextContent("Wrong username or password.");
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.queryByText("Zupan")).toBeNull();

    signIn("correct-password-2026");

    const table = await screen.findByRole("table");
    expect(within(table).getByText("Zupan")).toBeVisible();
    expect(within(table).getByText("Kranjc")).toBeVisible();
    expect(screen.getByText("2 registrations")).toBeVisible();
    expect(screen.getByRole("searchbox", { name: "Search" })).toBeVisible();
    expect(screen.getByRole("combobox", { name: "Type" })).toBeVisible();
    expect(screen.getByRole("combobox", { name: "Option" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
    expect(screen.queryByText("Wrong username or password.")).toBeNull();
    expect(searches(calls).at(-1)?.headers.get("Authorization")).toBe(VALID_AUTH);
  });

  it("AC-009-12 the search field and filters are sent as query parameters", async () => {
    const calls = organizerBackend(() => ({ total: 1, page: 1, items: [items[0]] }));
    renderApp("/organizer");
    await screen.findByLabelText("Username");
    signIn("correct-password-2026");
    await screen.findByRole("table");

    fireEvent.change(screen.getByRole("searchbox", { name: "Search" }), {
      target: { value: "zupan" },
    });
    fireEvent.change(screen.getByRole("combobox", { name: "Type" }), {
      target: { value: "STUDENT" },
    });
    const option = screen.getByRole("combobox", { name: "Option" });
    expect(within(option).getByRole("option", { name: "Lunch" })).toBeInTheDocument();
    fireEvent.change(option, { target: { value: "meal-lunch" } });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));

    await vi.waitFor(() => expect(searches(calls).at(-1)?.query.get("text")).toBe("zupan"));
    const last = searches(calls).at(-1)!;
    expect(last.query.get("type")).toBe("STUDENT");
    expect(last.query.get("option")).toBe("meal-lunch");
    expect(last.query.get("page") ?? "1").toBe("1");
  });

  it("AC-009-12 page navigation requests the next page", async () => {
    const calls = organizerBackend((call) => ({
      total: 45,
      page: Number(call.query.get("page") ?? "1"),
      items,
    }));
    renderApp("/organizer");
    await screen.findByLabelText("Username");
    signIn("correct-password-2026");
    await screen.findByRole("table");

    expect(screen.getByText("Page 1 of 3")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("Page 2 of 3")).toBeVisible();
    expect(searches(calls).at(-1)?.query.get("page")).toBe("2");
    expect(screen.getByRole("button", { name: "Previous" })).toBeEnabled();
  });

  it("AC-009-13 markup in registration data is shown as literal text and not executed", async () => {
    const markupName = '<img src="x" onerror="window.__pwned=true">';
    const scriptName = "<script>window.__pwned=true</script>Zupan";
    organizerBackend(() => ({
      total: 1,
      page: 1,
      items: [{ ...items[0], firstName: markupName, lastName: scriptName }],
    }));
    const { container } = renderApp("/organizer");
    await screen.findByLabelText("Username");
    signIn("correct-password-2026");

    const table = await screen.findByRole("table");
    expect(within(table).getByText(markupName)).toBeVisible();
    expect(within(table).getByText(scriptName)).toBeVisible();
    expect(container.querySelector("table img")).toBeNull();
    expect(container.querySelector("table script")).toBeNull();
    expect((window as unknown as { __pwned?: boolean }).__pwned).toBeUndefined();
  });
});
