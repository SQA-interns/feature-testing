import { expect, test, type Page } from "@playwright/test";
import {
  BACKEND_URL,
  fillRegistration,
  organizerCredentials,
  submitAndExpectConfirmation,
  token,
  type Participant,
} from "./support/stack";

async function signIn(page: Page, username: string, password: string): Promise<void> {
  await page.getByRole("textbox", { name: "Username", exact: true }).fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

// Covers AC-009-01, AC-009-02, AC-009-03, AC-009-12, DoD-P06.
test("E2E-06 organizer searches registrations (AC-009-01, AC-009-02, AC-009-03, AC-009-12)", async ({
  page,
  request,
}) => {
  const t = token();
  const external: Participant = {
    type: "External participant",
    firstName: "Eva",
    lastName: `Externa-${t}`,
    email: `eva.${t}@example.org`,
    organization: "Search Org",
  };
  const student: Participant = {
    type: "Student",
    firstName: "Sara",
    lastName: `Studentka-${t}`,
    email: `sara.${t}@student.example`,
    studyInstitution: "University of Maribor",
    studyProgramme: "Informatics",
    studentId: "93100002",
  };
  for (const p of [external, student]) {
    await fillRegistration(page, p);
    await submitAndExpectConfirmation(page, p);
  }

  await page.goto("/organizer");
  const { username, password } = organizerCredentials();
  await signIn(page, username, password + "-wrong");
  await expect(page.getByText("Invalid username or password.")).toBeVisible();
  await expect(page.getByRole("table", { name: "Registrations" })).toHaveCount(0);
  await expect(page.getByText(student.lastName)).toHaveCount(0);

  await signIn(page, username, password);
  const table = page.getByRole("table", { name: "Registrations" });
  await expect(table).toBeVisible();
  await expect(table.getByText(external.lastName, { exact: true })).toBeVisible();
  await expect(table.getByText(student.lastName, { exact: true })).toBeVisible();

  await page.getByRole("searchbox", { name: "Search" }).fill(student.lastName);
  await page.getByRole("button", { name: "Search" }).click();
  await expect(table.getByText(external.lastName, { exact: true })).toHaveCount(0);
  await expect(table.getByText(student.lastName, { exact: true })).toBeVisible();
  await expect(table.locator("tbody tr")).toHaveCount(1);

  await page.getByRole("searchbox", { name: "Search" }).fill("");
  await page.getByRole("button", { name: "Search" }).click();
  await page.getByRole("combobox", { name: "Type" }).selectOption({ label: "External" });
  await expect(table.getByText(external.lastName, { exact: true })).toBeVisible();
  await expect(table.getByText(student.lastName, { exact: true })).toHaveCount(0);
  const rows = table.locator("tbody tr");
  for (let i = 0; i < (await rows.count()); i++)
    await expect(rows.nth(i)).toContainText("External");

  const anonymous = await request.get(
    `${BACKEND_URL}/api/registrations?text=${encodeURIComponent(student.lastName)}`,
  );
  expect(anonymous.status()).toBe(401);
  expect(await anonymous.text()).not.toContain(student.lastName);
});
