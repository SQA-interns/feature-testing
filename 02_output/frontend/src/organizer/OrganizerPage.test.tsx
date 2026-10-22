import { fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { SearchItem, SearchResult } from "../api";
import { LOAD_ERROR, OrganizerPage, WRONG_CREDENTIALS } from "./OrganizerPage";

const options = [
  { id: "ws", name: "Workshop", category: "WORKSHOP", availableTo: ["EXTERNAL"] },
  { id: "meal", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
];

function item(overrides: Partial<SearchItem>): SearchItem {
  return {
    id: "1",
    type: "EXTERNAL",
    firstName: "Ana",
    lastName: "Novak",
    email: "ana@example.org",
    organization: "Org",
    studyInstitution: null,
    options: [],
    registeredAt: "2026-10-20T10:15:00Z",
    ...overrides,
  };
}

type Answer = () => Response | Promise<Response>;

function backend(
  searchAnswers: Answer[],
  formConfig: Answer = () => Response.json({ options, consents: [], captchaSiteKey: "" }),
) {
  const searches: URL[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string) => {
      if (url === "/api/form-config") return formConfig();
      searches.push(new URL(url, "http://localhost"));
      return (searchAnswers.shift() ?? (() => Response.json({}, { status: 500 })))();
    }),
  );
  return searches;
}

const ok = (result: SearchResult) => () => Response.json(result);

async function signIn() {
  fireEvent.change(screen.getByLabelText("Username"), { target: { value: "organizer" } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: "pw" } });
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
  await screen.findByRole("heading", { name: "Registrations" });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("OrganizerPage", () => {
  it("starts with empty credentials and sends the first search without filters", async () => {
    const searches = backend([ok({ total: 0, page: 1, items: [] })]);
    render(<OrganizerPage />);

    expect(screen.getByLabelText("Username")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveAttribute("type", "password");
    await signIn();

    expect([...searches[0].searchParams.entries()]).toEqual([["page", "1"]]);
    expect(screen.getByText("No registrations found.")).toBeVisible();
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("prevents the browser from submitting the forms", async () => {
    backend([ok({ total: 0, page: 1, items: [] }), ok({ total: 0, page: 1, items: [] })]);
    render(<OrganizerPage />);
    const signInForm = screen.getByRole("button", { name: "Sign in" }).closest("form")!;

    expect(fireEvent.submit(signInForm)).toBe(false);
    await screen.findByRole("heading", { name: "Registrations" });
    expect(fireEvent.submit(screen.getByRole("search"))).toBe(false);
  });

  it("shows types, institutions and option names in the table", async () => {
    backend([
      ok({
        total: 3,
        page: 1,
        items: [
          item({ id: "1", options: ["ws", "meal"] }),
          item({
            id: "2",
            type: "STUDENT",
            organization: null,
            studyInstitution: "Uni",
            options: ["gone"],
          }),
          item({ id: "3", type: "GUEST" as SearchItem["type"], organization: null }),
        ],
      }),
    ]);
    render(<OrganizerPage />);
    await signIn();
    await screen.findByRole("option", { name: "Lunch" });

    const rows = within(screen.getByRole("table")).getAllByRole("row").slice(1);
    const cells = rows.map((row) =>
      within(row)
        .getAllByRole("cell")
        .map((c) => c.textContent),
    );
    expect(cells[0].slice(1)).toEqual([
      "External participant",
      "Ana",
      "Novak",
      "ana@example.org",
      "Org",
      "Workshop, Lunch",
    ]);
    expect(cells[1][1]).toBe("Student");
    expect(cells[1][5]).toBe("Uni");
    expect(cells[1][6]).toBe("gone");
    expect(cells[2][1]).toBe("GUEST");
    expect(cells[2][5]).toBe("");
    expect(screen.getByText("3 registrations")).toBeVisible();
  });

  it("offers only the filter defaults when the options cannot be loaded", async () => {
    backend([ok({ total: 0, page: 1, items: [] })], () => Promise.reject(new TypeError("offline")));
    render(<OrganizerPage />);
    await signIn();

    expect(
      within(screen.getByRole("combobox", { name: "Option" })).getAllByRole("option"),
    ).toHaveLength(1);
  });

  it("offers only the filter defaults while the options are loading", async () => {
    backend([ok({ total: 0, page: 1, items: [] })], () => new Promise(() => {}));
    render(<OrganizerPage />);
    await signIn();

    expect(
      within(screen.getByRole("combobox", { name: "Option" })).getAllByRole("option"),
    ).toHaveLength(1);
  });

  it("shows rejected search parameters and keeps the list", async () => {
    backend([
      ok({ total: 1, page: 1, items: [item({})] }),
      () =>
        Response.json(
          {
            errors: [
              { field: "text", message: "Too long." },
              { field: "page", message: "Bad page." },
            ],
          },
          { status: 400 },
        ),
      () => Response.json({ errors: [] }, { status: 400 }),
      () => Response.json({}, { status: 500 }),
      ok({ total: 1, page: 1, items: [item({})] }),
    ]);
    render(<OrganizerPage />);
    await signIn();
    const search = screen.getByRole("button", { name: "Search" });

    fireEvent.click(search);
    expect(await screen.findByRole("alert")).toHaveTextContent(/^Too long\. Bad page\.$/);
    expect(screen.getByRole("table")).toBeVisible();
    fireEvent.click(search);
    expect(await screen.findByText(LOAD_ERROR)).toBeVisible();
    fireEvent.click(search);
    await vi.waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent(LOAD_ERROR));
    fireEvent.click(search);
    await vi.waitFor(() => expect(screen.queryByRole("alert")).toBeNull());
    expect(LOAD_ERROR).toBe("The registrations could not be loaded. Please try again later.");
  });

  it("returns to the sign-in when the credentials stop working", async () => {
    backend([
      ok({ total: 1, page: 1, items: [item({})] }),
      () => new Response(null, { status: 401 }),
    ]);
    render(<OrganizerPage />);
    await signIn();

    fireEvent.click(screen.getByRole("button", { name: "Search" }));

    expect(await screen.findByRole("button", { name: "Sign in" })).toBeVisible();
    expect(screen.getByRole("alert")).toHaveTextContent(WRONG_CREDENTIALS);
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("pages back and forth with the current filters", async () => {
    const searches = backend([
      ok({ total: 45, page: 1, items: [item({})] }),
      ok({ total: 41, page: 1, items: [item({})] }),
      ok({ total: 45, page: 2, items: [item({})] }),
      ok({ total: 45, page: 1, items: [item({})] }),
    ]);
    render(<OrganizerPage />);
    await signIn();
    fireEvent.change(screen.getByRole("combobox", { name: "Type" }), {
      target: { value: "STUDENT" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));
    expect(await screen.findByText("41 registrations")).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(await screen.findByText("Page 2 of 3")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Previous" }));
    expect(await screen.findByText("Page 1 of 3")).toBeVisible();

    expect(searches[2].searchParams.get("page")).toBe("2");
    expect(searches[2].searchParams.get("type")).toBe("STUDENT");
    expect(searches[3].searchParams.get("page")).toBe("1");
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
  });
});
