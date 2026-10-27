import { expect, test } from "@playwright/test";

test("E2E-03 field errors are shown next to the fields (AC-004-02, AC-004-03)", async ({
  page,
}) => {
  const registrationRequests: string[] = [];
  page.on("request", (request) => {
    if (
      request.method() === "POST" &&
      request.url().includes("/api/registrations")
    ) {
      registrationRequests.push(request.url());
    }
  });
  // An option the backend knows as inactive is offered to the browser, so the backend rejects it.
  await page.route("**/api/form-config", async (route) => {
    const response = await route.fetch();
    const config = await response.json();
    config.options.push({
      id: "ws-legacy",
      name: "Workshop: Legacy systems",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    });
    await route.fulfill({ response, json: config });
  });

  await page.goto("/");
  await page.getByRole("radio", { name: "External participant" }).check();
  await page.getByLabel("Last name").fill("Novak");
  await page.getByLabel("Email").fill("not-an-email");
  await page.getByLabel("Organization / institution").fill("Example Institute");
  await page
    .getByRole("checkbox", {
      name: /I agree that my personal data is processed/,
    })
    .check();
  await page
    .getByRole("checkbox", { name: "I am not a robot (test mode)" })
    .check();
  await page.getByRole("button", { name: "Register" }).click();

  await expect(page.getByLabel("First name")).toHaveAccessibleDescription(
    "This field is required.",
  );
  await expect(page.getByLabel("Email")).toHaveAccessibleDescription(
    "Enter a valid email address.",
  );
  expect(registrationRequests).toHaveLength(0);

  await page.getByLabel("First name").fill("Ana");
  await page.getByLabel("Email").fill("ana.novak@example.org");
  await page
    .getByRole("checkbox", { name: "Workshop: Legacy systems" })
    .check();
  const response = page.waitForResponse(
    (r) =>
      r.request().method() === "POST" && r.url().includes("/api/registrations"),
  );
  await page.getByRole("button", { name: "Register" }).click();
  expect((await response).status()).toBe(400);

  await expect(
    page.getByText("One or more selected options are not available."),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Registration received" }),
  ).toHaveCount(0);
  await expect(page.getByLabel("First name")).toHaveValue("Ana");
  await expect(page.getByLabel("Last name")).toHaveValue("Novak");
  await expect(page.getByLabel("Email")).toHaveValue("ana.novak@example.org");
  await expect(
    page.getByRole("checkbox", { name: "Workshop: Legacy systems" }),
  ).toBeChecked();
  expect(registrationRequests).toHaveLength(1);
});
