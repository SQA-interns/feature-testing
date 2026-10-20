// E2E-04 Slovenian characters survive the whole chain: AC-005-04, AC-008-03, NFR-01.
import { expect, test } from "@playwright/test";
import {
  LABELS,
  XLSX,
  field,
  jsonCopy,
  mailsTo,
  notificationsFor,
  organizerAuth,
  sheetRows,
  sql,
  submitAndCapture,
  tickMandatoryConsentAndCaptcha,
  tickOnePerCategory,
  uniqueEmail,
} from "./support";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
  request,
}) => {
  const firstName = "Žiga Črtomir";
  const lastName = "Šuštaršič-Čeh";
  const organization = "Fakulteta za računalništvo, ČŠŽ čšž";
  const email = uniqueEmail("slovenian");

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await field(page, LABELS.firstName).fill(firstName);
  await field(page, LABELS.lastName).fill(lastName);
  await field(page, LABELS.email).fill(email);
  await field(page, LABELS.organization).fill(organization);
  await tickOnePerCategory(page);
  await tickMandatoryConsentAndCaptcha(page);

  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(201);
  const id = (body as { id: string }).id;

  await expect(page.getByText(firstName).first()).toBeVisible();
  await expect(page.getByText(lastName).first()).toBeVisible();

  expect(
    sql(`SELECT first_name, last_name, organization FROM registration WHERE id = '${id}'`),
  ).toEqual([[firstName, lastName, organization]]);
  const copy = JSON.parse(jsonCopy(id).toString("utf8")) as Record<string, string>;
  expect([copy.firstName, copy.lastName, copy.organization]).toEqual([
    firstName,
    lastName,
    organization,
  ]);

  await expect
    .poll(async () => (await mailsTo(request, email)).length, { timeout: 30_000 })
    .toBe(1);
  const [confirmation] = await mailsTo(request, email);
  expect(confirmation.Text).toContain(firstName);
  expect(confirmation.Text).toContain(lastName);
  await expect
    .poll(async () => (await notificationsFor(request, id)).length, { timeout: 30_000 })
    .toBeGreaterThan(0);
  for (const { mail } of await notificationsFor(request, id)) {
    expect(mail.Text).toContain(firstName);
    expect(mail.Text).toContain(lastName);
    expect(mail.Text).toContain(organization);
  }

  const exported = await request.get("/api/export", {
    headers: { Authorization: organizerAuth() },
  });
  expect(exported.status()).toBe(200);
  expect(exported.headers()["content-type"]).toContain(XLSX);
  const rows = sheetRows(Buffer.from(await exported.body()));
  const row = rows.find((r) => r[0] === id);
  expect(row).toBeDefined();
  expect(row?.slice(2, 4)).toEqual([firstName, lastName]);
  expect(row?.[5]).toBe(organization);
});
