// E2E-06 organizer searches registrations (AC-009-01, AC-009-02, AC-009-03, AC-009-12, DoD-P06).
// Runs against the local stack; the organizer credentials come from the environment (.env).
import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { assertUuid } from "./support";

interface FormOption {
  id: string;
  name: string;
  availableTo: string[];
}

function credentials(): { username: string; password: string } {
  const username = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!username || !password) {
    throw new Error("Set ORGANIZER_USERNAME and ORGANIZER_PASSWORD (from .env) for the e2e run");
  }
  return { username, password };
}

async function register(request: APIRequestContext, body: Record<string, unknown>) {
  const r = await request.post("/api/registrations", {
    data: { consents: ["privacy"], captchaToken: "test-pass", ...body },
  });
  expect(r.status(), await r.text()).toBe(201);
  return assertUuid(((await r.json()) as { id: string }).id);
}

async function signIn(page: Page, username: string, password: string) {
  await page.getByLabel(/^Username/).fill(username);
  await page.getByLabel(/^Password/).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

/** Text of every data row of the result table. */
async function rows(page: Page): Promise<string[]> {
  return page.getByRole("table").locator("tbody tr").allTextContents();
}

test("E2E-06 organizer searches registrations (AC-009-01, AC-009-02, AC-009-03, AC-009-12)", async ({
  page,
  request,
}) => {
  const stamp = `${Date.now()}${Math.floor(Math.random() * 1e4)}`;
  const externalLast = `Zunanji${stamp}`;
  const studentLast = `Studentka${stamp}`;

  const config = (await (await request.get("/api/form-config")).json()) as {
    options: FormOption[];
  };
  const externalOption = config.options.find((o) => o.availableTo.includes("EXTERNAL"));
  const studentOption = config.options.find(
    (o) => o.availableTo.includes("STUDENT") && o.id !== externalOption?.id,
  );
  expect(externalOption).toBeDefined();
  expect(studentOption).toBeDefined();

  await register(request, {
    type: "EXTERNAL",
    firstName: "Eva",
    lastName: externalLast,
    email: `e2e-search-ext-${stamp}@example.org`,
    organization: "Search Test d.o.o.",
    options: [externalOption!.id],
  });
  await register(request, {
    type: "STUDENT",
    firstName: "Nina",
    lastName: studentLast,
    email: `e2e-search-stu-${stamp}@example.org`,
    studyInstitution: "Univerza v Mariboru",
    studyProgramme: "Informatika",
    studentId: `E${stamp}`.slice(0, 20),
    options: [studentOption!.id],
  });

  await page.goto("/organizer");
  const { username, password } = credentials();

  // wrong credentials: an error and no data
  await signIn(page, username, `wrong-${stamp}`);
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("table")).toHaveCount(0);
  await expect(page.getByText(externalLast)).toHaveCount(0);
  await expect(page.getByText(studentLast)).toHaveCount(0);

  // valid credentials: both new registrations are listed (newest first, so on page 1)
  await signIn(page, username, password);
  await expect(page.getByRole("table")).toBeVisible();
  await expect(page.getByRole("table").getByText(externalLast)).toBeVisible();
  await expect(page.getByRole("table").getByText(studentLast)).toBeVisible();

  // search for the student's last name: only the student
  await page.getByRole("textbox", { name: "Search" }).fill(studentLast);
  await page.getByRole("button", { name: "Search" }).click();
  await expect.poll(async () => (await rows(page)).length).toBe(1);
  expect((await rows(page))[0]).toContain(studentLast);

  // clear the text and filter by type "External": the external participant, no student
  await page.getByRole("textbox", { name: "Search" }).fill("");
  await page.getByRole("combobox", { name: "Type" }).selectOption({ label: "External" });
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByRole("table").getByText(externalLast)).toBeVisible();
  await expect(page.getByRole("table").getByText(studentLast)).toHaveCount(0);
  // column 3 is "Type" (interfaces.md, "Organizer search page")
  const types = await page.getByRole("table").locator("tbody tr td:nth-child(3)").allTextContents();
  expect(types.length).toBeGreaterThan(0);
  expect(types.map((t) => t.trim())).toEqual(types.map(() => "External"));
});
