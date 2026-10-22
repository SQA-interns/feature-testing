import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import { mockApi, PHOTO_TEXT, PRIVACY_TEXT } from "./support/mockApi";
import { chooseType, expectExactlyTextInputs, expectGroupOptions } from "./support/page";

describe("US-001 external participant form", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/");
    mockApi();
  });
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("AC-001-08: choosing external participant shows exactly the external fields", async () => {
    render(<App />);

    await chooseType("External participant");

    expectExactlyTextInputs(["First name", "Last name", "Email", "Organization / institution"]);
    expect(screen.queryByRole("textbox", { name: "Study institution" })).toBeNull();
    expect(screen.queryByRole("textbox", { name: "Student ID" })).toBeNull();
  });

  it("AC-001-08: active options are grouped as workshops, events, meals and other activities", async () => {
    render(<App />);

    await chooseType("External participant");

    expectGroupOptions("Workshops", ["AI workshop", "Cloud workshop"]);
    expectGroupOptions("Events", ["Gala evening"]);
    expectGroupOptions("Meals", ["Lunch"]);
    expectGroupOptions("Other activities", ["City tour"]);
    expect(screen.queryByRole("checkbox", { name: "Student night" })).toBeNull();
  });

  it("AC-001-08: every consent checkbox is unchecked", async () => {
    render(<App />);

    await chooseType("External participant");

    const consents = screen.getByRole("group", { name: "Consents" });
    const boxes = within(consents).getAllByRole("checkbox");
    expect(boxes).toHaveLength(2);
    expect(within(consents).getByRole("checkbox", { name: PRIVACY_TEXT })).not.toBeChecked();
    expect(within(consents).getByRole("checkbox", { name: PHOTO_TEXT })).not.toBeChecked();
    for (const box of screen.getAllByRole("checkbox")) expect(box).not.toBeChecked();
  });
});
