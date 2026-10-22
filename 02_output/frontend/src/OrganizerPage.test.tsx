import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  basicHeader,
  mockApi,
  ORGANIZER,
  type ApiAnswer,
  type ApiCall,
} from "../tests/acceptance/support/mockApi";
import { OrganizerPage, PAGE_SIZE } from "./OrganizerPage";
import type { SearchItem } from "./types";

function item(n: number, overrides: Partial<SearchItem> = {}): SearchItem {
  return {
    id: `id-${n}`,
    type: "EXTERNAL",
    firstName: `First${n}`,
    lastName: `Last${n}`,
    email: `p${n}@example.org`,
    organization: `Org${n}`,
    studyInstitution: null,
    options: [],
    registeredAt: "2026-10-20T10:15:00Z",
    ...overrides,
  };
}

/** Answers searches with `total` matches; records each search's query parameters. */
function organizerApi(
  total: number,
  items: SearchItem[],
  override?: (call: ApiCall) => ApiAnswer | undefined,
) {
  const searches: URLSearchParams[] = [];
  const calls = mockApi((call) => {
    if (call.url.pathname !== "/api/registrations") return undefined;
    const forced = override?.(call);
    if (forced) return forced;
    if (call.headers.get("Authorization") !== basicHeader(ORGANIZER.username, ORGANIZER.password))
      return { status: 401 };
    searches.push(call.url.searchParams);
    return {
      status: 200,
      body: { total, page: Number(call.url.searchParams.get("page")), items },
    };
  });
  return { calls, searches };
}

async function signIn(password = ORGANIZER.password) {
  fireEvent.change(await screen.findByRole("textbox", { name: "Username" }), {
    target: { value: ORGANIZER.username },
  });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

describe("OrganizerPage", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("pages forward and back with the page buttons", async () => {
    const { searches } = organizerApi(PAGE_SIZE * 2 + 1, [item(1)]);
    render(<OrganizerPage />);
    await signIn();
    expect(await screen.findByText("Page 1 of 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(await screen.findByText("Page 2 of 3")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(await screen.findByText("Page 3 of 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Previous" }));

    expect(await screen.findByText("Page 2 of 3")).toBeInTheDocument();
    expect(searches.map((s) => s.get("page"))).toEqual(["1", "2", "3", "2"]);
    expect(screen.getByText("41 registrations found")).toBeInTheDocument();
  });

  it("an exact multiple of the page size has no extra page", async () => {
    organizerApi(PAGE_SIZE, [item(1)]);
    render(<OrganizerPage />);
    await signIn();

    expect(await screen.findByText("Page 1 of 1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
    expect(PAGE_SIZE).toBe(20);
  });

  it("applies search text on submit and filters on change, from page one", async () => {
    const { searches } = organizerApi(1, [item(1)]);
    render(<OrganizerPage />);
    await signIn();
    await screen.findByRole("table", { name: "Registrations" });
    expect(
      within(screen.getByRole("combobox", { name: "Option" })).getAllByRole("option"),
    ).toHaveLength(7);

    fireEvent.change(screen.getByRole("searchbox", { name: "Search" }), {
      target: { value: "Nov" },
    });
    expect(searches).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: "Search" }));
    await waitFor(() => expect(searches).toHaveLength(2));
    fireEvent.change(screen.getByRole("combobox", { name: "Type" }), {
      target: { value: "STUDENT" },
    });
    await waitFor(() => expect(searches).toHaveLength(3));
    fireEvent.change(screen.getByRole("combobox", { name: "Option" }), {
      target: { value: "ws-ai" },
    });
    await waitFor(() => expect(searches).toHaveLength(4));

    expect(searches[1].toString()).toBe("text=Nov&page=1");
    expect(searches[2].toString()).toBe("text=Nov&type=STUDENT&page=1");
    expect(searches[3].toString()).toBe("text=Nov&type=STUDENT&option=ws-ai&page=1");
  });

  it("shows types, the organization or institution, options and raw invalid times", async () => {
    organizerApi(2, [
      item(1, {
        type: "STUDENT",
        organization: null,
        studyInstitution: "Uni",
        options: ["a", "b"],
      }),
      item(2, { organization: null, registeredAt: "not-a-time" }),
    ]);
    render(<OrganizerPage />);
    await signIn();

    const rows = within(await screen.findByRole("table", { name: "Registrations" })).getAllByRole(
      "row",
    );
    expect(rows).toHaveLength(3);
    expect(
      within(rows[1])
        .getAllByRole("cell")
        .map((c) => c.textContent),
    ).toEqual([
      new Date("2026-10-20T10:15:00Z").toLocaleString(),
      "Student",
      "First1",
      "Last1",
      "p1@example.org",
      "Uni",
      "a, b",
    ]);
    const second = within(rows[2])
      .getAllByRole("cell")
      .map((c) => c.textContent);
    expect(second[0]).toBe("not-a-time");
    expect(second[1]).toBe("External");
    expect(second[5]).toBe("");
  });

  it("signing out forgets the credentials and the list", async () => {
    organizerApi(1, [item(1)]);
    render(<OrganizerPage />);
    await signIn();
    await screen.findByRole("table", { name: "Registrations" });

    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.getByRole("textbox", { name: "Username" })).toHaveValue(ORGANIZER.username);
  });

  it("returns to the login when the credentials stop working", async () => {
    let reject = false;
    organizerApi(30, [item(1)], () => (reject ? { status: 401 } : undefined));
    render(<OrganizerPage />);
    await signIn();
    await screen.findByRole("table", { name: "Registrations" });
    reject = true;

    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("Invalid username or password.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("shows a load failure without signing in and clears it on success", async () => {
    let failing = true;
    organizerApi(1, [item(1)], () => (failing ? { status: 500 } : undefined));
    render(<OrganizerPage />);

    await signIn();
    expect(await screen.findByText("Registrations could not be loaded.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    failing = false;
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("table", { name: "Registrations" })).toBeInTheDocument();
    expect(screen.queryByText("Registrations could not be loaded.")).toBeNull();
  });

  it("works without options when the form configuration fails", async () => {
    mockApi((call) => {
      if (call.url.pathname === "/api/form-config") return { status: 500 };
      return { status: 200, body: { total: 0, page: 1, items: [] } };
    });
    render(<OrganizerPage />);
    await signIn();

    const option = await screen.findByRole("combobox", { name: "Option" });
    expect(within(option).getAllByRole("option")).toHaveLength(1);
    expect(screen.getByText("0 registrations found")).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 1")).toBeInTheDocument();
  });
});
