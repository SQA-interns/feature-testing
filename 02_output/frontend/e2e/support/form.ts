import { expect, type Page } from "@playwright/test";
import { CAPTCHA, GROUPS } from "./stack";

export interface Participant {
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
}

/** Opens the page and chooses the registration type. */
export async function openForm(
  page: Page,
  type: "External participant" | "Student",
): Promise<void> {
  await page.goto("/");
  await page
    .getByRole("radiogroup", { name: "Registration type" })
    .getByRole("radio", { name: type })
    .check();
}

export async function fillFields(page: Page, p: Participant): Promise<void> {
  await page.getByRole("textbox", { name: "First name" }).fill(p.firstName);
  await page.getByRole("textbox", { name: "Last name" }).fill(p.lastName);
  await page.getByRole("textbox", { name: "Email" }).fill(p.email);
  if (p.organization !== undefined) {
    await page.getByRole("textbox", { name: "Organization / institution" }).fill(p.organization);
  }
  if (p.studyInstitution !== undefined) {
    await page.getByRole("textbox", { name: "Study institution" }).fill(p.studyInstitution);
  }
  if (p.studyProgramme !== undefined) {
    await page.getByRole("textbox", { name: "Study programme" }).fill(p.studyProgramme);
  }
  if (p.studentId !== undefined) {
    await page.getByRole("textbox", { name: "Student ID" }).fill(p.studentId);
  }
}

/** Checks the first option of every shown category group; returns the selected option ids. */
export async function selectOnePerCategory(page: Page): Promise<string[]> {
  const selected: string[] = [];
  for (const legend of GROUPS) {
    const group = page.getByRole("group", { name: legend });
    if ((await group.count()) === 0) continue;
    const first = group.getByRole("checkbox").first();
    await first.check();
    selected.push(await first.inputValue());
  }
  return selected;
}

/** Option ids of every option checkbox on the page, grouped by legend. */
export async function shownOptions(page: Page): Promise<Record<string, string[]>> {
  const shown: Record<string, string[]> = {};
  for (const legend of GROUPS) {
    const group = page.getByRole("group", { name: legend });
    if ((await group.count()) === 0) continue;
    const boxes = group.getByRole("checkbox");
    const values: string[] = [];
    for (let i = 0; i < (await boxes.count()); i++) values.push(await boxes.nth(i).inputValue());
    shown[legend] = values.sort();
  }
  return shown;
}

/** Gives every mandatory consent shown on the page, then the test-mode captcha. */
export async function consentAndCaptcha(page: Page): Promise<void> {
  const consents = page.getByRole("group", { name: "Consents" }).getByRole("checkbox");
  for (let i = 0; i < (await consents.count()); i++) {
    await expect(consents.nth(i)).not.toBeChecked();
    await consents.nth(i).check();
  }
  await page.getByRole("checkbox", { name: CAPTCHA }).check();
}

export async function submit(page: Page): Promise<void> {
  await page.getByRole("button", { name: "Register" }).click();
}

export async function expectConfirmation(page: Page, p: Participant): Promise<void> {
  const status = page.getByRole("status");
  await expect(status.getByRole("heading", { name: "Registration received" })).toBeVisible();
  await expect(status).toContainText(`${p.firstName} ${p.lastName}`);
  await expect(page.getByRole("button", { name: "Register" })).toHaveCount(0);
}
