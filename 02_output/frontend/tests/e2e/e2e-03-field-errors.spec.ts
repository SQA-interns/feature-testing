import { expect, test } from "@playwright/test";
import { fillRegistration, token } from "./support/stack";

// Covers AC-004-02, AC-004-03, NFR-03.
test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const t = token();
  let posts = 0;
  page.on("request", (r) => {
    if (r.method() === "POST" && r.url().includes("/api/registrations")) posts++;
  });

  await fillRegistration(page, {
    type: "External participant",
    firstName: "Ana",
    lastName: `Errors-${t}`,
    email: `ana.${t}@example.org`,
    organization: "Example d.o.o.",
  });
  const firstName = page.getByRole("textbox", { name: "First name", exact: true });
  const email = page.getByRole("textbox", { name: "Email", exact: true });
  await firstName.fill("");
  await email.fill("not-an-email");
  await page.getByRole("button", { name: "Register" }).click();

  await expect(firstName).toHaveAccessibleDescription("This field is required.");
  await expect(email).toHaveAccessibleDescription("Enter a valid email address.");
  await page.waitForTimeout(500);
  expect(posts, "requests sent with client-side errors").toBe(0);

  await firstName.fill("Ana");
  await email.fill(`ana.${t}@example.org`);
  await page.route("**/api/registrations", async (route) => {
    const body = JSON.parse(route.request().postData() ?? "{}") as { options?: string[] };
    body.options = [...(body.options ?? []), "no-such-option"];
    await route.continue({ postData: JSON.stringify(body) });
  });
  const responsePromise = page.waitForResponse(
    (r) => r.request().method() === "POST" && r.url().includes("/api/registrations"),
  );
  await page.getByRole("button", { name: "Register" }).click();
  const response = await responsePromise;

  expect(response.status()).toBe(400);
  await expect(page.locator("#options-error")).not.toBeEmpty();
  await expect(page.getByRole("heading", { name: "Registration received" })).toHaveCount(0);
  await expect(firstName).toHaveValue("Ana");
  await expect(email).toHaveValue(`ana.${t}@example.org`);
  await expect(page.getByRole("textbox", { name: "Last name", exact: true })).toHaveValue(
    `Errors-${t}`,
  );
  await expect(
    page.getByRole("group", { name: "Workshops", exact: true }).getByRole("checkbox").first(),
  ).toBeChecked();
});
