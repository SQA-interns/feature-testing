import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "../App";
import { stubApi } from "./support/api";
import {
  ALL_FIELDS,
  CATEGORY_LEGENDS,
  EXTERNAL_FIELDS,
  PRIVACY_CONSENT,
  chooseType,
} from "./support/form";

const EXTERNAL_OPTIONS: Record<string, string[]> = {
  Workshops: [
    "Workshop: Testing in practice",
    "Workshop: Industry partnerships",
  ],
  Events: ["Conference dinner"],
  Meals: ["Lunch (vegetarian)"],
  "Other activities": ["City tour"],
};

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields, active options by category and unchecked consents", async () => {
    stubApi();
    render(<App />);

    await chooseType("External participant");

    expect(screen.getAllByRole("textbox")).toHaveLength(EXTERNAL_FIELDS.length);
    for (const field of EXTERNAL_FIELDS) {
      expect(screen.getByRole("textbox", { name: field })).toBeInTheDocument();
    }
    for (const field of ALL_FIELDS.filter(
      (f) => !EXTERNAL_FIELDS.includes(f),
    )) {
      expect(screen.queryByLabelText(field)).toBeNull();
    }

    for (const legend of CATEGORY_LEGENDS) {
      const group = screen.getByRole("group", { name: legend });
      expect(within(group).getAllByRole("checkbox")).toHaveLength(
        EXTERNAL_OPTIONS[legend].length,
      );
      for (const option of EXTERNAL_OPTIONS[legend]) {
        expect(
          within(group).getByRole("checkbox", { name: option }),
        ).toBeInTheDocument();
      }
    }

    expect(
      screen.getByRole("checkbox", { name: PRIVACY_CONSENT }),
    ).not.toBeChecked();
  });
});
