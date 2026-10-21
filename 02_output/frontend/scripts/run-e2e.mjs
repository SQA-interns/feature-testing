// Runs the end-to-end tests against the local stack (`docker compose up` in 02_output/).
// Loads the repository's .env into the environment of the test run (values are never printed),
// so the tests get the organizer settings and `docker compose exec` can interpolate the compose
// file. Usage: npm run test:e2e [-- <playwright arguments>]
import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const envFile = resolve(here, "../../../.env");
const env = { ...process.env };

if (existsSync(envFile)) {
  for (const line of readFileSync(envFile, "utf8").split(/\r?\n/)) {
    const match = /^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/.exec(line);
    if (match && env[match[1]] === undefined) env[match[1]] = match[2].replace(/^["']|["']$/g, "");
  }
  env.COMPOSE_ENV_FILES ??= envFile;
} else {
  console.warn(`No ${envFile}; using the current environment only`);
}

const result = spawnSync(
  "npx",
  ["playwright", "test", "-c", "e2e/playwright.config.ts", ...process.argv.slice(2)],
  { cwd: resolve(here, ".."), env, stdio: "inherit", shell: process.platform === "win32" },
);
process.exit(result.status ?? 1);
