import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "../App";
import { stubApi } from "./support/api";
import {
  ALL_FIELDS,
  CATEGORY_LEGENDS,
  PRIVACY_CONSENT,
  STUDENT_FIELDS,
  chooseType,
} from "./support/form";

const STUDENT_OPTIONS: Record<string, string[]> = {
  Workshops: ["Workshop: Testing in practice"],
  Events: ["Conference dinner"],
  Meals: ["Lunch (vegetarian)"],
  "Other activities": ["City tour"],
};

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields, only student options by category and unchecked consents", async () => {
    stubApi();
    render(<App />);

    await chooseType("Student");

    expect(screen.getAllByRole("textbox")).toHaveLength(STUDENT_FIELDS.length);
    for (const field of STUDENT_FIELDS) {
      expect(screen.getByRole("textbox", { name: field })).toBeInTheDocument();
    }
    for (const field of ALL_FIELDS.filter((f) => !STUDENT_FIELDS.includes(f))) {
      expect(screen.queryByLabelText(field)).toBeNull();
    }

    for (const legend of CATEGORY_LEGENDS) {
      const group = screen.getByRole("group", { name: legend });
      expect(within(group).getAllByRole("checkbox")).toHaveLength(
        STUDENT_OPTIONS[legend].length,
      );
      for (const option of STUDENT_OPTIONS[legend]) {
        expect(
          within(group).getByRole("checkbox", { name: option }),
        ).toBeInTheDocument();
      }
    }
    expect(
      screen.queryByRole("checkbox", {
        name: "Workshop: Industry partnerships",
      }),
    ).toBeNull();

    expect(
      screen.getByRole("checkbox", { name: PRIVACY_CONSENT }),
    ).not.toBeChecked();
  });
});
