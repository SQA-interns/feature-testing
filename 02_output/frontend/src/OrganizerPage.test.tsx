import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { searchRegistrations, type SearchOutcome } from "./api";
import { OrganizerPage } from "./OrganizerPage";
import type { SearchResult } from "./types";

vi.mock("./api", async (original) => ({
  ...(await original<typeof import("./api")>()),
  searchRegistrations: vi.fn(),
}));

const search = vi.mocked(searchRegistrations);

const NO_QUERY = {
  search: "",
  type: "",
  workshop: "",
  event: "",
  meal: "",
  other: "",
};

function resultWith(lastName: string): SearchResult {
  return {
    count: 1,
    registrations: [
      {
        id: lastName,
        type: "EXTERNAL",
        registeredAt: "2026-10-27T10:00:00Z",
        firstName: "Ana",
        lastName,
        email: "a@example.org",
        organization: "O",
        options: [],
        consents: [],
      },
    ],
    optionCounts: [
      {
        category: "OTHER",
        options: [
          { id: "other-tour", name: "City tour", active: true, count: 0 },
        ],
      },
    ],
  };
}

const ok = (lastName = "Novak"): SearchOutcome => ({
  kind: "ok",
  result: resultWith(lastName),
});

async function loggedIn() {
  render(<OrganizerPage />);
  fireEvent.change(screen.getByLabelText("Username"), {
    target: { value: "organizer" },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: "secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
  await screen.findByText("Matches: 1");
}

function searchFor(text: string) {
  fireEvent.change(screen.getByLabelText("Search"), {
    target: { value: text },
  });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
}

const LOAD_FAILED =
  "The registrations could not be loaded. Please try again later.";
const TOO_LONG = "The search text can have at most 100 characters.";

beforeEach(() => {
  search.mockReset();
});

describe("OrganizerPage login", () => {
  it("starts with empty fields and no alert", () => {
    render(<OrganizerPage />);

    expect(screen.getByLabelText("Username")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("logs in with the entered credentials and no criteria", async () => {
    search.mockResolvedValue(ok());

    await loggedIn();

    expect(search).toHaveBeenCalledWith(
      { username: "organizer", password: "secret" },
      NO_QUERY,
    );
    expect(screen.getByLabelText("Search")).toHaveValue("");
  });

  it("disables Log in while the request runs", async () => {
    let finish: (o: SearchOutcome) => void = () => {};
    search.mockReturnValue(new Promise((resolve) => (finish = resolve)));
    render(<OrganizerPage />);

    fireEvent.click(screen.getByRole("button", { name: "Log in" }));

    expect(screen.getByRole("button", { name: "Log in" })).toBeDisabled();
    finish({ kind: "unauthorized" });
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Log in" })).toBeEnabled(),
    );
  });

  it("shows the general alert when the login request fails, and keeps the form", async () => {
    search.mockResolvedValue({ kind: "failed" });
    render(<OrganizerPage />);

    fireEvent.click(screen.getByRole("button", { name: "Log in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(LOAD_FAILED);
    expect(screen.getByRole("button", { name: "Log in" })).toBeInTheDocument();
  });

  it("clears the wrong-credentials alert when the next login succeeds", async () => {
    search.mockResolvedValueOnce({ kind: "unauthorized" });
    render(<OrganizerPage />);
    fireEvent.click(screen.getByRole("button", { name: "Log in" }));
    await screen.findByText("Wrong username or password.");

    search.mockResolvedValueOnce(ok());
    fireEvent.click(screen.getByRole("button", { name: "Log in" }));

    await screen.findByText("Matches: 1");
    expect(screen.queryByRole("alert")).toBeNull();
  });
});

describe("OrganizerPage search", () => {
  it.each([
    ["exactly 100 characters", "a".repeat(100)],
    ["100 characters with surrounding spaces", "  " + "a".repeat(100) + " "],
  ])("sends %s", async (_case, text) => {
    search.mockResolvedValue(ok());
    await loggedIn();

    searchFor(text);

    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    expect(search.mock.calls[1][1].search).toBe(text);
    expect(screen.queryByText(TOO_LONG)).toBeNull();
  });

  it("hides the error and shows the list again after a valid search", async () => {
    search.mockResolvedValue(ok());
    await loggedIn();
    searchFor("a".repeat(101));
    await screen.findByText(TOO_LONG);

    search.mockResolvedValue(ok("Kranjc"));
    searchFor("kranjc");

    expect(await screen.findByText("Kranjc")).toBeInTheDocument();
    expect(screen.queryByText(TOO_LONG)).toBeNull();
    expect(screen.getByLabelText("Search")).not.toHaveAttribute("aria-invalid");
  });

  it("shows the search error for a 400 on the search field and no list", async () => {
    search.mockResolvedValueOnce(ok());
    await loggedIn();
    search.mockResolvedValueOnce({
      kind: "rejected",
      fieldErrors: [{ field: "search", code: "TOO_LONG", message: "m" }],
    });

    searchFor("x");

    expect(await screen.findByText(TOO_LONG)).toBeInTheDocument();
    expect(screen.queryByText("Novak")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("shows the general alert for a 400 on a filter and no list", async () => {
    search.mockResolvedValueOnce(ok());
    await loggedIn();
    search.mockResolvedValueOnce({
      kind: "rejected",
      fieldErrors: [{ field: "meal", code: "UNKNOWN_OPTION", message: "m" }],
    });

    searchFor("x");

    expect(await screen.findByRole("alert")).toHaveTextContent(LOAD_FAILED);
    expect(screen.queryByText("Novak")).toBeNull();
    expect(screen.queryByText(TOO_LONG)).toBeNull();
  });

  it("shows the general alert when a search fails and clears it on the next result", async () => {
    search.mockResolvedValueOnce(ok());
    await loggedIn();
    search.mockResolvedValueOnce({ kind: "failed" });
    searchFor("x");
    expect(await screen.findByRole("alert")).toHaveTextContent(LOAD_FAILED);

    search.mockResolvedValueOnce(ok("Kranjc"));
    searchFor("y");

    expect(await screen.findByText("Kranjc")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("returns to the login form when a later search is refused, and resets the criteria at the next login", async () => {
    search.mockResolvedValueOnce(ok());
    await loggedIn();
    search.mockResolvedValueOnce(ok());
    fireEvent.change(screen.getByLabelText("Registration type"), {
      target: { value: "STUDENT" },
    });
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    search.mockResolvedValueOnce({ kind: "unauthorized" });
    searchFor("novak");

    expect(
      await screen.findByText("Wrong username or password."),
    ).toBeInTheDocument();
    expect(screen.queryByText("Novak")).toBeNull();
    expect(screen.queryByLabelText("Search")).toBeNull();

    search.mockResolvedValueOnce(ok());
    fireEvent.click(screen.getByRole("button", { name: "Log in" }));
    await screen.findByText("Matches: 1");
    expect(search.mock.calls[3][1]).toEqual(NO_QUERY);
    expect(screen.getByLabelText("Registration type")).toHaveValue("");
    expect(screen.getByLabelText("Search")).toHaveValue("");
  });

  it("ignores a response that arrives after a newer one", async () => {
    search.mockResolvedValueOnce(ok());
    await loggedIn();
    let slow: (o: SearchOutcome) => void = () => {};
    search.mockReturnValueOnce(new Promise((resolve) => (slow = resolve)));
    searchFor("old");
    search.mockResolvedValueOnce(ok("Newest"));
    searchFor("new");
    await screen.findByText("Newest");

    slow(ok("Stale"));

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(screen.getByText("Newest")).toBeInTheDocument();
    expect(screen.queryByText("Stale")).toBeNull();
  });

  it("sends each category filter under its own name", async () => {
    search.mockResolvedValue(ok());
    await loggedIn();

    fireEvent.change(screen.getByLabelText("Other activity"), {
      target: { value: "other-tour" },
    });

    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    expect(search.mock.calls[1][1]).toEqual({
      ...NO_QUERY,
      other: "other-tour",
    });
    for (const label of ["Workshop", "Event", "Meal"]) {
      expect(
        within(screen.getByLabelText(label)).getAllByRole("option"),
      ).toHaveLength(1);
    }
  });
});
