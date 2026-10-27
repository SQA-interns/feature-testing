import { fireEvent, screen } from "@testing-library/react";

export const CATEGORY_LEGENDS = [
  "Workshops",
  "Events",
  "Meals",
  "Other activities",
];
export const EXTERNAL_FIELDS = [
  "First name",
  "Last name",
  "Email",
  "Organization / institution",
];
export const STUDENT_FIELDS = [
  "First name",
  "Last name",
  "Email",
  "Study institution",
  "Study programme",
  "Student ID",
];
export const ALL_FIELDS = [...new Set([...EXTERNAL_FIELDS, ...STUDENT_FIELDS])];
export const GENERAL_ERROR =
  "Your registration could not be sent. Please try again later.";
export const PRIVACY_CONSENT = /I agree that my personal data is processed/;
export const TEST_CAPTCHA = "I am not a robot (test mode)";

export async function chooseType(label: "External participant" | "Student") {
  fireEvent.click(await screen.findByRole("radio", { name: label }));
}

export function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

export function check(name: string | RegExp) {
  fireEvent.click(screen.getByRole("checkbox", { name }));
}

/** Fills a valid external form: fields, one workshop, the mandatory consent, the test-mode captcha. */
export async function fillValidExternal() {
  await chooseType("External participant");
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.org");
  type("Organization / institution", "Example Institute");
  check("Workshop: Testing in practice");
  check(PRIVACY_CONSENT);
  check(TEST_CAPTCHA);
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}
