import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  chooseType,
  fakeBackend,
  formConfig,
  optionsInGroup,
  renderApp,
  textBoxNames,
} from "./support";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields and only the options available to students", async () => {
    fakeBackend();
    renderApp("/");

    await chooseType("Student");

    expect(textBoxNames()).toEqual([
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ]);
    expect(optionsInGroup("Workshops")).toEqual(["AI workshop"]);
    expect(optionsInGroup("Events")).toEqual(["Conference dinner"]);
    expect(optionsInGroup("Meals")).toEqual(["Lunch"]);
    expect(optionsInGroup("Other activities")).toEqual(["City tour"]);
    expect(screen.queryByRole("checkbox", { name: "Leadership workshop" })).toBeNull();
    expect(screen.queryByRole("checkbox", { name: "Gala evening" })).toBeNull();
    for (const consent of formConfig.consents) {
      expect(screen.getByRole("checkbox", { name: new RegExp(consent.text) })).not.toBeChecked();
    }
  });

  it("AC-002-07 switching from external to student replaces the fields", async () => {
    fakeBackend();
    renderApp("/");

    await chooseType("External participant");
    expect(textBoxNames()).toContain("Organization / institution");
    await chooseType("Student");

    expect(textBoxNames()).not.toContain("Organization / institution");
    expect(textBoxNames()).toContain("Student ID");
    expect(screen.queryByRole("checkbox", { name: "Leadership workshop" })).toBeNull();
  });
});
