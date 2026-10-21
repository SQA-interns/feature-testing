import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { OptionGroups } from "./OptionGroups";

describe("OptionGroups", () => {
  it("renders no fieldset for a category without options", () => {
    render(
      <OptionGroups
        options={[{ id: "m", name: "Lunch", category: "MEAL", availableTo: ["STUDENT"] }]}
        selected={["m"]}
        onToggle={() => {}}
      />,
    );

    expect(screen.getAllByRole("group")).toHaveLength(1);
    expect(screen.getByRole("group", { name: "Meals" })).not.toHaveAttribute("aria-describedby");
    expect(screen.getByRole("checkbox", { name: "Lunch" })).toBeChecked();
    expect(document.getElementById("options-error")).toBeNull();
  });
});
