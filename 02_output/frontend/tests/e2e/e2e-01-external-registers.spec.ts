import { expect, test } from "@playwright/test";
import {
  attachmentBytes,
  awaitMail,
  fillRegistration,
  jsonCopyBytes,
  organizerEmails,
  registrationIdByEmail,
  sql,
  submitAndExpectConfirmation,
  token,
  type Participant,
} from "./support/stack";

// Covers AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02,
// DoD-P01 to DoD-P03.
test("E2E-01 external participant registers (AC-001-01, AC-004-01, AC-005-01, AC-007-02)", async ({
  page,
  request,
}) => {
  const t = token();
  const participant: Participant = {
    type: "External participant",
    firstName: "Ana",
    lastName: `Novak-${t}`,
    email: `ana.${t}@example.org`,
    organization: "Example d.o.o.",
  };

  const selected = await fillRegistration(page, participant);
  expect(selected, "one option per category").toHaveLength(4);
  await submitAndExpectConfirmation(page, participant);
  for (const name of selected) await expect(page.getByText(name, { exact: true })).toBeVisible();

  const id = registrationIdByEmail(participant.email);
  expect(id, "registration row").toBeTruthy();
  const [row] = sql(
    `select type, first_name, last_name, organization, registered_at from registration where id = '${id}'`,
  );
  expect(row.slice(0, 4)).toEqual(["EXTERNAL", "Ana", participant.lastName, "Example d.o.o."]);
  expect(row[4]).not.toBe("");
  expect(
    sql(`select option_id from registration_option where registration_id = '${id}'`),
  ).toHaveLength(4);
  expect(
    sql(`select consent_id from registration_consent where registration_id = '${id}'`).flat(),
  ).toContain("privacy");

  const copy = jsonCopyBytes(id!);
  const json = JSON.parse(copy.toString("utf8")) as Record<string, unknown>;
  expect(json.id).toBe(id);
  expect(json.email).toBe(participant.email);
  expect(json.lastName).toBe(participant.lastName);

  const confirmation = await awaitMail(request, participant.email, (m) =>
    m.Text.includes(participant.lastName),
  );
  for (const name of selected) expect(confirmation.Text).toContain(name);

  const organizers = organizerEmails();
  expect(organizers.length).toBeGreaterThan(0);
  for (const organizer of organizers) {
    const notification = await awaitMail(request, organizer, (m) =>
      m.Text.includes(participant.email),
    );
    const attachment = notification.Attachments.find((a) => a.FileName.endsWith(".json"));
    expect(attachment, "JSON attachment").toBeDefined();
    const bytes = await attachmentBytes(request, notification.ID, attachment!.PartID);
    expect(bytes.equals(copy), "attachment equals the JSON copy").toBe(true);
  }
});
