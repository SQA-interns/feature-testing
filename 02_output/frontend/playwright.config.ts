import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests (e2e-scenarios.md) against the running local stack:
 * `docker compose up` in 02_output (see 02_output/README.md). Base URLs come from
 * E2E_BASE_URL and E2E_MAILPIT_URL; organizer credentials from ORGANIZER_USERNAME
 * and ORGANIZER_PASSWORD, exported from .env by the command that runs the tests.
 */
export default defineConfig({
  testDir: "e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  reporter: [["list"], ["junit", { outputFile: "test-results/e2e-junit.xml" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:8080",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
