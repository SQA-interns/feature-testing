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

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields, the grouped active options and unchecked consents", async () => {
    fakeBackend();
    renderApp("/");

    expect(await screen.findByRole("radio", { name: "External participant" })).not.toBeChecked();
    expect(textBoxNames()).toEqual([]);

    await chooseType("External participant");

    expect(textBoxNames()).toEqual([
      "First name",
      "Last name",
      "Email",
      "Organization / institution",
    ]);
    expect(optionsInGroup("Workshops")).toEqual(["AI workshop", "Leadership workshop"]);
    expect(optionsInGroup("Events")).toEqual(["Conference dinner", "Gala evening"]);
    expect(optionsInGroup("Meals")).toEqual(["Lunch"]);
    expect(optionsInGroup("Other activities")).toEqual(["City tour"]);
    for (const option of formConfig.options) {
      expect(screen.getByRole("checkbox", { name: option.name })).not.toBeChecked();
    }
    for (const consent of formConfig.consents) {
      expect(screen.getByRole("checkbox", { name: new RegExp(consent.text) })).not.toBeChecked();
    }
  });
});
