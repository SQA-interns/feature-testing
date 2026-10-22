import { fireEvent, screen, within } from "@testing-library/react";
import { expect } from "vitest";

/** Option group names in the order of the UI contract. */
export const GROUPS = ["Workshops", "Events", "Meals", "Other activities"] as const;

/** Asserts the page shows exactly these text inputs (by accessible name), and no others. */
export function expectExactlyTextInputs(names: string[]): void {
  expect(screen.getAllByRole("textbox")).toHaveLength(names.length);
  for (const name of names) expect(screen.getByRole("textbox", { name })).toBeInTheDocument();
}

/** Asserts an option group holds exactly these checkboxes; [] means the group is not shown. */
export function expectGroupOptions(group: string, names: string[]): void {
  const element = screen.queryByRole("group", { name: group });
  if (names.length === 0) {
    expect(element).toBeNull();
    return;
  }
  expect(element, `group ${group}`).not.toBeNull();
  const scope = within(element as HTMLElement);
  expect(scope.getAllByRole("checkbox")).toHaveLength(names.length);
  for (const name of names) {
    const box = scope.getByRole("checkbox", { name });
    expect(box).not.toBeChecked();
  }
}

export async function chooseType(label: "External participant" | "Student"): Promise<void> {
  fireEvent.click(await screen.findByRole("radio", { name: label }));
}

export function fill(label: string, value: string): void {
  fireEvent.change(screen.getByRole("textbox", { name: label }), { target: { value } });
}

export function check(name: string): void {
  fireEvent.click(screen.getByRole("checkbox", { name }));
}

export function submit(): void {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

/** Fills a valid external form (no options) and gives the mandatory consent. */
export function fillValidExternal(privacyText: string): void {
  fill("First name", "Ana");
  fill("Last name", "Novak");
  fill("Email", "ana@example.org");
  fill("Organization / institution", "Example d.o.o.");
  check(privacyText);
}
