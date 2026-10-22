import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import { basicHeader, mockApi, ORGANIZER, type ApiHandler } from "./support/mockApi";

interface Item {
  id: string;
  type: string;
  firstName: string;
  lastName: string;
  email: string;
  organization: string | null;
  studyInstitution: string | null;
  options: string[];
  registeredAt: string;
}

const items: Item[] = [
  {
    id: "7d1c2a9e-0000-4000-8000-000000000001",
    type: "STUDENT",
    firstName: "Luka",
    lastName: "Kranjc",
    email: "luka@uni-lj.si",
    organization: null,
    studyInstitution: "University of Ljubljana",
    options: ["ws-ai", "meal-lunch"],
    registeredAt: "2026-10-20T10:16:00Z",
  },
  {
    id: "7d1c2a9e-0000-4000-8000-000000000002",
    type: "EXTERNAL",
    firstName: "Ana",
    lastName: "Novak",
    email: "ana@example.org",
    organization: "Example d.o.o.",
    studyInstitution: null,
    options: ["other-tour"],
    registeredAt: "2026-10-20T10:15:00Z",
  },
];

function organizerApi(list: Item[]): ApiHandler {
  return (call) => {
    if (call.method !== "GET" || call.url.pathname !== "/api/registrations") return undefined;
    if (call.headers.get("Authorization") !== basicHeader(ORGANIZER.username, ORGANIZER.password)) {
      return { status: 401, body: { message: "Authentication required" } };
    }
    return { status: 200, body: { total: list.length, page: 1, items: list } };
  };
}

async function signIn(username: string, password: string): Promise<void> {
  fireEvent.change(await screen.findByRole("textbox", { name: "Username" }), {
    target: { value: username },
  });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

describe("US-009 organizer page", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/organizer");
  });
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    window.history.pushState({}, "", "/");
  });

  it("AC-009-12: wrong credentials show an error and no registration data", async () => {
    mockApi(organizerApi(items));
    render(<App />);

    await signIn(ORGANIZER.username, "wrong-password");

    expect(await screen.findByText("Invalid username or password.")).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Registrations" })).toBeNull();
    expect(screen.queryByText("Kranjc")).toBeNull();
    expect(screen.queryByText("ana@example.org")).toBeNull();
  });

  it("AC-009-12: valid credentials show the list with search, filters and page navigation", async () => {
    mockApi(organizerApi(items));
    render(<App />);

    await signIn(ORGANIZER.username, ORGANIZER.password);

    const table = await screen.findByRole("table", { name: "Registrations" });
    expect(within(table).getByText("Kranjc")).toBeInTheDocument();
    expect(within(table).getByText("Novak")).toBeInTheDocument();
    expect(within(table).getByText("ana@example.org")).toBeInTheDocument();
    expect(within(table).getByText("University of Ljubljana")).toBeInTheDocument();
    expect(screen.getByRole("searchbox", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Type" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Option" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 1")).toBeInTheDocument();
    expect(screen.getByText("2 registrations found")).toBeInTheDocument();
    expect(screen.queryByText("Invalid username or password.")).toBeNull();
  });

  it("AC-009-12: wrong then valid credentials show the list", async () => {
    mockApi(organizerApi(items));
    render(<App />);

    await signIn("someone", "else");
    expect(await screen.findByText("Invalid username or password.")).toBeInTheDocument();
    await signIn(ORGANIZER.username, ORGANIZER.password);

    expect(await screen.findByRole("table", { name: "Registrations" })).toBeInTheDocument();
  });

  it("AC-009-13: markup and script in registration data appear only as literal text", async () => {
    const hostile: Item[] = [
      {
        ...items[1],
        firstName: '<img src="x" onerror="window.__xss=1">',
        lastName: "<script>window.__xss=2</script>",
        organization: "<b>Bold</b> & Co",
        email: "x@example.org",
      },
    ];
    mockApi(organizerApi(hostile));
    const { container } = render(<App />);

    await signIn(ORGANIZER.username, ORGANIZER.password);

    const table = await screen.findByRole("table", { name: "Registrations" });
    expect(within(table).getByText('<img src="x" onerror="window.__xss=1">')).toBeInTheDocument();
    expect(within(table).getByText("<script>window.__xss=2</script>")).toBeInTheDocument();
    expect(within(table).getByText("<b>Bold</b> & Co")).toBeInTheDocument();
    expect(container.querySelector("img")).toBeNull();
    expect(container.querySelector("script")).toBeNull();
    expect(table.querySelector("b")).toBeNull();
    expect((window as unknown as { __xss?: number }).__xss).toBeUndefined();
  });
});
