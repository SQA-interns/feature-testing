import { expect, test } from "@playwright/test";
import {
  checkFirstOptionPerGroup,
  checkMandatoryConsents,
  chooseType,
  fillPerson,
  isAddressedTo,
  jsonCopy,
  mailMessage,
  mailPart,
  mailsMatching,
  registrationRow,
  sql,
  submitAndGetId,
  uniqueToken,
} from "./support";

test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02)", async ({
  page,
  request,
}) => {
  const token = uniqueToken("e2e01");
  const person = {
    firstName: "Ana",
    lastName: `Novak${token}`,
    email: `${token}@example.org`,
    organization: "Example d.o.o.",
  };

  await page.goto("/");
  await chooseType(page, "External participant");
  await fillPerson(page, person);
  const options = await checkFirstOptionPerGroup(page);
  expect(options, "one option per category").toHaveLength(4);
  await checkMandatoryConsents(page);
  const id = await submitAndGetId(page);

  // confirmation
  await expect(page.getByText(`${person.firstName} ${person.lastName}`)).toBeVisible();
  for (const option of options) {
    await expect(page.getByText(option, { exact: true })).toBeVisible();
  }
  await expect(page.getByRole("button", { name: "Register" })).toHaveCount(0);

  // database
  expect(registrationRow(id)).toBe(
    `EXTERNAL|${person.firstName}|${person.lastName}|${person.email}|${person.organization}|`,
  );
  expect(sql(`select count(*) from registration_option where registration_id = '${id}'`)).toBe("4");
  expect(
    sql(`select consent_id from registration_consent where registration_id = '${id}'`).split("\n"),
  ).toContain("privacy");

  // JSON copy
  const copyText = jsonCopy(id);
  const copy = JSON.parse(copyText) as Record<string, unknown>;
  expect(copy.id).toBe(id);
  expect(copy.lastName).toBe(person.lastName);
  expect(copy.email).toBe(person.email);

  // emails: the participant confirmation and the organizer notification with the JSON attachment
  const messages = await mailsMatching(request, token, 2);
  const participant = messages.filter((m) => isAddressedTo(m, person.email));
  const organizer = messages.filter((m) => !isAddressedTo(m, person.email));
  expect(participant).toHaveLength(1);
  expect(organizer.length).toBeGreaterThanOrEqual(1);
  const notification = await mailMessage(request, organizer[0].ID);
  expect(notification.Attachments.length).toBeGreaterThanOrEqual(1);
  const attachment = await mailPart(request, notification.ID, notification.Attachments[0].PartID);
  expect(attachment.toString("utf8")).toBe(copyText);
});
