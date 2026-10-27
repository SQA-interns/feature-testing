import { expect, test } from "@playwright/test";
import { jsonCopy, literal, mailsTo, sql, unique } from "./support/stack";

const OPTIONS = [
  "Workshop: Testing in practice",
  "Conference dinner",
  "Lunch (vegetarian)",
  "City tour",
];

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({
  page,
  request,
}) => {
  const u = unique();
  const lastName = `Kranjc${u}`;
  const email = `luka.${u}@student.example.org`;

  await page.goto("/");
  await page.getByRole("radio", { name: "Student" }).check();

  await expect(page.getByRole("textbox")).toHaveCount(6);
  for (const label of [
    "First name",
    "Last name",
    "Email",
    "Study institution",
    "Study programme",
    "Student ID",
  ]) {
    await expect(
      page.getByRole("textbox", { name: label, exact: true }),
    ).toBeVisible();
  }
  await expect(page.getByLabel("Organization / institution")).toHaveCount(0);
  await expect(
    page.getByRole("checkbox", { name: "Workshop: Industry partnerships" }),
  ).toHaveCount(0);

  await page.getByLabel("First name").fill("Luka");
  await page.getByLabel("Last name").fill(lastName);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Study institution").fill("University of Ljubljana");
  await page.getByLabel("Study programme").fill("Computer Science");
  await page.getByLabel("Student ID").fill("63200001");
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
  await expect(page.getByText(`Thank you, Luka ${lastName}.`)).toBeVisible();

  const rows = sql(
    `SELECT id, type, study_institution, study_programme, student_id, organization FROM registration WHERE email = ${literal(email)}`,
  );
  expect(rows).toHaveLength(1);
  const [id, type, institution, programme, studentId, organization] = rows[0];
  expect([type, institution, programme, studentId, organization]).toEqual([
    "STUDENT",
    "University of Ljubljana",
    "Computer Science",
    "63200001",
    "",
  ]);
  expect(JSON.parse(jsonCopy(id)).id).toBe(id);

  expect(await mailsTo(request, email, lastName)).toHaveLength(1);
  const organizers = (process.env.ORGANIZER_EMAILS ?? "")
    .split(",")
    .map((a) => a.trim())
    .filter(Boolean);
  expect(organizers.length).toBeGreaterThan(0);
  for (const organizer of organizers) {
    expect(await mailsTo(request, organizer, lastName)).toHaveLength(1);
  }
});
