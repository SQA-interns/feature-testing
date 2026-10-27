# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

React single-page registration form (texts and behaviour: [`../docs/02_contracts/registration-form-ui.md`](../docs/02_contracts/registration-form-ui.md)). It loads the form configuration from `/api/form-config`, validates the fields before sending, and shows the backend's field errors next to the fields. It shows the confirmation only after the backend has accepted the registration. It talks to the backend only through relative `/api` paths.

In the container, nginx serves the built files as a non-root user on port 8080. It proxies `/api/` to the backend with its own `Host` header (SR-08) and sets the security headers.

## Prerequisites

- Node 24.13.0 with npm 11.6.2. With nvm: `nvm use` in this folder (reads `.nvmrc`).
- For the end-to-end tests: the running local stack (root README, quick start) and Playwright's Chromium (`npx playwright install chromium` once).

## Configuration

The frontend has no settings of its own. The conference name, captcha mode and site key, options and consents all come from the backend at runtime. In development, Vite proxies `/api` to `http://127.0.0.1:8080`, the local stack.

The end-to-end tests read these environment variables:

| Variable | Meaning | Source |
|---|---|---|
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | Organizer login for E2E-05 | `.env` in the repository root (secret; export it, do not print it) |
| `ORGANIZER_EMAILS` | Recipients the organizer emails are checked against | `.env` |
| `E2E_BASE_URL`, `E2E_MAILPIT_URL` | Override `http://127.0.0.1:8080` and `http://127.0.0.1:8025` | optional |

Run them from this folder, for example:

```sh
set -a; . ../../.env; set +a
npx playwright test
```

## Commands

Run from this folder after `npm ci`.

| Purpose | Command |
|---|---|
| Install dependencies (exact, from the lock file) | `npm ci` |
| Build (to `dist/`, with type check) | `npm run build` |
| Test (unit, component and acceptance, Vitest) | `npm test` |
| Check (Prettier, ESLint, TypeScript) | `npm run check` |
| Run the dev server (needs the local stack for `/api`) | `npm run dev`, then open the printed URL |
| Run in the local stack | from `02_output/`: `docker compose --env-file ../.env up -d --build --wait` |
| End-to-end tests (Playwright, against the stack) | `npx playwright test` |
| Coverage (v8, `coverage/`) | `npm run coverage` |
| Mutation testing (Stryker, `reports/mutation/`) | `npm run mutation` |
| Duplication | `npm run duplication` |
| Dependency scan | `npm audit` (`npm audit --omit=dev` for the shipped code) |

## Troubleshooting

- **"The registration form is not available right now."** The backend cannot be reached through `/api`. Start the local stack, or check `docker compose logs backend`.
- **`npm ci` complains about the engine.** Switch to Node 24.13.0 (`nvm use`).
- **End-to-end tests time out at the start.** Check that the stack is up and healthy (`docker compose ps`), and that `E2E_BASE_URL` points to it when you use other ports.
- **E2E-05 fails with 401.** The organizer variables in the shell do not match the `.env` the stack was started with.
- **The reCAPTCHA widget does not appear in production.** The CSP allows scripts and frames only from `www.google.com/recaptcha` and `www.gstatic.com/recaptcha`, so check that the site key belongs to the served domain.
