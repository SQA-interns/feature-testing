import { expect, test, type Locator } from "@playwright/test";
import { organizerCredentials, sql, unique } from "./support/stack";

const MEAL = "meal-lunch-vegetarian";

function value(item: Locator, label: string): Locator {
  return item.locator(`dt:text-is("${label}") + dd`);
}

test("E2E-06 organizer searches registrations (AC-009-01, 04, 06, 10, 18, 24, 25, 28)", async ({
  page,
  request,
}) => {
  // The scenario counts exactly two registrations, so it starts from an empty table.
  sql("TRUNCATE registration CASCADE");
  const u = unique();
  const external = await request.post("/api/registrations", {
    data: {
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Koš",
      email: `ana.${u}@example.org`,
      organization: "Example Institute",
      optionIds: [MEAL],
      consents: ["privacy"],
      recaptchaToken: "test-pass",
    },
  });
  expect(external.status()).toBe(201);
  const student = await request.post("/api/registrations", {
    data: {
      type: "STUDENT",
      firstName: "Luka",
      lastName: "Kranjc",
      email: `luka.${u}@example.org`,
      studyInstitution: "University of Ljubljana",
      studyProgramme: "Computer Science",
      studentId: "63200001",
      optionIds: [MEAL],
      consents: ["privacy"],
      recaptchaToken: "test-pass",
    },
  });
  expect(student.status()).toBe(201);

  await page.goto("/organizer");
  await expect(
    page.getByRole("heading", { name: "Organizer registrations" }),
  ).toBeVisible();
  await expect(page.getByText("Koš")).toHaveCount(0);

  const { username, password } = organizerCredentials();
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(`${password}-wrong`);
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page.getByRole("alert")).toHaveText(
    "Wrong username or password.",
  );
  await expect(page.getByRole("list", { name: "Registrations" })).toHaveCount(
    0,
  );

  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();

  const items = page
    .getByRole("list", { name: "Registrations" })
    .getByRole("listitem");
  await expect(items).toHaveCount(2);
  await expect(page.getByText("Matches: 2")).toBeVisible();
  await expect(value(items.nth(0), "Last name")).toHaveText("Kranjc");
  await expect(value(items.nth(1), "Last name")).toHaveText("Koš");
  const counts = page.getByRole("region", { name: "Option counts" });
  await expect(counts.getByText("Lunch (vegetarian): 2")).toBeVisible();
  await expect(counts.getByText("Workshop: Legacy systems: 0")).toBeVisible();

  await page.getByLabel("Search").fill("kos");
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByText("Matches: 1")).toBeVisible();
  await expect(items).toHaveCount(1);
  await expect(value(items.nth(0), "Last name")).toHaveText("Koš");

  await page.getByLabel("Search").fill("");
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByText("Matches: 2")).toBeVisible();
  await page.getByLabel("Registration type").selectOption({ label: "Student" });
  await expect(page.getByText("Matches: 1")).toBeVisible();
  await expect(items).toHaveCount(1);
  await expect(value(items.nth(0), "Last name")).toHaveText("Kranjc");
  await expect(counts.getByText("Lunch (vegetarian): 1")).toBeVisible();
});
