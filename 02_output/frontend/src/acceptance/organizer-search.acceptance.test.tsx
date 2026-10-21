// US-009 organizer search page: AC-009-12, AC-009-13 (SR-09).
import "@testing-library/jest-dom/vitest";
import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  ORGANIZER,
  choose,
  item,
  mockOrganizerBackend,
  renderOrganizerPage,
  signIn,
} from "./organizer-harness";

describe("US-009 organizer search page", () => {
  it("AC-009-12 valid credentials show the list with search, filters and page navigation", async () => {
    const items = Array.from({ length: 25 }, (_, i) => item(25 - i));
    const calls = mockOrganizerBackend(items);
    renderOrganizerPage();

    await signIn(ORGANIZER.username, ORGANIZER.password);

    const table = await screen.findByRole("table");
    expect(within(table).getByText("Last25", { exact: false })).toBeInTheDocument();
    expect(within(table).getByText("person6@example.org")).toBeInTheDocument();
    expect(within(table).queryByText("person5@example.org")).toBeNull();
    expect(screen.getByRole("textbox", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Type" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Option" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeInTheDocument();
    expect(calls[0].authorization).toBe(
      "Basic " + btoa(ORGANIZER.username + ":" + ORGANIZER.password),
    );

    // page navigation asks for page 2 and shows the remaining registrations
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    await waitFor(() =>
      expect(within(screen.getByRole("table")).getByText("person5@example.org")).toBeVisible(),
    );
    expect(calls.at(-1)?.params.get("page")).toBe("2");

    // the search sends the text, the type and the option filter
    fireEvent.change(screen.getByRole("textbox", { name: "Search" }), {
      target: { value: "Last1" },
    });
    choose("Type", "Student");
    choose("Option", "AI workshop");
    fireEvent.click(screen.getByRole("button", { name: "Search" }));
    await waitFor(() => expect(calls.at(-1)?.params.get("text")).toBe("Last1"));
    const last = calls.at(-1)!;
    expect(last.params.get("type")).toBe("STUDENT");
    expect(last.params.get("option")).toBe("ws-ai");
    expect(last.params.get("page") ?? "1").toBe("1");
  });

  it("AC-009-12 wrong credentials show an error and no registration data", async () => {
    const calls = mockOrganizerBackend([item(1), item(2)]);
    renderOrganizerPage();

    await signIn(ORGANIZER.username, "wrong-password");

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(calls.length).toBeGreaterThan(0);
    expect(screen.queryByRole("table")).toBeNull();
    const text = document.body.textContent ?? "";
    expect(text).not.toContain("Last1");
    expect(text).not.toContain("person2@example.org");
    expect(screen.queryByRole("textbox", { name: "Search" })).toBeNull();
  });

  it("AC-009-13 markup in registration data is shown as literal text and not executed", async () => {
    const w = window as unknown as { __xss?: number };
    delete w.__xss;
    const firstName = '<img src="x" onerror="window.__xss=1">';
    const lastName = "<script>window.__xss=2</script>";
    const organization = '<b onclick="window.__xss=3">Bold</b> & Co';
    mockOrganizerBackend([item(1, { firstName, lastName, organization })]);
    renderOrganizerPage();

    await signIn(ORGANIZER.username, ORGANIZER.password);

    const table = await screen.findByRole("table");
    const text = table.textContent ?? "";
    expect(text).toContain(firstName);
    expect(text).toContain(lastName);
    expect(text).toContain(organization);
    expect(table.querySelector("img")).toBeNull();
    expect(table.querySelector("script")).toBeNull();
    expect(table.querySelector("b")).toBeNull();
    expect(w.__xss).toBeUndefined();
  });
});
