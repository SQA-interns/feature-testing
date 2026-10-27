import { expect, test } from "@playwright/test";
import {
  jsonCopy,
  literal,
  mailAttachment,
  mailsTo,
  sql,
  unique,
} from "./support/stack";

const OPTIONS = [
  "Workshop: Testing in practice",
  "Conference dinner",
  "Lunch (vegetarian)",
  "City tour",
];

test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02)", async ({
  page,
  request,
}) => {
  const u = unique();
  const lastName = `Novak${u}`;
  const email = `ana.${u}@example.org`;

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByLabel("First name").fill("Ana");
  await page.getByLabel("Last name").fill(lastName);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Organization / institution").fill("Example Institute");
  for (const option of OPTIONS) {
    await page.getByRole("checkbox", { name: option }).check();
  }
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
    page.getByRole("heading", { name: "Registration received" }),
  ).toBeVisible();
  await expect(page.getByText(`Thank you, Ana ${lastName}.`)).toBeVisible();
  for (const option of OPTIONS) {
    await expect(page.getByText(option)).toBeVisible();
  }
  await expect(page.getByRole("button", { name: "Register" })).toHaveCount(0);

  const rows = sql(
    `SELECT id, type, first_name, last_name, organization FROM registration WHERE email = ${literal(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, type, firstName, storedLast, organization] = rows[0];
  expect([type, firstName, storedLast, organization]).toEqual([
    "EXTERNAL",
    "Ana",
    lastName,
    "Example Institute",
  ]);
  expect(
    sql(
      `SELECT option_name FROM registration_option WHERE registration_id = ${literal(id)} ORDER BY position`,
    ).map((r) => r[0]),
  ).toEqual(OPTIONS);
  expect(
    sql(
      `SELECT consent_id FROM registration_consent WHERE registration_id = ${literal(id)}`,
    ).map((r) => r[0]),
  ).toEqual(["privacy"]);

  const copyText = jsonCopy(id);
  const copy = JSON.parse(copyText);
  expect(copy.id).toBe(id);
  expect(copy.participant.email).toBe(email);
  expect(copy.options.map((o: { name: string }) => o.name)).toEqual(OPTIONS);

  const participantMail = await mailsTo(request, email, lastName);
  expect(participantMail).toHaveLength(1);
  for (const option of OPTIONS) {
    expect(participantMail[0].Text).toContain(option);
  }

  const organizers = (process.env.ORGANIZER_EMAILS ?? "")
    .split(",")
    .map((a) => a.trim())
    .filter(Boolean);
  expect(organizers.length).toBeGreaterThan(0);
  for (const organizer of organizers) {
    const notes = await mailsTo(request, organizer, lastName);
    expect(notes).toHaveLength(1);
    const attachment = notes[0].Attachments.find((a) =>
      a.FileName.endsWith(".json"),
    );
    expect(attachment).toBeDefined();
    expect(await mailAttachment(request, notes[0].ID, attachment!.PartID)).toBe(
      copyText,
    );
  }
});
