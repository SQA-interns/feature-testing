import { expect, test } from "@playwright/test";
import {
  BACKEND_URL,
  EXPORT_COLUMNS,
  basicAuth,
  readWorkbook,
  registerViaPage,
  sql,
  uniqueToken,
} from "./support";

const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({ page, request }) => {
  const token = uniqueToken("e2e05");
  const email = `${token}@example.org`;
  await registerViaPage(page, "External participant", {
    firstName: "Eva",
    lastName: `Kranjc${token}`,
    email,
    organization: "Example d.o.o.",
  });

  const exported = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: basicAuth() },
  });
  expect(exported.status()).toBe(200);
  expect(exported.headers()["content-type"]).toContain(XLSX);
  const rows = readWorkbook(await exported.body());
  expect(rows[0]).toEqual(EXPORT_COLUMNS);
  expect(rows.length - 1).toBe(Number(sql("select count(*) from registration")));
  expect(rows.some((r) => r[EXPORT_COLUMNS.indexOf("email")] === email)).toBe(true);

  const refused = await request.get(`${BACKEND_URL}/api/export`);
  expect(refused.status()).toBe(401);
  expect(refused.headers()["content-type"] ?? "").not.toContain(XLSX);
  expect((await refused.body()).toString("utf8")).not.toContain(email);
});
