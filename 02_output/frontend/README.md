# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React single-page app: the registration form at `/` (type choice, the type's fields, options grouped by category, consents, reCAPTCHA, confirmation) and the organizer search page at `/organizer` (sign-in, search, type and option filters, paging). It talks only to `/api` on its own origin (AR-01); in the container, nginx serves the static files and proxies `/api` to the backend. UI contract: `../docs/02_contracts/ui.md`.

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2 (`.npmrc` enforces the engines and exact versions).
- For the end-to-end tests: the running local stack (`../README.md`) and Playwright's Chromium (`npx playwright install chromium` once, if not yet installed).

## Configuration

The frontend has no secret and no build-time setting (AR-07). The reCAPTCHA site key and test mode come from `GET /api/form-config` at runtime. The nginx configuration is `nginx.conf` (port 8080, user `nginx`, security headers, `/api` proxy to `backend:8080`).

## Build

```sh
npm ci
npm run build          # type check and bundle into dist/
```

## Test

```sh
npm test               # 62 tests: unit (src/**/*.test.*) and acceptance (tests/acceptance)
npm run test:coverage  # with V8 coverage in coverage/
npm run mutation       # Stryker mutation testing, report in reports/mutation (about 25 minutes)
```

End-to-end tests (6 scenarios, `tests/e2e`) against the running local stack, with the organizer credentials from `.env`:

```sh
export ORGANIZER_USERNAME="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../../.env | tr -d '\r')"
export ORGANIZER_PASSWORD="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../../.env | tr -d '\r')"
npm run test:e2e
```

They use `http://127.0.0.1:8081` (frontend), `http://127.0.0.1:8080` (backend), `http://127.0.0.1:8025` (Mailpit) and `docker compose exec` on the services `db` and `backend`; override with `E2E_BASE_URL`, `E2E_BACKEND_URL`, `E2E_MAILPIT_URL`, `E2E_COMPOSE_FILE`, `E2E_ENV_FILE`. The stack must run with `CAPTCHA_TEST_MODE=true` (the local default).

## Check

```sh
npm run check          # Prettier, ESLint, tsc --noEmit
npm run duplication    # jscpd
npm audit --omit=dev   # dependency scan of the shipped packages
```

`npm run format` formats the sources.

## Run

- Development server with hot reload, proxying `/api` to a backend on `127.0.0.1:8080`: `npm run dev` (http://127.0.0.1:5173).
- Production-like: the `frontend` service of `../docker-compose.yml` (http://127.0.0.1:8081), built by `Dockerfile` (Node build stage, nginx runtime).

## Troubleshooting

| Symptom                                                             | Cause and remedy                                                               |
| ------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| "The registration form could not be loaded"                         | the backend is not reachable on `/api` (dev server: start the backend on 8080) |
| `npm ci` fails with an engine error                                 | Node or npm differ from 24.13.0 / 11.6.2                                       |
| e2e tests fail with `Set ORGANIZER_USERNAME and ORGANIZER_PASSWORD` | export them from `.env` as shown above                                         |
| e2e tests fail at the first page                                    | the stack is not running or not healthy (`docker compose ps`)                  |
| No reCAPTCHA widget, "reCAPTCHA test mode" shown                    | the backend runs with `CAPTCHA_TEST_MODE=true`                                 |
