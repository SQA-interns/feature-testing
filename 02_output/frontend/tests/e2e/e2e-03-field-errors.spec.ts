import { expect, test } from "@playwright/test";
import { checkMandatoryConsents, chooseType, fillPerson, uniqueToken } from "./support";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const token = uniqueToken("e2e03");
  const posts: string[] = [];
  page.on("request", (request) => {
    if (request.method() === "POST" && request.url().endsWith("/api/registrations")) {
      posts.push(request.url());
    }
  });

  await page.goto("/");
  await chooseType(page, "External participant");
  await fillPerson(page, {
    firstName: "",
    lastName: `Kos${token}`,
    email: "not-an-email",
    organization: "Example d.o.o.",
  });
  await page.getByRole("group", { name: "Workshops" }).getByRole("checkbox").first().check();
  await checkMandatoryConsents(page);
  await page.getByRole("button", { name: "Register" }).click();

  // client-side errors, nothing sent
  await expect(page.getByLabel("First name", { exact: true })).toHaveAccessibleDescription(
    "This field is required.",
  );
  await expect(page.getByLabel("Email", { exact: true })).toHaveAccessibleDescription(
    "Enter a valid email address.",
  );
  expect(posts).toEqual([]);

  // a submission the backend rejects: an option id that is not configured is added on the way
  await page.getByLabel("First name", { exact: true }).fill("Marko");
  await page.getByLabel("Email", { exact: true }).fill(`${token}@example.org`);
  await page.route("**/api/registrations", async (route) => {
    const body = JSON.parse(route.request().postData() ?? "{}") as { options: string[] };
    body.options = [...body.options, "no-such-option-e2e"];
    await route.continue({ postData: JSON.stringify(body) });
  });
  const [response] = await Promise.all([
    page.waitForResponse((r) => r.url().endsWith("/api/registrations")),
    page.getByRole("button", { name: "Register" }).click(),
  ]);

  expect(response.status()).toBe(400);
  const { errors } = (await response.json()) as { errors: { field: string; message: string }[] };
  const optionsError = errors.find((e) => e.field === "options");
  expect(optionsError).toBeDefined();
  await expect(page.getByText(optionsError!.message)).toBeVisible();
  await expect(page.getByRole("heading", { name: "Registration received" })).toHaveCount(0);
  await expect(page.getByLabel("First name", { exact: true })).toHaveValue("Marko");
  await expect(page.getByLabel("Last name", { exact: true })).toHaveValue(`Kos${token}`);
  await expect(page.getByLabel("Organization / institution", { exact: true })).toHaveValue(
    "Example d.o.o.",
  );
});
