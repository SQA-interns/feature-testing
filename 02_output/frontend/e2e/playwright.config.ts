import { defineConfig, devices } from "@playwright/test";

/** End-to-end tests against the running local stack (`docker compose up` in `02_output/`). */
export default defineConfig({
  testDir: ".",
  testMatch: "**/*.e2e.ts",
  outputDir: "../test-results/e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: [["list"], ["json", { outputFile: "../test-results/e2e-results.json" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:3000",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
