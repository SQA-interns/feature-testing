// E2E-05 Organizer exports registrations: AC-008-01, AC-008-02, DoD-P04.
import { expect, test } from "@playwright/test";
import { BACKEND_URL, XLSX, organizerAuth, sheetRows, sql, uniqueEmail } from "./support";

const HEADERS = [
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
  const email = uniqueEmail("export");
  const created = await request.post(`${BACKEND_URL}/api/registrations`, {
    data: {
      type: "EXTERNAL",
      firstName: "Eva",
      lastName: "Kranjc",
      email,
      organization: "Export d.o.o.",
      studyInstitution: null,
      studyProgramme: null,
      studentId: null,
      options: [],
      consents: ["privacy"],
      captchaToken: "test-pass",
    },
  });
  expect(created.status()).toBe(201);
  const id = ((await created.json()) as { id: string }).id;

  const exported = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: organizerAuth() },
  });
  expect(exported.status()).toBe(200);
  expect(exported.headers()["content-type"]).toContain(XLSX);
  const rows = sheetRows(Buffer.from(await exported.body()));
  expect(rows[0]).toEqual(HEADERS);
  const [[count]] = sql("SELECT count(*) FROM registration");
  expect(rows.length - 1).toBe(Number(count));
  expect(new Set(rows.slice(1).map((r) => r[0])).size).toBe(Number(count));
  const row = rows.find((r) => r[0] === id);
  expect(row?.slice(1, 6)).toEqual(["EXTERNAL", "Eva", "Kranjc", email, "Export d.o.o."]);

  const refused = await request.get(`${BACKEND_URL}/api/export`);
  expect(refused.status()).toBe(401);
  expect(refused.headers()["content-type"] ?? "").not.toContain(XLSX);
  const refusedBody = (await refused.body()).toString("utf8");
  expect(refusedBody).not.toContain(email);
  expect(refusedBody).not.toContain("Kranjc");
});
