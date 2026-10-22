import { expect, test } from "@playwright/test";
import {
  BACKEND_URL,
  basicAuth,
  EXPORT_COLUMNS,
  readXlsx,
  registerViaApi,
  sql,
  token,
} from "./support/stack";

// Covers AC-008-01, AC-008-02, DoD-P04.
test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({ request }) => {
  const t = token();
  const email = `export.${t}@example.org`;
  const id = await registerViaApi(request, {
    type: "EXTERNAL",
    firstName: "Export",
    lastName: `Person-${t}`,
    email,
    organization: "Export Org",
  });

  const response = await request.get(`${BACKEND_URL}/api/export`, {
    headers: { Authorization: basicAuth() },
  });

  expect(response.status()).toBe(200);
  expect(response.headers()["content-type"]).toContain(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  );
  const rows = readXlsx(Buffer.from(await response.body()));
  expect(rows[0]).toEqual(EXPORT_COLUMNS);
  const [[count]] = sql("select count(*) from registration");
  expect(rows.length - 1, "one row per registration").toBe(Number(count));
  expect(rows.some((r) => r[0] === id && r[4] === email)).toBe(true);

  const refused = await request.get(`${BACKEND_URL}/api/export`);
  expect(refused.status()).toBe(401);
  const body = Buffer.from(await refused.body());
  expect(body.subarray(0, 2).toString("latin1")).not.toBe("PK");
  expect(body.toString("utf8")).not.toContain(email);
});
