import { expect, test, type Page } from "@playwright/test";
import { organizerCredentials, registerViaPage, uniqueToken } from "./support";

async function signIn(page: Page, username: string, password: string) {
  await page.getByLabel("Username", { exact: true }).fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

test("E2E-06 organizer searches registrations (AC-009-01, AC-009-02, AC-009-03, AC-009-12)", async ({
  page,
}) => {
  const token = uniqueToken("e2e06");
  const externalLast = `Extern${token}`;
  const studentLast = `Student${token}`;
  await registerViaPage(page, "External participant", {
    firstName: "Eva",
    lastName: externalLast,
    email: `ext${token}@example.org`,
    organization: "Example d.o.o.",
  });
  await registerViaPage(page, "Student", {
    firstName: "Jan",
    lastName: studentLast,
    email: `stu${token}@student.example.org`,
    studyInstitution: "Univerza v Ljubljani",
    studyProgramme: "Computer Science",
    studentId: "63219999",
  });
  const { username, password } = organizerCredentials();

  await page.goto("/organizer");

  // wrong credentials: an error and no data
  await signIn(page, username, `${password}-wrong`);
  await expect(page.getByRole("alert")).toContainText("Wrong username or password.");
  await expect(page.getByRole("table")).toHaveCount(0);
  await expect(page.getByText(externalLast)).toHaveCount(0);

  // valid credentials: both registrations are listed (newest first, so on page 1)
  await signIn(page, username, password);
  const table = page.getByRole("table");
  await expect(table.getByText(externalLast)).toBeVisible();
  await expect(table.getByText(studentLast)).toBeVisible();

  // the student's last name: only the student
  await page.getByRole("searchbox", { name: "Search" }).fill(studentLast);
  await page.getByRole("button", { name: "Search" }).click();
  await expect(table.getByText(studentLast)).toBeVisible();
  await expect(table.getByText(externalLast)).toHaveCount(0);
  await expect(table.getByRole("row")).toHaveCount(2); // header and one registration

  // no text, type "external": the external participant, never the student
  await page.getByRole("searchbox", { name: "Search" }).fill("");
  await page
    .getByRole("combobox", { name: "Type" })
    .selectOption({ label: "External participant" });
  await page.getByRole("button", { name: "Search" }).click();
  await expect(table.getByText(externalLast)).toBeVisible();
  await expect(table.getByText(studentLast)).toHaveCount(0);
  const typeCells = table.getByRole("row").locator("td:nth-child(2)");
  for (const text of await typeCells.allTextContents()) {
    expect(text).toMatch(/external/i);
  }
});
