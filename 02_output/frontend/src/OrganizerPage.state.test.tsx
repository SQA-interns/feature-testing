import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { basicHeader, mockApi, ORGANIZER } from "../tests/acceptance/support/mockApi";
import { App } from "./App";
import { OrganizerPage } from "./OrganizerPage";

function api(formConfigPending = false) {
  const searches: string[] = [];
  mockApi((call) => {
    if (call.url.pathname === "/api/form-config" && formConfigPending) return undefined;
    if (call.url.pathname !== "/api/registrations") return undefined;
    if (call.headers.get("Authorization") !== basicHeader(ORGANIZER.username, ORGANIZER.password))
      return { status: 401 };
    searches.push(call.url.searchParams.toString());
    return { status: 200, body: { total: 0, page: 1, items: [] } };
  });
  return searches;
}

async function signIn(password = ORGANIZER.password) {
  fireEvent.change(await screen.findByRole("textbox", { name: "Username" }), {
    target: { value: ORGANIZER.username },
  });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

describe("OrganizerPage state", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    window.history.pushState({}, "", "/");
  });

  it("starts with empty credentials, no alert and asks for the first page only", async () => {
    const searches = api();
    render(<OrganizerPage />);

    expect(await screen.findByRole("textbox", { name: "Username" })).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByRole("alert")).toBeNull();
    await signIn();

    expect(await screen.findByRole("searchbox", { name: "Search" })).toHaveValue("");
    expect(searches).toEqual(["page=1"]);
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("removes the login error after a successful sign-in", async () => {
    api();
    render(<OrganizerPage />);
    await signIn("wrong");
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password.");

    await signIn();

    await screen.findByRole("table", { name: "Registrations" });
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("clears the search text on sign-out", async () => {
    const searches = api();
    render(<OrganizerPage />);
    await signIn();
    fireEvent.change(await screen.findByRole("searchbox", { name: "Search" }), {
      target: { value: "Novak" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));
    await waitFor(() => expect(searches).toHaveLength(2));

    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
    await signIn();

    expect(await screen.findByRole("searchbox", { name: "Search" })).toHaveValue("");
    await waitFor(() => expect(searches).toHaveLength(3));
    expect(searches[2]).toBe("page=1");
  });

  it("offers no option filter values before the configuration arrives", async () => {
    api(true);
    vi.stubGlobal(
      "fetch",
      ((original) => (input: RequestInfo | URL, init?: RequestInit) =>
        String(input) === "/api/form-config" ? new Promise(() => {}) : original(input, init))(
        globalThis.fetch,
      ),
    );
    render(<OrganizerPage />);
    await signIn();

    const option = await screen.findByRole("combobox", { name: "Option" });
    expect(
      within(option)
        .getAllByRole("option")
        .map((o) => o.textContent),
    ).toEqual(["All options"]);
  });

  it("routes paths below /organizer to the organizer page", async () => {
    api();
    window.history.pushState({}, "", "/organizer/");

    render(<App />);

    expect(
      await screen.findByRole("heading", { name: "Organizer registrations" }),
    ).toBeInTheDocument();
  });
});
