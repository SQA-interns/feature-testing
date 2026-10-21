# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Participants register online for a conference, either as external participants or as students. They pick the workshops, events, meals and other activities that are open to their type, and they give the required consent. The form is protected by Google reCAPTCHA v2. Every accepted registration is stored in PostgreSQL and as a raw JSON copy. The participant then gets a confirmation email, each organizer gets a notification with the JSON copy attached, and organizers can export all registrations as an Excel workbook. The conference options and consents come from a configuration file, so a new programme needs no code change.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot 4.1 REST API (Java 21): validation, storage, emails, export, organizer login | [backend/README.md](backend/README.md) |
| frontend | React 19 single page served by nginx; calls only `/api` on its own origin | [frontend/README.md](frontend/README.md) |
| `docker-compose.yml` | the local stack: backend, frontend, PostgreSQL 16, Mailpit (mail catcher) | this file |

## Quick start

Prerequisites: Docker Engine with Compose, JDK 21 and Node.js 24.13.0 with npm 11.6.2 (exact versions: `01_input/01_project/00_setup/tech-stack.md`).

1. Create the secrets file at the **repository root** (one level above `02_output/`): copy `01_input/01_project/00_setup/secrets.env.example` to `.env` and fill in `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters) and `ORGANIZER_EMAILS`. The reCAPTCHA and SMTP keys stay empty locally. `.env` is ignored by git.
2. Build the backend jar (the container image packages it):

   ```bash
   cd 02_output/backend
   ./mvnw -B -DskipTests package        # Windows: mvnw.cmd -B -DskipTests package
   ```

3. Start the stack from `02_output/`:

   ```bash
   cd 02_output
   docker compose --env-file ../.env up -d --build
   docker compose --env-file ../.env ps   # all four services "healthy"
   ```

4. Open:
   - registration form: <http://127.0.0.1:3000> (reCAPTCHA test mode: tick "I am not a robot (test mode)")
   - API: <http://127.0.0.1:8080/api/form-config>
   - caught emails: <http://127.0.0.1:8025>
   - export (organizer login from `.env`): `curl -u "<user>:<password>" -o registrations.xlsx http://127.0.0.1:8080/api/export`
5. Run the end-to-end tests against the running stack: `cd 02_output/frontend && npm ci && npm run test:e2e`.
6. Stop with `docker compose --env-file ../.env down`. Data stays on the named volumes `pgdata` and `json-copies`; `down -v` deletes it.

Settings passed through to the backend (`api-contract.md`): `CAPTCHA_TEST_MODE` (default `true` locally), `RATE_LIMIT_PER_MINUTE` (default `30`), `ORGANIZER_HTTPS_ONLY` (default `false` locally), for example `RATE_LIMIT_PER_MINUTE=100 docker compose --env-file ../.env up -d`.

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | architecture, configuration, API behaviour, security, storage, frontend |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, configuration and JSON copy schemas, database, emails, UI, reCAPTCHA |
| [docs/01_requirements-check.md](docs/01_requirements-check.md) | stories → criteria, gaps |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, runs, coverage, mutation, quality gate |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, runtime demonstration, findings |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests, pending decisions |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision of the run |
| [docs/00_preflight-report.md](docs/00_preflight-report.md) | tool, dependency and secret checks |
| [logs/](logs/) | raw output of builds, tests, scans and demonstrations |
