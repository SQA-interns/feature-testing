import { expect, test } from "@playwright/test";
import { BACKEND_URL, XLSX, organizerAuthorization, registrationCount, sql } from "./support/stack";
import { readRecords } from "./support/xlsx";

const COLUMNS = [
  "id",
  "type",
  "firstName",
  "lastName",
  "email",
  "organization",
  "studyInstitution",
  "studyProgramme",
  "studentId",
  "options",
  "consents",
  "registeredAt",
];

test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({ request }) => {
  const response = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: organizerAuthorization() },
  });

  expect(response.status()).toBe(200);
  expect(response.headers()["content-type"]).toContain(XLSX);
  const workbook = await response.body();
  const { header, records } = readRecords(workbook);
  expect(header).toEqual(COLUMNS);
  expect(records).toHaveLength(registrationCount());
  const ids = sql("select id from registration").map(([id]) => id);
  expect(records.map((r) => r.id).sort()).toEqual(ids.sort());

  const refused = await request.get(`${BACKEND_URL}/api/export`);
  expect(refused.status()).toBe(401);
  const body = await refused.body();
  expect(body.subarray(0, 2).toString("latin1")).not.toBe("PK");
  const text = body.toString("utf8");
  for (const [email] of sql("select email from registration")) {
    expect(text).not.toContain(email);
  }
});
