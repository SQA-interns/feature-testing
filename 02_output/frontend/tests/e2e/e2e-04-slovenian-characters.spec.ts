import { expect, test } from "@playwright/test";
import {
  BACKEND_URL,
  EXPORT_COLUMNS,
  basicAuth,
  isAddressedTo,
  jsonCopy,
  mailMessage,
  mailsMatching,
  readWorkbook,
  registerViaPage,
  registrationRow,
  uniqueToken,
} from "./support";

test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
  request,
}) => {
  const token = uniqueToken("e2e04");
  const person = {
    firstName: "Čedomir Žan",
    lastName: `Šuštaršič-Žužek čšž ${token}`,
    email: `${token}@example.org`,
    organization: "Društvo čebelarjev Šentjur ČŠŽ",
  };

  const id = await registerViaPage(page, "External participant", person);

  // page confirmation
  await expect(page.getByText(`${person.firstName} ${person.lastName}`)).toBeVisible();

  // database row and JSON copy
  expect(registrationRow(id)).toBe(
    `EXTERNAL|${person.firstName}|${person.lastName}|${person.email}|${person.organization}|`,
  );
  const copyText = jsonCopy(id);
  expect(copyText).toContain(person.firstName);
  expect(copyText).toContain(person.lastName);
  expect(copyText).toContain(person.organization);

  // both emails
  const messages = await mailsMatching(request, token, 2);
  const participant = messages.find((m) => isAddressedTo(m, person.email));
  const organizer = messages.find((m) => !isAddressedTo(m, person.email));
  expect(participant).toBeDefined();
  expect(organizer).toBeDefined();
  const participantText = (await mailMessage(request, participant!.ID)).Text;
  expect(participantText).toContain(person.firstName);
  expect(participantText).toContain(person.lastName);
  const organizerText = (await mailMessage(request, organizer!.ID)).Text;
  expect(organizerText).toContain(person.lastName);
  expect(organizerText).toContain(person.organization);

  // exported workbook
  const exported = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: basicAuth() },
  });
  expect(exported.status()).toBe(200);
  const rows = readWorkbook(await exported.body());
  expect(rows[0]).toEqual(EXPORT_COLUMNS);
  const row = rows.find((r) => r[EXPORT_COLUMNS.indexOf("id")] === id);
  expect(row).toBeDefined();
  expect(row![EXPORT_COLUMNS.indexOf("firstName")]).toBe(person.firstName);
  expect(row![EXPORT_COLUMNS.indexOf("lastName")]).toBe(person.lastName);
  expect(row![EXPORT_COLUMNS.indexOf("organization")]).toBe(person.organization);
});
