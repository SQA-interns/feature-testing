import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import App from "../App";
import {
  ERROR_TOO_LONG,
  EXTERNAL,
  OPTION_COUNTS,
  STUDENT,
  choose,
  entries,
  logIn,
  param,
  registrationItems,
  result,
  stubSearchApi,
} from "./support/organizer";

function openOrganizerPage() {
  window.history.pushState({}, "", "/organizer");
  return render(<App />);
}

beforeEach(() => {
  window.localStorage.clear();
  window.sessionStorage.clear();
});

afterEach(() => {
  window.history.pushState({}, "", "/");
});

describe("US-009 organizer search bar, filters and counts", () => {
  it("AC-009-17 a search text over 100 characters shows an error next to the search bar and no result list", async () => {
    const api = stubSearchApi(() => ({
      status: 200,
      body: result([EXTERNAL]),
    }));
    openOrganizerPage();
    logIn();
    await registrationItems();

    const search = screen.getByLabelText("Search");
    fireEvent.change(search, { target: { value: "a".repeat(101) } });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));

    expect(await screen.findByText(ERROR_TOO_LONG)).toBeInTheDocument();
    expect(search).toHaveAttribute("aria-invalid", "true");
    expect(search).toHaveAccessibleDescription(ERROR_TOO_LONG);
    expect(screen.queryByRole("list", { name: "Registrations" })).toBeNull();
    expect(screen.queryByText(/Novak/)).toBeNull();
    expect(api.calls.every((call) => param(call, "search").length <= 100)).toBe(
      true,
    );
  });

  it("AC-009-21 each category filter offers any and every configured option, active and inactive", async () => {
    stubSearchApi(() => ({ status: 200, body: result([EXTERNAL]) }));
    openOrganizerPage();
    logIn();
    await registrationItems();

    const optionNames = (label: string) =>
      within(screen.getByLabelText(label))
        .getAllByRole("option")
        .map((o) => o.textContent);
    expect(optionNames("Workshop")).toEqual([
      "Any",
      "Workshop: Testing in practice",
      "Workshop: Legacy systems",
    ]);
    expect(optionNames("Event")).toEqual(["Any", "Conference dinner"]);
    expect(optionNames("Meal")).toEqual(["Any", "Lunch (vegetarian)"]);
    expect(optionNames("Other activity")).toEqual(["Any", "City tour"]);
    expect(optionNames("Registration type")).toEqual([
      "All",
      "External participant",
      "Student",
    ]);
  });

  it("AC-009-23 no match shows 'No registrations match' and the number 0", async () => {
    const zero = OPTION_COUNTS.map((c) => ({
      ...c,
      options: c.options.map((o) => ({ ...o, count: 0 })),
    }));
    stubSearchApi(() => ({ status: 200, body: result([], zero) }));
    openOrganizerPage();

    logIn();

    expect(
      await screen.findByText("No registrations match"),
    ).toBeInTheDocument();
    expect(screen.getByText("Matches: 0")).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "Registrations" })).toBeNull();
  });

  it("AC-009-24 every configured option is listed by category with its count, zero included", async () => {
    stubSearchApi(() => ({
      status: 200,
      body: result([EXTERNAL, STUDENT]),
    }));
    openOrganizerPage();
    logIn();
    await registrationItems();

    const region = screen.getByRole("region", { name: "Option counts" });
    const headings = within(region)
      .getAllByRole("heading")
      .map((h) => h.textContent);
    expect(headings).toEqual([
      "Workshops",
      "Events",
      "Meals",
      "Other activities",
    ]);
    for (const text of [
      "Workshop: Testing in practice: 2",
      "Workshop: Legacy systems: 0",
      "Conference dinner: 1",
      "Lunch (vegetarian): 2",
      "City tour: 0",
    ]) {
      expect(within(region).getByText(text)).toBeInTheDocument();
    }
  });

  it("AC-009-26 changing a filter updates the list and the counts without pressing Search", async () => {
    const studentCounts = OPTION_COUNTS.map((c) => ({
      ...c,
      options: c.options.map((o) => ({
        ...o,
        count: o.id === "ws-testing" || o.id === "meal-lunch" ? 1 : 0,
      })),
    }));
    const api = stubSearchApi((call) =>
      param(call, "type") === "STUDENT"
        ? { status: 200, body: result([STUDENT], studentCounts) }
        : { status: 200, body: result([EXTERNAL, STUDENT]) },
    );
    openOrganizerPage();
    logIn();
    await registrationItems();

    choose("Registration type", "Student");

    await waitFor(() =>
      expect(screen.getByText("Matches: 1")).toBeInTheDocument(),
    );
    const items = await registrationItems();
    expect(items).toHaveLength(1);
    expect(entries(items[0])["Last name"]).toBe("Kranjc");
    const region = screen.getByRole("region", { name: "Option counts" });
    expect(
      within(region).getByText("Lunch (vegetarian): 1"),
    ).toBeInTheDocument();
    expect(api.calls).toHaveLength(2);
    expect(param(api.calls[1], "type")).toBe("STUDENT");

    choose("Meal", "Lunch (vegetarian)");

    await waitFor(() => expect(api.calls).toHaveLength(3));
    expect(param(api.calls[2], "meal")).toBe("meal-lunch");
    expect(param(api.calls[2], "type")).toBe("STUDENT");
  });

  it.each([
    [
      "the Search button",
      () => fireEvent.click(screen.getByRole("button", { name: "Search" })),
    ],
    [
      "Enter in the search bar",
      () =>
        fireEvent.submit(
          screen.getByRole("search", { name: "Search registrations" }),
        ),
    ],
  ])(
    "AC-009-27 a search text with %s updates the list and the counts",
    async (_how, press) => {
      const novakCounts = OPTION_COUNTS.map((c) => ({
        ...c,
        options: c.options.map((o) => ({
          ...o,
          count: ["ws-testing", "ev-dinner", "meal-lunch"].includes(o.id)
            ? 1
            : 0,
        })),
      }));
      const api = stubSearchApi((call) =>
        param(call, "search") === "novak"
          ? { status: 200, body: result([EXTERNAL], novakCounts) }
          : { status: 200, body: result([EXTERNAL, STUDENT]) },
      );
      openOrganizerPage();
      logIn();
      await registrationItems();

      fireEvent.change(screen.getByLabelText("Search"), {
        target: { value: "novak" },
      });
      press();

      await waitFor(() =>
        expect(screen.getByText("Matches: 1")).toBeInTheDocument(),
      );
      const items = await registrationItems();
      expect(items).toHaveLength(1);
      expect(entries(items[0])["Last name"]).toBe("Novak");
      const region = screen.getByRole("region", { name: "Option counts" });
      expect(
        within(region).getByText("Lunch (vegetarian): 1"),
      ).toBeInTheDocument();
      expect(param(api.calls[api.calls.length - 1], "search")).toBe("novak");
    },
  );
});
