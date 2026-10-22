# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference: external participants and students register through a public form protected by reCAPTCHA; every accepted registration is stored in PostgreSQL and as a raw JSON copy, confirmed by email to the participant and announced to the organizers with the JSON attached. Organizers sign in to search the registrations and download them as an Excel workbook. Workshops, events, meals and other activities, and the consents, come from a configuration file.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API under `/api` (Java 21, PostgreSQL, Flyway, SMTP, Apache POI) | [backend/README.md](backend/README.md) |
| frontend | React single-page app (registration form at `/`, organizer search at `/organizer`), served by nginx, which proxies `/api` to the backend | [frontend/README.md](frontend/README.md) |
| local stack | `docker-compose.yml`: PostgreSQL, Mailpit (mail catcher), backend, frontend | this file |

## Quick start (local stack)

Prerequisites: JDK 21, Docker Engine with Compose, Node.js 24 with npm 11 (only for the frontend tools and the end-to-end tests).

1. At the repository root, copy `01_input/01_project/00_setup/secrets.env.example` to `.env` and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters) and `ORGANIZER_EMAILS`. reCAPTCHA keys are not needed locally (test mode).
2. Build the backend jar (the backend image copies it):

   ```sh
   cd 02_output/backend
   ./mvnw -B package -DskipTests
   ```

3. Start the stack from `02_output`:

   ```sh
   cd 02_output
   docker compose --env-file ../.env up -d --build
   docker compose --env-file ../.env ps        # wait until all four services are healthy
   ```

4. Use it:

   | What | Where |
   |---|---|
   | Registration form | http://127.0.0.1:8081/ (reCAPTCHA test mode) |
   | Organizer search | http://127.0.0.1:8081/organizer (credentials from `.env`) |
   | Excel export | `curl -u <user>:<password> -o registrations.xlsx http://127.0.0.1:8080/api/export` |
   | REST API | http://127.0.0.1:8080/api (contract: `docs/02_contracts/openapi.json`) |
   | Health | http://127.0.0.1:8080/actuator/health/readiness |
   | Emails | Mailpit at http://127.0.0.1:8025 |

5. Stop with `docker compose --env-file ../.env down`; data stays in the volumes `pgdata` and `json-copies` (`down -v` deletes it).

Settings that `docker compose` takes from the environment: `CAPTCHA_TEST_MODE` (default `true` locally), `RATE_LIMIT_PER_MINUTE` (default 60), `ORGANIZER_HTTPS_ONLY` (default `false` locally), plus `CONFERENCE_NAME` and `CONFERENCE_CONFIG`. Example: `RATE_LIMIT_PER_MINUTE=200 docker compose --env-file ../.env up -d`.

End-to-end tests (Chromium) against the running stack: see [frontend/README.md](frontend/README.md#test).

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | specification, traceability of AC, SR, SB, NFR, AR |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, JSON schemas (JSON copy, conference configuration), database, emails, reCAPTCHA, UI |
| [docs/01_requirements-check.md](docs/01_requirements-check.md) | stories → criteria |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, runs, coverage and mutation figures |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, runtime demonstration, security evidence, findings |
| [docs/release-notes.md](docs/release-notes.md) | delivered scope, limitations, manual tests, pending decisions |
| [docs/decisions-log.md](docs/decisions-log.md) | decision records D-01 … D-16 |
| [docs/00_preflight-report.md](docs/00_preflight-report.md) | environment and dependency checks |
