# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference: external participants and students register through a web form, choose workshops, events, meals and other activities from a configurable list, and give the required consent. Every accepted registration is stored in PostgreSQL and as a raw JSON copy, confirmed to the participant by email and announced to the organizers by email with the JSON copy attached. Organizers search and filter registrations on an organizer page and export them as an Excel workbook.

## Components

| Component | Folder | What it is |
|---|---|---|
| backend | [backend/](backend/README.md) | Spring Boot REST API (`/api`), PostgreSQL via Flyway, JSON copies, emails, reCAPTCHA, organizer access |
| frontend | [frontend/](frontend/README.md) | React single-page app (registration form at `/`, organizer page at `/organizer`) served by nginx, which forwards `/api` to the backend |

The local stack (`docker-compose.yml`) runs both components with PostgreSQL and Mailpit (a local mail catcher).

## Quick start

Prerequisites: Docker Engine with Docker Compose, and a `.env` file at the repository root (one level above this folder):

1. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` at the repository root and fill in `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (16+ characters) and `ORGANIZER_EMAILS`. The reCAPTCHA and SMTP keys stay empty locally.
2. Start everything from this folder:

   ```bash
   cd 02_output
   docker compose --env-file ../.env up -d --build
   ```

3. Open:
   - registration form: <http://127.0.0.1:8081/>
   - organizer page: <http://127.0.0.1:8081/organizer> (the organizer credentials from `.env`)
   - emails (Mailpit): <http://127.0.0.1:8025/>
   - API: <http://127.0.0.1:8080/api/form-config>, health <http://127.0.0.1:8080/actuator/health>

4. Stop with `docker compose --env-file ../.env down` (data stays in the named volumes `pgdata` and `jsoncopies`; add `-v` to delete it).

Locally reCAPTCHA runs in test mode (`CAPTCHA_TEST_MODE=true`, the form sends the token `test-pass`), organizer access over plain HTTP is allowed (`ORGANIZER_HTTPS_ONLY=false`), and the API allows 60 requests per minute per client and endpoint (`RATE_LIMIT_PER_MINUTE`). All three can be overridden when starting, for example `RATE_LIMIT_PER_MINUTE=1000 docker compose --env-file ../.env up -d`.

## Tests

| What | Command | Needs |
|---|---|---|
| backend unit, architecture and acceptance tests | `cd backend && ./mvnw verify` | Docker (Testcontainers) |
| frontend unit and acceptance tests | `cd frontend && npm ci && npm test` | Node |
| end-to-end tests (browser) | `cd frontend && npm run e2e` | the running local stack above |

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | architecture, behaviour, settings, traceability |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI contract, JSON schemas, email and UI contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, coverage and mutation results |
| [docs/06_verification-report.md](docs/06_verification-report.md) | verification evidence and findings |
| [docs/release-notes.md](docs/release-notes.md) | release notes and manual tests |
| [docs/decisions-log.md](docs/decisions-log.md) | decisions taken during the build |
