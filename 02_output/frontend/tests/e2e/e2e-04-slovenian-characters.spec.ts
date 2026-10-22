import { expect, test } from "@playwright/test";
import {
  awaitMail,
  BACKEND_URL,
  basicAuth,
  EXPORT_COLUMNS,
  fillRegistration,
  jsonCopyBytes,
  organizerEmails,
  readXlsx,
  registrationIdByEmail,
  sql,
  submitAndExpectConfirmation,
  token,
  type Participant,
} from "./support/stack";

// Covers AC-005-04, AC-008-03, NFR-01.
test("E2E-04 Slovenian characters survive the whole chain (AC-005-04, AC-008-03)", async ({
  page,
  request,
}) => {
  const t = token();
  const participant: Participant = {
    type: "External participant",
    firstName: "Čedomir Šžč",
    lastName: `Žužek-ČŠŽ-${t}`,
    email: `cedomir.${t}@example.org`,
    organization: "Fakulteta za računalništvo Čšž ŠŽČ",
  };

  await fillRegistration(page, participant);
  await submitAndExpectConfirmation(page, participant);

  const id = registrationIdByEmail(participant.email);
  expect(id, "registration row").toBeTruthy();
  const [row] = sql(
    `select first_name, last_name, organization from registration where id = '${id}'`,
  );
  expect(row).toEqual([participant.firstName, participant.lastName, participant.organization]);

  const copy = JSON.parse(jsonCopyBytes(id!).toString("utf8")) as Record<string, string>;
  expect([copy.firstName, copy.lastName, copy.organization]).toEqual([
    participant.firstName,
    participant.lastName,
    participant.organization,
  ]);

  const confirmation = await awaitMail(request, participant.email, (m) =>
    m.Text.includes(participant.lastName),
  );
  expect(confirmation.Text).toContain(participant.firstName);
  for (const organizer of organizerEmails()) {
    const notification = await awaitMail(request, organizer, (m) =>
      m.Text.includes(participant.email),
    );
    expect(notification.Text).toContain(participant.firstName);
    expect(notification.Text).toContain(participant.lastName);
    expect(notification.Text).toContain(participant.organization!);
  }

  const exported = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: basicAuth() },
  });
  expect(exported.status()).toBe(200);
  const rows = readXlsx(Buffer.from(await exported.body()));
  const cells = rows.find((r) => r[0] === id);
  expect(cells, "workbook row").toBeDefined();
  expect(cells![EXPORT_COLUMNS.indexOf("firstName")]).toBe(participant.firstName);
  expect(cells![EXPORT_COLUMNS.indexOf("lastName")]).toBe(participant.lastName);
  expect(cells![EXPORT_COLUMNS.indexOf("organization")]).toBe(participant.organization);
});
