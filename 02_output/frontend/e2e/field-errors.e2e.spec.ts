// E2E-03 Field errors are shown next to the fields: AC-004-02, AC-004-03, NFR-03.
import { expect, test } from "@playwright/test";
import {
  LABELS,
  field,
  tickMandatoryConsentAndCaptcha,
  tickOnePerCategory,
  uniqueEmail,
} from "./support";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const registrationRequests: string[] = [];
  page.on("request", (r) => {
    if (r.url().endsWith("/api/registrations")) registrationRequests.push(r.method());
  });
  const email = uniqueEmail("errors");

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await field(page, LABELS.lastName).fill("Novak");
  await field(page, LABELS.email).fill("not-an-email");
  await field(page, LABELS.organization).fill("Example d.o.o.");
  await tickOnePerCategory(page);
  await tickMandatoryConsentAndCaptcha(page);

  await page.getByRole("button", { name: "Register" }).click();

  await expect(field(page, LABELS.firstName)).toHaveAccessibleDescription(/\S/);
  await expect(field(page, LABELS.email)).toHaveAccessibleDescription(/\S/);
  await page.waitForTimeout(1000);
  expect(registrationRequests).toEqual([]);

  // Make the backend reject the submission: the request names an option that does not exist.
  await field(page, LABELS.firstName).fill("Ana");
  await field(page, LABELS.email).fill(email);
  await page.route("**/api/registrations", async (route) => {
    const body = route.request().postDataJSON() as { options: string[] };
    body.options = ["e2e-unavailable-option"];
    await route.continue({ postData: JSON.stringify(body) });
  });
  const response = page.waitForResponse((r) => r.url().endsWith("/api/registrations"));
  await page.getByRole("button", { name: "Register" }).click();
  const rejected = await response;

  expect(rejected.status()).toBe(400);
  const { errors } = (await rejected.json()) as { errors: { field: string; message: string }[] };
  expect(errors.map((e) => e.field)).toContain("options");
  for (const error of errors) {
    await expect(page.getByText(error.message).first()).toBeVisible();
  }
  await expect(field(page, LABELS.firstName)).toHaveValue("Ana");
  await expect(field(page, LABELS.lastName)).toHaveValue("Novak");
  await expect(field(page, LABELS.email)).toHaveValue(email);
  await expect(page.getByRole("button", { name: "Register" })).toBeVisible();
});
