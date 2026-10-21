import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../App";
import type { SearchItem } from "../types";
import { HTTPS_REQUIRED, LOAD_FAILED, OrganizerPage, WRONG_CREDENTIALS } from "./OrganizerPage";

const config = {
  options: [
    { id: "ws-ai", name: "AI workshop", category: "WORKSHOP", availableTo: ["EXTERNAL"] },
    { id: "ml-lunch", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
  ],
  consents: [],
  captchaSiteKey: "",
};

function item(n: number, overrides: Partial<SearchItem> = {}): SearchItem {
  return {
    id: `id-${n}`,
    type: "EXTERNAL",
    firstName: `Ana${n}`,
    lastName: `Novak${n}`,
    email: `ana${n}@example.org`,
    organization: `Org ${n}`,
    studyInstitution: null,
    options: [],
    registeredAt: `2026-10-20T10:00:0${n % 10}Z`,
    ...overrides,
  };
}

type Answer = { status: number; body?: unknown } | Promise<Response>;

/** fetch replacement: form-config from `formConfig`, each search answered by the next answer. */
function backend(answers: Answer[], formConfig: Answer = { status: 200, body: config }) {
  const searches: URLSearchParams[] = [];
  const respond = (a: Answer) =>
    a instanceof Promise
      ? a
      : new Response(a.body === undefined ? "" : JSON.stringify(a.body), { status: a.status });
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: string) => {
      const url = new URL(input, "http://localhost");
      if (url.pathname === "/api/form-config") return respond(formConfig);
      searches.push(url.searchParams);
      const next = answers.shift();
      if (!next) throw new Error("unexpected search " + input);
      return respond(next);
    }),
  );
  return searches;
}

function found(items: SearchItem[], total = items.length, page = 1): Answer {
  return { status: 200, body: { total, page, items } };
}

