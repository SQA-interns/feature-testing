import { expect, test } from "@playwright/test";
import { consentAndCaptcha, fillFields, openForm, submit } from "./support/form";
import { registrationsByEmail, uniqueEmail } from "./support/stack";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const email = uniqueEmail("e2e-errors");
  const posts: string[] = [];
  page.on("request", (r) => {
    if (r.method() === "POST" && r.url().endsWith("/api/registrations")) posts.push(r.url());
  });

  await openForm(page, "Student");
  await fillFields(page, {
    firstName: "",
    lastName: "Horvat",
    email: "not-an-email@",
    studyInstitution: "Univerza v Mariboru",
    studyProgramme: "Elektrotehnika",
    studentId: "93210003",
  });
  await consentAndCaptcha(page);
  await submit(page);

  await expect(page.locator("#firstName-error")).toHaveText("This field is required.");
  await expect(page.locator("#email-error")).toHaveText("Enter a valid email address.");
  expect(posts).toHaveLength(0);

  await page.getByRole("textbox", { name: "First name" }).fill("Maja");
  await page.getByRole("textbox", { name: "Email" }).fill(email);
  await page.route("**/api/registrations", async (route) => {
    const body = JSON.parse(route.request().postData() ?? "{}") as { options?: string[] };
    body.options = [...(body.options ?? []), "e2e-unavailable-option"];
    await route.continue({ postData: JSON.stringify(body) });
  });
  const answer = page.waitForResponse((r) => r.url().endsWith("/api/registrations"));
  await submit(page);
  expect((await answer).status()).toBe(400);

  await expect(page.locator("#options-error")).not.toBeEmpty();
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(page.getByRole("textbox", { name: "First name" })).toHaveValue("Maja");
  await expect(page.getByRole("textbox", { name: "Last name" })).toHaveValue("Horvat");
  await expect(page.getByRole("textbox", { name: "Email" })).toHaveValue(email);
  await expect(page.getByRole("textbox", { name: "Student ID" })).toHaveValue("93210003");
  expect(posts).toHaveLength(1);
  expect(registrationsByEmail(email)).toHaveLength(0);
});
