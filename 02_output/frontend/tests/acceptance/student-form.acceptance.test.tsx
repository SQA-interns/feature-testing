import "@testing-library/jest-dom/vitest";
import { render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import { chooseType, optionGroups, optionIn, stubFetch, textboxNames } from "./support";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("Student");

    expect(textboxNames()).toEqual(
      ["email", "firstName", "lastName", "studentId", "studyInstitution", "studyProgramme"].sort(),
    );
    expect(screen.getByRole("textbox", { name: "Study institution" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Study programme" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Student ID" })).toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "Organization / institution" }),
    ).not.toBeInTheDocument();
  });

  it("AC-002-07 shows only the active options available to students, grouped by category", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("Student");

    expect(optionGroups()).toEqual([
      { legend: "Workshops", values: ["ws-ai", "ws-thesis"] },
      { legend: "Events", values: ["ev-gala"] },
      { legend: "Meals", values: ["meal-lunch"] },
      { legend: "Other activities", values: ["other-cert"] },
    ]);
    expect(optionIn("Workshops", "Thesis writing workshop").value).toBe("ws-thesis");
    expect(screen.queryByRole("checkbox", { name: "Industry tour" })).not.toBeInTheDocument();
  });

  it("AC-002-07 shows every consent unchecked", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("Student");

    const boxes = within(screen.getByRole("group", { name: "Consents" })).getAllByRole("checkbox");
    expect(boxes).toHaveLength(2);
    boxes.forEach((box) => expect(box).not.toBeChecked());
  });

  it("AC-002-07 switching from external to student removes the external-only fields and options", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("External participant");
    await chooseType("Student");

    expect(
      screen.queryByRole("textbox", { name: "Organization / institution" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: "Industry tour" })).not.toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Student ID" })).toBeInTheDocument();
  });
});
