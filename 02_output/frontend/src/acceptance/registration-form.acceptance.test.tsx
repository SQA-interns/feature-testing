// AC-001-08 and AC-002-07: the registration page shows exactly the fields, options and consents
// of the chosen registration type.
import "@testing-library/jest-dom/vitest";
import { fireEvent, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  GROUPS,
  LABELS,
  field,
  mockBackend,
  optionNamesIn,
  queryField,
  renderAndChoose,
} from "./harness";

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields, grouped active options and unchecked consents", async () => {
    mockBackend({ status: 201, body: { id: "unused" } });
    await renderAndChoose("External participant");

    for (const label of [LABELS.firstName, LABELS.lastName, LABELS.email, LABELS.organization]) {
      expect(field(label)).toBeVisible();
    }
    for (const label of [LABELS.studyInstitution, LABELS.studyProgramme, LABELS.studentId]) {
      expect(queryField(label)).toBeNull();
    }
    expect(screen.getAllByRole("textbox")).toHaveLength(4);

    expect(optionNamesIn(GROUPS.WORKSHOP)).toEqual([
      "AI workshop",
      "Professional practice workshop",
    ]);
    expect(optionNamesIn(GROUPS.EVENT)).toEqual(["Gala dinner"]);
    expect(optionNamesIn(GROUPS.MEAL)).toEqual(["Lunch"]);
    expect(optionNamesIn(GROUPS.OTHER)).toEqual(["City tour"]);
    expect(screen.queryByRole("checkbox", { name: /Student meetup/ })).toBeNull();

    expect(
      screen.getByRole("checkbox", { name: /^I agree that my personal data/ }),
    ).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: /^I agree to be photographed/ })).not.toBeChecked();
    for (const box of screen.getAllByRole("checkbox")) {
      expect(box).not.toBeChecked();
    }
  });
});

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields and only student options, all unchecked", async () => {
    mockBackend({ status: 201, body: { id: "unused" } });
    await renderAndChoose("Student");

    for (const label of [
      LABELS.firstName,
      LABELS.lastName,
      LABELS.email,
      LABELS.studyInstitution,
      LABELS.studyProgramme,
      LABELS.studentId,
    ]) {
      expect(field(label)).toBeVisible();
    }
    expect(queryField(LABELS.organization)).toBeNull();
    expect(screen.getAllByRole("textbox")).toHaveLength(6);

    expect(optionNamesIn(GROUPS.WORKSHOP)).toEqual(["AI workshop"]);
    expect(optionNamesIn(GROUPS.EVENT)).toEqual(["Gala dinner", "Student meetup"]);
    expect(optionNamesIn(GROUPS.MEAL)).toEqual(["Lunch"]);
    expect(optionNamesIn(GROUPS.OTHER)).toEqual(["City tour"]);
    expect(screen.queryByRole("checkbox", { name: /Professional practice workshop/ })).toBeNull();

    expect(
      screen.getByRole("checkbox", { name: /^I agree that my personal data/ }),
    ).not.toBeChecked();
    for (const box of screen.getAllByRole("checkbox")) {
      expect(box).not.toBeChecked();
    }
  });

  it("AC-002-07 switching from external to student replaces the organization field", async () => {
    mockBackend({ status: 201, body: { id: "unused" } });
    await renderAndChoose("External participant");
    expect(field(LABELS.organization)).toBeVisible();

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(await screen.findByLabelText(/^Student ID\b/)).toBeVisible();
    expect(queryField(LABELS.organization)).toBeNull();
  });
});
