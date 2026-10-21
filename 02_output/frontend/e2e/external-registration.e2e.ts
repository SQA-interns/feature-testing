import { expect, test } from "@playwright/test";
import {
  consentAndCaptcha,
  expectConfirmation,
  fillFields,
  openForm,
  selectOnePerCategory,
  submit,
  type Participant,
} from "./support/form";
import {
  GROUPS,
  awaitMailsTo,
  jsonAttachments,
  jsonCopy,
  organizerEmails,
  registrationsByEmail,
  storedOptions,
  uniqueEmail,
} from "./support/stack";

test("E2E-01 external participant registers (AC-001-01, AC-001-08, AC-004-01, AC-005-01, AC-005-02, AC-006-01, AC-007-01, AC-007-02)", async ({
  page,
}) => {
  const p: Participant = {
    firstName: "Eva",
    lastName: "Kos",
    email: uniqueEmail("e2e-external"),
    organization: "Primer d.o.o.",
  };

  await openForm(page, "External participant");
  await expect(page.getByRole("textbox", { name: "Organization / institution" })).toBeVisible();
  await expect(page.getByRole("textbox", { name: "Student ID" })).toHaveCount(0);
  for (const legend of GROUPS) {
    await expect(page.getByRole("group", { name: legend })).toBeVisible();
  }
  await fillFields(page, p);
  const options = await selectOnePerCategory(page);
  expect(options).toHaveLength(4);
  await consentAndCaptcha(page);
  await submit(page);

  await expectConfirmation(page, p);

  const rows = registrationsByEmail(p.email);
  expect(rows).toHaveLength(1);
  const row = rows[0];
  expect(row).toMatchObject({
    type: "EXTERNAL",
    firstName: p.firstName,
    lastName: p.lastName,
    email: p.email,
    organization: p.organization,
  });
  expect(storedOptions(row.id).sort()).toEqual([...options].sort());

  const copy = jsonCopy(row.id);
  expect(copy).toMatchObject({ id: row.id, type: "EXTERNAL", email: p.email, firstName: "Eva" });

  const participantMails = await awaitMailsTo(p.email);
  expect(participantMails).toHaveLength(1);
  expect(participantMails[0].text).toContain("Eva");

  for (const organizer of organizerEmails()) {
    const mails = await awaitMailsTo(organizer, p.email);
    expect(mails).toHaveLength(1);
    expect(await jsonAttachments(mails[0])).toEqual([copy]);
  }
});
