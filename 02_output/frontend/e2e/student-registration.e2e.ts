import { expect, test } from "@playwright/test";
import {
  consentAndCaptcha,
  expectConfirmation,
  fillFields,
  openForm,
  selectOnePerCategory,
  shownOptions,
  submit,
  type Participant,
} from "./support/form";
import {
  awaitMailsTo,
  jsonAttachments,
  jsonCopy,
  organizerEmails,
  registrationsByEmail,
} from "./support/stack";

const CATEGORY_LEGEND: Record<string, string> = {
  WORKSHOP: "Workshops",
  EVENT: "Events",
  MEAL: "Meals",
  OTHER: "Other activities",
};

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({ page, request }) => {
  const p: Participant = {
    firstName: "Nika",
    lastName: "Zupan",
    email: `e2e-student.${Date.now()}@example.org`,
    studyInstitution: "Univerza v Ljubljani",
    studyProgramme: "Informatika",
    studentId: "63220002",
  };
  const formConfig = await request.get("/api/form-config");
  expect(formConfig.status()).toBe(200);
  const config = (await formConfig.json()) as {
    options: { id: string; category: string; availableTo: string[] }[];
  };
  const expected: Record<string, string[]> = {};
  for (const o of config.options.filter((o) => o.availableTo.includes("STUDENT"))) {
    (expected[CATEGORY_LEGEND[o.category]] ??= []).push(o.id);
  }
  Object.values(expected).forEach((ids) => ids.sort());
  expect(config.options.some((o) => !o.availableTo.includes("STUDENT"))).toBe(true);

  await openForm(page, "Student");
  await expect(page.getByRole("textbox", { name: "Student ID" })).toBeVisible();
  await expect(page.getByRole("textbox", { name: "Study institution" })).toBeVisible();
  await expect(page.getByRole("textbox", { name: "Study programme" })).toBeVisible();
  await expect(page.getByRole("textbox", { name: "Organization / institution" })).toHaveCount(0);
  expect(await shownOptions(page)).toEqual(expected);
  await fillFields(page, p);
  await selectOnePerCategory(page);
  await consentAndCaptcha(page);
  await submit(page);

  await expectConfirmation(page, p);

  const rows = registrationsByEmail(p.email);
  expect(rows).toHaveLength(1);
  expect(rows[0]).toMatchObject({
    type: "STUDENT",
    firstName: p.firstName,
    lastName: p.lastName,
    studyInstitution: p.studyInstitution,
    studyProgramme: p.studyProgramme,
    studentId: p.studentId,
    organization: "",
  });
  const copy = jsonCopy(rows[0].id);
  expect(copy).toMatchObject({ id: rows[0].id, type: "STUDENT", studentId: p.studentId });

  expect(await awaitMailsTo(p.email)).toHaveLength(1);
  for (const organizer of organizerEmails()) {
    const mails = await awaitMailsTo(organizer, p.email);
    expect(mails).toHaveLength(1);
    expect(await jsonAttachments(mails[0])).toEqual([copy]);
  }
});
