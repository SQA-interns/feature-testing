import { expect, test } from "@playwright/test";
import {
  accessibleLabel,
  awaitMail,
  BACKEND_URL,
  fillRegistration,
  jsonCopyBytes,
  organizerEmails,
  registrationIdByEmail,
  sql,
  submitAndExpectConfirmation,
  token,
  type Participant,
} from "./support/stack";

interface FormOption {
  id: string;
  name: string;
  availableTo: string[];
}

// Covers AC-002-01, AC-002-07, AC-004-01, DoD-P01 to DoD-P03.
test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({ page, request }) => {
  const t = token();
  const participant: Participant = {
    type: "Student",
    firstName: "Luka",
    lastName: `Kranjc-${t}`,
    email: `luka.${t}@student.example`,
    studyInstitution: "University of Ljubljana",
    studyProgramme: "Computer Science",
    studentId: "63200001",
  };
  const configResponse = await request.get(`${BACKEND_URL}/api/form-config`);
  expect(configResponse.status()).toBe(200);
  const config = (await configResponse.json()) as { options: FormOption[] };
  const studentOptions = config.options
    .filter((o) => o.availableTo.includes("STUDENT"))
    .map((o) => o.name);

  const selected = await fillRegistration(page, participant);

  const textboxes = page.getByRole("textbox");
  await expect(textboxes).toHaveCount(6);
  for (const name of [
    "First name",
    "Last name",
    "Email",
    "Study institution",
    "Study programme",
    "Student ID",
  ]) {
    await expect(page.getByRole("textbox", { name, exact: true })).toBeVisible();
  }
  const shown: string[] = [];
  for (const group of ["Workshops", "Events", "Meals", "Other activities"]) {
    const boxes = page.getByRole("group", { name: group, exact: true }).getByRole("checkbox");
    for (let i = 0; i < (await boxes.count()); i++) shown.push(await accessibleLabel(boxes.nth(i)));
  }
  expect(shown.sort()).toEqual([...studentOptions].sort());

  await submitAndExpectConfirmation(page, participant);
  for (const name of selected) await expect(page.getByText(name, { exact: true })).toBeVisible();

  const id = registrationIdByEmail(participant.email);
  expect(id, "registration row").toBeTruthy();
  const [row] = sql(
    `select type, study_institution, study_programme, student_id from registration where id = '${id}'`,
  );
  expect(row).toEqual(["STUDENT", "University of Ljubljana", "Computer Science", "63200001"]);
  expect(JSON.parse(jsonCopyBytes(id!).toString("utf8")).id).toBe(id);

  await awaitMail(request, participant.email, (m) => m.Text.includes(participant.lastName));
  for (const organizer of organizerEmails()) {
    const notification = await awaitMail(request, organizer, (m) =>
      m.Text.includes(participant.email),
    );
    expect(notification.Attachments.some((a) => a.FileName.endsWith(".json"))).toBe(true);
  }
});
