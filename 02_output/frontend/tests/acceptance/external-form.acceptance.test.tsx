import "@testing-library/jest-dom/vitest";
import { render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import {
  CAPTCHA,
  PRIVACY,
  chooseType,
  optionGroups,
  optionIn,
  stubFetch,
  textboxNames,
} from "./support";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("External participant");

    expect(textboxNames()).toEqual(["email", "firstName", "lastName", "organization"].sort());
    expect(screen.getByRole("textbox", { name: "First name" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Last name" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Email" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Organization / institution" })).toBeInTheDocument();
    expect(screen.queryByRole("textbox", { name: "Student ID" })).not.toBeInTheDocument();
    expect(screen.queryByRole("textbox", { name: "Study institution" })).not.toBeInTheDocument();
  });

  it("AC-001-08 groups the active options available to external participants by category", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("External participant");

    expect(optionGroups()).toEqual([
      { legend: "Workshops", values: ["ws-ai"] },
      { legend: "Events", values: ["ev-gala", "ev-tour"] },
      { legend: "Meals", values: ["meal-lunch"] },
      { legend: "Other activities", values: ["other-cert"] },
    ]);
    expect(optionIn("Workshops", "AI workshop").value).toBe("ws-ai");
    expect(optionIn("Events", "Industry tour").value).toBe("ev-tour");
    expect(optionIn("Meals", "Lunch").value).toBe("meal-lunch");
    expect(optionIn("Other activities", "Certificate of attendance").value).toBe("other-cert");
    expect(
      screen.queryByRole("checkbox", { name: "Thesis writing workshop" }),
    ).not.toBeInTheDocument();
  });

  it("AC-001-08 shows every consent and option unchecked", async () => {
    stubFetch({ status: 201, body: { id: "x" } });
    render(<App />);

    await chooseType("External participant");

    const consents = screen.getByRole("group", { name: "Consents" });
    const boxes = within(consents).getAllByRole("checkbox");
    expect(boxes).toHaveLength(2);
    boxes.forEach((box) => expect(box).not.toBeChecked());
    expect(within(consents).getByRole("checkbox", { name: PRIVACY })).not.toBeChecked();
    for (const { legend } of optionGroups()) {
      within(screen.getByRole("group", { name: legend }))
        .getAllByRole("checkbox")
        .forEach((box) => expect(box).not.toBeChecked());
    }
    expect(screen.getByRole("checkbox", { name: CAPTCHA })).not.toBeChecked();
  });
});
