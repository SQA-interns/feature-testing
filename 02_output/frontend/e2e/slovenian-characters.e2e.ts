import { expect, test } from "@playwright/test";
import {
  consentAndCaptcha,
  expectConfirmation,
  fillFields,
  openForm,
  submit,
  type Participant,
} from "./support/form";
import {
  BACKEND_URL,
  awaitMailsTo,
  jsonCopy,
  organizerAuthorization,
  organizerEmails,
  registrationsByEmail,
  uniqueEmail,
} from "./support/stack";
import { readRecords } from "./support/xlsx";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
  request,
}) => {
  const p: Participant = {
    firstName: "Žiga Čeh",
    lastName: "Šuštaršič",
    email: uniqueEmail("e2e-slovenian"),
    organization: "Zavod ČŠŽ, Črnuče čšž",
  };

  await openForm(page, "External participant");
  await fillFields(page, p);
  await consentAndCaptcha(page);
  await submit(page);

  await expectConfirmation(page, p);

  const rows = registrationsByEmail(p.email);
  expect(rows).toHaveLength(1);
  const row = rows[0];
  expect(row).toMatchObject({
    firstName: p.firstName,
    lastName: p.lastName,
    organization: p.organization,
  });

  expect(jsonCopy(row.id)).toMatchObject({
    firstName: p.firstName,
    lastName: p.lastName,
    organization: p.organization,
  });

  const participantMails = await awaitMailsTo(p.email);
  expect(participantMails).toHaveLength(1);
  expect(participantMails[0].text).toContain(p.firstName);
  expect(participantMails[0].text).toContain(p.lastName);
  for (const organizer of organizerEmails()) {
    const mails = await awaitMailsTo(organizer, p.email);
    expect(mails).toHaveLength(1);
    expect(mails[0].text).toContain(p.firstName);
    expect(mails[0].text).toContain(p.lastName);
    expect(mails[0].text).toContain(p.organization!);
  }

  const response = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: organizerAuthorization() },
  });
  expect(response.status()).toBe(200);
  const record = readRecords(await response.body()).records.find((r) => r.id === row.id);
  expect(record).toMatchObject({
    firstName: p.firstName,
    lastName: p.lastName,
    organization: p.organization,
  });
});
