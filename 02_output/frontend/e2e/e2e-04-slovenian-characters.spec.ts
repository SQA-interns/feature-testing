import { expect, test } from "@playwright/test";
import {
  basicAuth,
  jsonCopy,
  literal,
  mailsTo,
  sql,
  unique,
  workbookRows,
} from "./support/stack";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
  request,
}) => {
  const u = unique();
  const firstName = "Čedomir Žiga";
  const lastName = `Šuštaršič${u}`;
  const organization = "Inštitut za ŽČŠ raziskave, Črnomelj";
  const email = `cedomir.${u}@example.org`;

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByLabel("First name").fill(firstName);
  await page.getByLabel("Last name").fill(lastName);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Organization / institution").fill(organization);
  await page
    .getByRole("checkbox", {
      name: /I agree that my personal data is processed/,
    })
    .check();
  await page
    .getByRole("checkbox", { name: "I am not a robot (test mode)" })
    .check();
  await page.getByRole("button", { name: "Register" }).click();

  await expect(
    page.getByText(`Thank you, ${firstName} ${lastName}.`),
  ).toBeVisible();

  const rows = sql(
    `SELECT id, first_name, last_name, organization FROM registration WHERE email = ${literal(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, storedFirst, storedLast, storedOrganization] = rows[0];
  expect([storedFirst, storedLast, storedOrganization]).toEqual([
    firstName,
    lastName,
    organization,
  ]);

  const participant = JSON.parse(jsonCopy(id)).participant;
  expect([
    participant.firstName,
    participant.lastName,
    participant.organization,
  ]).toEqual([firstName, lastName, organization]);

  const participantMail = await mailsTo(request, email, lastName);
  expect(participantMail[0].Text).toContain(`${firstName} ${lastName}`);
  const organizer = (process.env.ORGANIZER_EMAILS ?? "").split(",")[0].trim();
  const organizerMail = await mailsTo(request, organizer, lastName);
  expect(organizerMail[0].Text).toContain(firstName);
  expect(organizerMail[0].Text).toContain(lastName);
  expect(organizerMail[0].Text).toContain(organization);

  const exported = await request.get("/api/registrations/export", {
    headers: { Authorization: basicAuth() },
  });
  expect(exported.status()).toBe(200);
  const row = workbookRows(await exported.body()).find((cells) =>
    cells.includes(id),
  );
  expect(row).toBeDefined();
  expect(row).toEqual(
    expect.arrayContaining([firstName, lastName, organization]),
  );
});
