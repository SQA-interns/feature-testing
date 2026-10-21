# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React 19 single page (Vite 6) with the registration form of `docs/02_contracts/ui-registration-form.json`. It talks to the backend only through `/api` on its own origin (AR-01). The production image serves the build with nginx as an unprivileged user and forwards `/api/` to the backend with a fixed upstream `Host`.

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2
- Playwright's Chromium for the end-to-end tests: `npx playwright install chromium` (once per machine)
- Docker Engine for the image and the end-to-end stack

## Configuration

The frontend holds no secret (AR-07). It reads the reCAPTCHA site key and the test-mode flag from `GET /api/form-config`, so they are configured on the backend (`RECAPTCHA_SITE_KEY`, `CAPTCHA_TEST_MODE`). Other settings:

| Setting                            | Where                                                                                | Default                                                                     |
| ---------------------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------- |
| backend address for the dev server | `vite.config.ts` (`server.proxy`)                                                    | `http://127.0.0.1:8080`                                                     |
| backend address in the image       | `nginx.conf` (`proxy_pass`)                                                          | `http://backend:8080` (compose service name)                                |
| e2e targets                        | environment `E2E_BASE_URL`, `E2E_BACKEND_URL`, `E2E_MAILPIT_URL`, `E2E_COMPOSE_FILE` | `http://127.0.0.1:3000`, `:8080`, `:8025`, `../docker-compose.yml`          |
| e2e organizer settings             | `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`                       | loaded from the repository's `.env` by `scripts/run-e2e.mjs`, never printed |

## Build, check, test, run

| Task                                         | Command                                                                                                 |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| install                                      | `npm ci`                                                                                                |
| build                                        | `npm run build` (type check and bundle into `dist/`)                                                    |
| check (format, lint, type check)             | `npm run check` (format with `npm run format`)                                                          |
| test (unit, component, acceptance)           | `npm test`; with coverage `npm run test:coverage` → `coverage/`                                         |
| end-to-end (needs the running compose stack) | `npm run test:e2e`                                                                                      |
| mutation testing                             | `npm run mutation` → `reports/mutation/`                                                                |
| duplication                                  | `npm run duplication`                                                                                   |
| run (dev server, backend on `:8080`)         | `npm run dev` → <http://127.0.0.1:5173>                                                                 |
| run (image, with the stack)                  | from `02_output/`: `docker compose --env-file ../.env up -d --build frontend` → <http://127.0.0.1:3000> |

## Troubleshooting

| Symptom                                               | Cause and remedy                                                                                                 |
| ----------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| "The registration form could not be loaded"           | The backend is not reachable at `/api/form-config` (dev: start it on `:8080`; stack: `docker compose ps`).       |
| e2e: `required variable POSTGRES_PASSWORD is missing` | Run the tests with `npm run test:e2e`, which loads `.env`, not with `npx playwright test` directly.              |
| e2e: browser not found                                | `npx playwright install chromium`.                                                                               |
| The reCAPTCHA widget does not appear                  | Test mode is off and the site key is empty or not allowed for this domain; locally use `CAPTCHA_TEST_MODE=true`. |
| Mutation score far too low (about 25 %)               | Vitest and `@vitest/coverage-v8` must stay on 4.1.x for `@stryker-mutator/vitest-runner` 10.0.0.                 |
