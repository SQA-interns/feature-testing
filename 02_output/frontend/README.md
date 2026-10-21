# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React 19 single page for the registration form (`docs/02_contracts/interfaces.md`, "Registration form"), built with Vite and served by nginx. It talks to the backend only through `/api` (AR-01) and holds no secret: the reCAPTCHA site key comes from `GET /api/form-config` (AR-07).

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2 (`engine-strict` is on).
- For `run`: the backend on `http://127.0.0.1:8080` (the Vite dev server proxies `/api` there).
- For the end-to-end tests: the local stack (root [README](../README.md)) and Playwright's Chromium (`npx playwright install chromium` once, if it is missing).

## Configuration

The frontend has no settings of its own. nginx (`nginx.conf`) forwards `/api/` to `backend:8080` with a fixed `Host` (SR-08), limits bodies to 16 KiB and adds the security headers. In production the external reverse proxy serves `/` from this container and forwards `/api` to the backend.

## Commands (ES-05)

| Purpose         | Command                                                                                             |
| --------------- | --------------------------------------------------------------------------------------------------- |
| install         | `npm ci`                                                                                            |
| build           | `npm run build` (type check, then `dist/`)                                                          |
| test            | `npm test` (unit and acceptance tests, Vitest); `npm run coverage` for the v8 report in `coverage/` |
| check           | `npm run check` (Prettier, ESLint, `tsc --noEmit`); format with `npm run format`                    |
| run             | `npm run dev` (http://127.0.0.1:5173)                                                               |
| mutation        | `npm run mutation` (Stryker, about 17 minutes; report in `reports/mutation/`)                       |
| duplication     | `npm run duplication`                                                                               |
| dependency scan | `npm audit`                                                                                         |
| container       | `docker build -t registration-frontend .` (nginx as user `nginx`, port 8080)                        |

## End-to-end tests

The five tests in `e2e/` (E2E-01 … E2E-05) need the running local stack. They read the database and the JSON copies through `docker compose exec`, so Compose must find `.env`, and the export check needs the organizer login. Set both from `.env` without printing them, from `02_output/frontend` in Git Bash:

```sh
export COMPOSE_ENV_FILES="$(cd ../.. && pwd)/.env"
export ORGANIZER_USERNAME="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../../.env | tr -d '\r')"
export ORGANIZER_PASSWORD="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../../.env | tr -d '\r')"
npm run e2e
```

## Troubleshooting

- "The registration form could not be loaded": the backend is not reachable through `/api` (check `docker compose ps`, or start the backend for `npm run dev`).
- `npm ci` fails with an engine error: use Node 24.13.0 / npm 11.6.2.
- e2e: `required variable … is missing a value`: `COMPOSE_ENV_FILES` is not set; `Set ORGANIZER_USERNAME and ORGANIZER_PASSWORD`: export them as shown above.
- e2e: many 429 answers: the stack's rate limit (60 per minute) was hit by repeated runs; wait a minute or start the stack with a higher `RATE_LIMIT_PER_MINUTE`.
