import { expect, test } from "@playwright/test";
import { basicAuth, sql, unique, workbookRows } from "./support/stack";

const XLSX =
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({
  request,
}) => {
  // At least one stored registration, made through the public API.
  const created = await request.post("/api/registrations", {
    data: {
      type: "EXTERNAL",
      firstName: "Export",
      lastName: `Check${unique()}`,
      email: `export.${unique()}@example.org`,
      organization: "Example Institute",
      optionIds: [],
      consents: ["privacy"],
      recaptchaToken: "test-pass",
    },
  });
  expect(created.status()).toBe(201);

  const authorized = await request.get("/api/registrations/export", {
    headers: { Authorization: basicAuth() },
  });
  expect(authorized.status()).toBe(200);
  expect(authorized.headers()["content-type"]).toContain(XLSX);
  const body = await authorized.body();
  expect(body.subarray(0, 2).toString("latin1")).toBe("PK");
  const rows = workbookRows(body);
  const storedIds = sql("SELECT id FROM registration").map((r) => r[0]);
  expect(storedIds.length).toBeGreaterThan(0);
  expect(rows.length - 1).toBe(storedIds.length);
  for (const id of storedIds) {
    expect(rows.filter((cells) => cells.includes(id))).toHaveLength(1);
  }

  const anonymous = await request.get("/api/registrations/export");
  expect(anonymous.status()).toBe(401);
  const anonymousBody = (await anonymous.body()).toString("utf8");
  expect(anonymous.headers()["content-type"] ?? "").not.toContain(XLSX);
  for (const id of storedIds) {
    expect(anonymousBody).not.toContain(id);
  }
});
