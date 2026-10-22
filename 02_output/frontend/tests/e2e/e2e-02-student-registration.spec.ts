import { expect, test } from "@playwright/test";
import {
  BACKEND_URL,
  checkFirstOptionPerGroup,
  checkMandatoryConsents,
  chooseType,
  fillPerson,
  isAddressedTo,
  mailsMatching,
  registrationRow,
  shownOptionNames,
  submitAndGetId,
  uniqueToken,
} from "./support";

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({ page, request }) => {
  const token = uniqueToken("e2e02");
  const person = {
    firstName: "Luka",
    lastName: `Horvat${token}`,
    email: `${token}@student.example.org`,
    studyInstitution: "Univerza v Ljubljani",
    studyProgramme: "Computer Science",
    studentId: "63210001",
  };
  const config = (await (await request.get(`${BACKEND_URL}/api/form-config`)).json()) as {
    options: { name: string; availableTo: string[] }[];
  };
  const studentOptions = config.options
    .filter((o) => o.availableTo.includes("STUDENT"))
    .map((o) => o.name);

  await page.goto("/");
  await chooseType(page, "Student");

  // only the student fields and the options available to students
  const textBoxes = page.getByRole("textbox");
  await expect(textBoxes).toHaveCount(6);
  for (const label of ["Study institution", "Study programme", "Student ID"]) {
    await expect(page.getByLabel(label, { exact: true })).toBeVisible();
  }
  await expect(page.getByLabel("Organization / institution", { exact: true })).toHaveCount(0);
  expect((await shownOptionNames(page)).sort()).toEqual([...studentOptions].sort());

  await fillPerson(page, person);
  const options = await checkFirstOptionPerGroup(page);
  await checkMandatoryConsents(page);
  const id = await submitAndGetId(page);

  await expect(page.getByText(`${person.firstName} ${person.lastName}`)).toBeVisible();
  for (const option of options) {
    await expect(page.getByText(option, { exact: true })).toBeVisible();
  }
  expect(registrationRow(id)).toBe(
    `STUDENT|${person.firstName}|${person.lastName}|${person.email}||${person.studyInstitution}`,
  );

  const messages = await mailsMatching(request, token, 2);
  expect(messages.filter((m) => isAddressedTo(m, person.email))).toHaveLength(1);
  expect(messages.filter((m) => !isAddressedTo(m, person.email)).length).toBeGreaterThanOrEqual(1);
});
