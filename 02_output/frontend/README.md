# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React single-page app built with Vite: the registration form at `/` and the organizer search page at `/organizer`. It talks to the backend only through `/api` (AR-01); in the container nginx serves the built files and forwards `/api` to the backend. Accessible names follow [`../docs/02_contracts/ui-contract.md`](../docs/02_contracts/ui-contract.md).

## Prerequisites

- Node.js 24.13 with npm 11.6
- for the end-to-end tests: the local stack running (see the root README) and Playwright's Chromium (`npx playwright install chromium` once, if it is not installed yet)

## Commands (ES-05)

| Purpose | Command |
|---|---|
| install | `npm ci` |
| build | `npm run build` (type check, then `dist/`) |
| test | `npm test` (unit and acceptance tests) |
| check | `npm run check` (Prettier, ESLint, `tsc --noEmit`) |
| run (development) | `npm run dev`: <http://127.0.0.1:5173/>, `/api` is forwarded to the backend on `127.0.0.1:8080` |
| run (container) | `docker compose --env-file ../.env up -d --build` in `02_output/`: <http://127.0.0.1:8081/> |
| end-to-end tests | `npm run e2e` (needs the running local stack; reads the organizer settings from the repository's `.env`) |
| coverage | `npm run coverage` (report in `coverage/`) |
| mutation score | `npm run mutation` (report in `reports/mutation/`, about 10 minutes) |

## Configuration

The frontend has no secrets (AR-07). Everything it needs comes from the backend's `GET /api/form-config`: the options, the consents, the reCAPTCHA site key and whether test mode is on (then the form sends `test-pass` and shows "reCAPTCHA test mode"). The organizer credentials are typed into the organizer page and kept in memory only.

| Setting | Where | Default |
|---|---|---|
| backend address in development | `vite.config.ts` (`server.proxy`) | `http://127.0.0.1:8080` |
| backend address in the container | `nginx.conf` (`backend:8080`, fixed upstream Host) | compose service `backend` |
| e2e target | `E2E_FRONTEND_URL`, `E2E_BACKEND_URL`, `E2E_MAILPIT_URL` | `http://127.0.0.1:8081`, `:8080`, `:8025` |

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| "The registration form could not be loaded." | the backend is not reachable through `/api`; start it (or the stack) |
| e2e tests time out on the first step | the stack is not running or not healthy: `docker compose --env-file ../.env ps` |
| e2e tests see 429 answers | the API rate limit counts all local browser requests as one client; start the stack with a higher `RATE_LIMIT_PER_MINUTE` or wait a minute |
| Playwright cannot find a browser | `npx playwright install chromium` |
