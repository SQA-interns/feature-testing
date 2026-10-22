import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import { mockApi, PHOTO_TEXT, PRIVACY_TEXT } from "./support/mockApi";
import { chooseType, expectExactlyTextInputs, expectGroupOptions } from "./support/page";

describe("US-002 student form", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/");
    mockApi();
  });
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("AC-002-07: choosing student shows exactly the student fields", async () => {
    render(<App />);

    await chooseType("Student");

    expectExactlyTextInputs([
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ]);
    expect(screen.queryByRole("textbox", { name: "Organization / institution" })).toBeNull();
  });

  it("AC-002-07: only active options available to students are shown, grouped by category", async () => {
    render(<App />);

    await chooseType("Student");

    expectGroupOptions("Workshops", ["AI workshop"]);
    expectGroupOptions("Events", ["Gala evening", "Student night"]);
    expectGroupOptions("Meals", ["Lunch"]);
    expectGroupOptions("Other activities", ["City tour"]);
    expect(screen.queryByRole("checkbox", { name: "Cloud workshop" })).toBeNull();
  });

  it("AC-002-07: every consent checkbox is unchecked", async () => {
    render(<App />);

    await chooseType("Student");

    const consents = screen.getByRole("group", { name: "Consents" });
    expect(within(consents).getByRole("checkbox", { name: PRIVACY_TEXT })).not.toBeChecked();
    expect(within(consents).getByRole("checkbox", { name: PHOTO_TEXT })).not.toBeChecked();
    for (const box of screen.getAllByRole("checkbox")) expect(box).not.toBeChecked();
  });

  it("AC-002-07: switching from external to student hides the external-only option", async () => {
    render(<App />);

    await chooseType("External participant");
    expect(screen.getByRole("checkbox", { name: "Cloud workshop" })).toBeInTheDocument();
    await chooseType("Student");

    expect(screen.queryByRole("checkbox", { name: "Cloud workshop" })).toBeNull();
    expect(screen.getByRole("checkbox", { name: "Student night" })).toBeInTheDocument();
  });
});