async function signIn(username = "organizer", password = "pass") {
  fireEvent.change(await screen.findByLabelText("Username"), { target: { value: username } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

async function signedIn(answers: Answer[]) {
  const searches = backend(answers);
  render(<OrganizerPage />);
  await signIn();
  await screen.findByRole("heading", { name: "Registrations" });
  return searches;
}

function button(name: string) {
  return screen.getByRole("button", { name });
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.pushState({}, "", "/");
});

describe("OrganizerPage sign-in", () => {
  it("starts with an empty sign-in form and no alert", async () => {
    backend([]);
    render(<OrganizerPage />);

    expect(await screen.findByRole("heading", { name: "Organizer sign-in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveAttribute("type", "password");
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("asks for page 1 without filters and shows no alert after success", async () => {
    const searches = await signedIn([found([item(1)])]);

    expect(Object.fromEntries(searches[0])).toEqual({ page: "1" });
    expect(screen.queryByRole("alert")).toBeNull();
    expect(screen.getByLabelText("Search")).toHaveValue("");
  });

  it.each([
    [401, WRONG_CREDENTIALS],
    [403, HTTPS_REQUIRED],
    [500, LOAD_FAILED],
  ])("shows the message for HTTP %i and stays on the sign-in form", async (status, message) => {
    backend([{ status }]);
    render(<OrganizerPage />);

    await signIn();

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByRole("heading", { name: "Organizer sign-in" })).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("disables the sign-in button while the request runs", async () => {
    let release: (r: Response) => void = () => undefined;
    backend([new Promise<Response>((resolve) => (release = resolve))]);
    render(<OrganizerPage />);

    await signIn();

    await waitFor(() => expect(button("Sign in")).toBeDisabled());
    release(new Response("", { status: 401 }));
    await screen.findByRole("alert");
    expect(button("Sign in")).toBeEnabled();
  });
});

describe("OrganizerPage results", () => {
  it("shows each registration with option names, type and institution", async () => {
    await signedIn([
      found([
        item(1, { options: ["ws-ai", "retired"] }),
        item(2, {
          type: "STUDENT",
          organization: null,
          studyInstitution: "FRI",
          options: ["ml-lunch"],
        }),
        item(3, { organization: null, type: "OTHER" as SearchItem["type"] }),
      ]),
    ]);

    const rows = within(screen.getByRole("table")).getAllByRole("row").slice(1);
    const cells = rows.map((r) =>
      within(r)
        .getAllByRole("cell")
        .map((c) => c.textContent),
    );
    expect(cells[0]).toEqual([
      "Ana1 Novak1",
      "ana1@example.org",
      "External",
      "Org 1",
      "AI workshop, retired",
      "2026-10-20T10:00:01Z",
    ]);
    expect(cells[1].slice(2, 5)).toEqual(["Student", "FRI", "Lunch"]);
    expect(cells[2].slice(2, 5)).toEqual(["OTHER", "", ""]);
  });

  it("lists the configured options in the option filter", async () => {
    await signedIn([found([item(1)])]);

    const select = screen.getByRole("combobox", { name: "Option" }) as HTMLSelectElement;
    expect(Array.from(select.options).map((o) => [o.value, o.textContent])).toEqual([
      ["", "All options"],
      ["ws-ai", "AI workshop"],
      ["ml-lunch", "Lunch"],
    ]);
    const type = screen.getByRole("combobox", { name: "Type" }) as HTMLSelectElement;
    expect(Array.from(type.options).map((o) => [o.value, o.textContent])).toEqual([
      ["", "All types"],
      ["EXTERNAL", "External"],
      ["STUDENT", "Student"],
    ]);
  });

  it("shows option ids when the form configuration cannot be loaded", async () => {
    backend([found([item(1, { options: ["ws-ai"] })])], { status: 500 });
    render(<OrganizerPage />);
    await signIn();

    const table = await screen.findByRole("table");
    expect(within(table).getByText("ws-ai")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it.each([
    [1, "1 registration found"],
    [2, "2 registrations found"],
  ])("counts %i match(es)", async (total, text) => {
    await signedIn([found([item(1)], total)]);

    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it("says that nothing was found and shows no table for a total of 0", async () => {
    await signedIn([found([], 0)]);

    expect(screen.getByText("No registrations found.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.getByText("Page 1 of 1")).toBeInTheDocument();
    expect(button("Previous")).toBeDisabled();
    expect(button("Next")).toBeDisabled();
  });
});

describe("OrganizerPage paging", () => {
  it("moves between the pages of 20 and keeps the last search", async () => {
    const searches = await signedIn([
      found([item(1)], 40, 1),
      found([item(2)], 40, 1),
      found([item(3)], 40, 2),
      found([item(4)], 40, 1),
    ]);
    expect(screen.getByText("Page 1 of 2")).toBeInTheDocument();
    expect(button("Previous")).toBeDisabled();
    expect(button("Next")).toBeEnabled();

    fireEvent.change(screen.getByLabelText("Search"), { target: { value: "nov" } });
    fireEvent.change(screen.getByRole("combobox", { name: "Type" }), {
      target: { value: "EXTERNAL" },
    });
    fireEvent.click(button("Search"));
    await screen.findByText("Ana2 Novak2");
    // the draft text is not sent until the next search
    fireEvent.change(screen.getByLabelText("Search"), { target: { value: "draft" } });

    fireEvent.click(button("Next"));
    await screen.findByText("Page 2 of 2");
    expect(Object.fromEntries(searches[2])).toEqual({ text: "nov", type: "EXTERNAL", page: "2" });
    expect(button("Previous")).toBeEnabled();
    expect(button("Next")).toBeDisabled();

    fireEvent.click(button("Previous"));
    await screen.findByText("Ana4 Novak4");
    expect(Object.fromEntries(searches[3])).toEqual({ text: "nov", type: "EXTERNAL", page: "1" });
  });

  it("enables both buttons on a middle page", async () => {
    await signedIn([found([item(1)], 41, 2)]);

    expect(screen.getByText("Page 2 of 3")).toBeInTheDocument();
    expect(button("Previous")).toBeEnabled();
    expect(button("Next")).toBeEnabled();
  });
});

describe("OrganizerPage errors and sign-out", () => {
  it("shows field errors of a rejected search and clears them after a good search", async () => {
    await signedIn([
      found([item(1)]),
      { status: 400, body: { errors: [{ field: "text", message: "Text too long." }] } },
      found([item(2)]),
    ]);

    fireEvent.click(button("Search"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Text too long.");
    expect(screen.getByRole("table")).toBeInTheDocument();

    fireEvent.click(button("Search"));
    await screen.findByText("Ana2 Novak2");
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("returns to the sign-in form when a later search is refused", async () => {
    await signedIn([found([item(1)]), { status: 401 }]);

    fireEvent.click(button("Search"));

    expect(await screen.findByRole("alert")).toHaveTextContent(WRONG_CREDENTIALS);
    expect(screen.getByRole("heading", { name: "Organizer sign-in" })).toBeInTheDocument();
    expect(screen.queryByText("Ana1 Novak1")).toBeNull();
  });

  it("keeps the results when a later search fails", async () => {
    await signedIn([found([item(1)]), { status: 403 }]);

    fireEvent.click(button("Search"));

    expect(await screen.findByRole("alert")).toHaveTextContent(HTTPS_REQUIRED);
    expect(screen.getByText("Ana1 Novak1")).toBeInTheDocument();
  });

  it("forgets the password and the results on sign-out", async () => {
    await signedIn([
      found([item(1)]),
      { status: 400, body: { errors: [{ field: "x", message: "m" }] } },
    ]);
    fireEvent.change(screen.getByLabelText("Search"), { target: { value: "abc" } });
    fireEvent.click(button("Search"));
    await screen.findByRole("alert");

    fireEvent.click(button("Sign out"));

    expect(screen.getByRole("heading", { name: "Organizer sign-in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toHaveValue("organizer");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByRole("alert")).toBeNull();
    expect(screen.queryByText("Ana1 Novak1")).toBeNull();
  });

  it("starts with an empty search after signing in again", async () => {
    const searches = await signedIn([found([item(1)]), found([item(2)])]);
    fireEvent.change(screen.getByLabelText("Search"), { target: { value: "abc" } });
    fireEvent.click(button("Sign out"));

    await signIn();
    await screen.findByText("Ana2 Novak2");

    expect(screen.getByLabelText("Search")).toHaveValue("");
    expect(Object.fromEntries(searches[1])).toEqual({ page: "1" });
  });
});

describe("App routing", () => {
  it("shows the organizer page below /organizer", async () => {
    backend([]);
    window.history.pushState({}, "", "/organizer/search");
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Organizer sign-in" })).toBeInTheDocument();
  });

  it("shows the registration form on any other path", async () => {
    backend([]);
    window.history.pushState({}, "", "/register/organizer");
    render(<App />);

    expect(await screen.findByRole("radio", { name: "Student" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Organizer sign-in" })).toBeNull();
  });
});
