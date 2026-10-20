// E2E-02 Student registers: AC-002-01, AC-002-07, AC-004-01, DoD-P01 to DoD-P03.
import { expect, test } from "@playwright/test";
import {
  GROUPS,
  LABELS,
  field,
  group,
  jsonCopy,
  mailsTo,
  notificationsFor,
  sql,
  submitAndCapture,
  tickMandatoryConsentAndCaptcha,
  tickOnePerCategory,
  uniqueEmail,
} from "./support";

interface FormOption {
  id: string;
  name: string;
  availableTo: string[];
}

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({ page, request }) => {
  const config = (await (await request.get("/api/form-config")).json()) as {
    options: FormOption[];
  };
  const studentOptions = config.options
    .filter((o) => o.availableTo.includes("STUDENT"))
    .map((o) => o.name);
  const email = uniqueEmail("student");

  await page.goto("/");
  await page.getByRole("radio", { name: "Student" }).check();

  for (const label of [
    LABELS.firstName,
    LABELS.lastName,
    LABELS.email,
    LABELS.studyInstitution,
    LABELS.studyProgramme,
    LABELS.studentId,
  ]) {
    await expect(field(page, label)).toBeVisible();
  }
  await expect(field(page, LABELS.organization)).toHaveCount(0);
  const shown: string[] = [];
  for (const name of GROUPS) {
    const labels = await group(page, name).locator("label").allTextContents();
    shown.push(...labels.map((t) => t.trim()));
  }
  expect([...shown].sort()).toEqual([...studentOptions].sort());

  await field(page, LABELS.firstName).fill("Luka");
  await field(page, LABELS.lastName).fill("Horvat");
  await field(page, LABELS.email).fill(email);
  await field(page, LABELS.studyInstitution).fill("University of Ljubljana");
  await field(page, LABELS.studyProgramme).fill("Computer Science");
  await field(page, LABELS.studentId).fill("63210001");
  const options = await tickOnePerCategory(page);
  await tickMandatoryConsentAndCaptcha(page);

  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(201);
  const id = (body as { id: string }).id;

  await expect(field(page, LABELS.firstName)).toHaveCount(0);
  await expect(page.getByText(/Luka/).first()).toBeVisible();
  for (const option of options) {
    await expect(page.getByText(option).first()).toBeVisible();
  }

  expect(
    sql(
      "SELECT type, first_name, last_name, email, organization IS NULL, study_institution," +
        ` study_programme, student_id FROM registration WHERE id = '${id}'`,
    ),
  ).toEqual([
    [
      "STUDENT",
      "Luka",
      "Horvat",
      email,
      "t",
      "University of Ljubljana",
      "Computer Science",
      "63210001",
    ],
  ]);
  const copy = jsonCopy(id);
  expect(JSON.parse(copy.toString("utf8"))).toMatchObject({
    id,
    type: "STUDENT",
    studentId: "63210001",
  });

  await expect
    .poll(async () => (await mailsTo(request, email)).length, { timeout: 30_000 })
    .toBe(1);
  await expect
    .poll(async () => (await notificationsFor(request, id)).length, { timeout: 30_000 })
    .toBeGreaterThan(0);
  for (const { attachment } of await notificationsFor(request, id)) {
    expect(attachment.equals(copy)).toBe(true);
  }
});
