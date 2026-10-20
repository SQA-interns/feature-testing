// E2E-01 External participant registers: AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02,
// AC-006-01, AC-007-01, AC-007-02, DoD-P01 to DoD-P03.
import { expect, test } from "@playwright/test";
import {
  LABELS,
  field,
  jsonCopy,
  mailsTo,
  notificationsFor,
  sql,
  submitAndCapture,
  tickMandatoryConsentAndCaptcha,
  tickOnePerCategory,
  uniqueEmail,
} from "./support";

test("E2E-01 external participant registers (AC-001-01, AC-004-01, AC-005-01, AC-006-01, AC-007-02)", async ({
  page,
  request,
}) => {
  const email = uniqueEmail("external");
  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();

  await expect(field(page, LABELS.organization)).toBeVisible();
  await expect(field(page, LABELS.studentId)).toHaveCount(0);
  await field(page, LABELS.firstName).fill("Ana");
  await field(page, LABELS.lastName).fill("Novak");
  await field(page, LABELS.email).fill(email);
  await field(page, LABELS.organization).fill("Example d.o.o.");
  const options = await tickOnePerCategory(page);
  await tickMandatoryConsentAndCaptcha(page);

  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(201);
  const id = (body as { id: string }).id;

  await expect(field(page, LABELS.firstName)).toHaveCount(0);
  await expect(page.getByText(/Ana/).first()).toBeVisible();
  for (const option of options) {
    await expect(page.getByText(option).first()).toBeVisible();
  }

  const rows = sql(
    `SELECT type, first_name, last_name, email, organization FROM registration WHERE id = '${id}'`,
  );
  expect(rows).toEqual([["EXTERNAL", "Ana", "Novak", email, "Example d.o.o."]]);
  expect(sql(`SELECT count(*) FROM registration_option WHERE registration_id = '${id}'`)).toEqual([
    ["4"],
  ]);
  expect(
    sql(`SELECT consent_id FROM registration_consent WHERE registration_id = '${id}'`),
  ).toEqual([["privacy"]]);

  const copy = jsonCopy(id);
  expect(JSON.parse(copy.toString("utf8"))).toMatchObject({ id, type: "EXTERNAL", email });

  await expect
    .poll(async () => (await mailsTo(request, email)).length, { timeout: 30_000 })
    .toBe(1);
  const [confirmation] = await mailsTo(request, email);
  expect(confirmation.Text).toContain("Ana");
  expect(confirmation.Text).toContain("Novak");

  await expect
    .poll(async () => (await notificationsFor(request, id)).length, { timeout: 30_000 })
    .toBeGreaterThan(0);
  for (const { mail, attachment } of await notificationsFor(request, id)) {
    expect(mail.Text).toContain(email);
    expect(attachment.equals(copy)).toBe(true);
  }
});
