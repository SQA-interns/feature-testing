# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

A web application for registering participants of a conference. External participants and students fill in a form, choose workshops, events, meals and other activities, and give the required consents. A registration is accepted only after server-side validation and a Google reCAPTCHA v2 check. It is stored in PostgreSQL and as a raw JSON copy, and then confirmed on the page and by email. The organizers receive a notification email with the JSON copy attached and can download every registration as an Excel workbook.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API under `/api`: validation, storage, emails, export | [backend/README.md](backend/README.md) |
| frontend | React single-page form, served by nginx, which also proxies `/api` to the backend | [frontend/README.md](frontend/README.md) |

`docker-compose.yml` runs the local stack: frontend, backend, PostgreSQL and Mailpit (a local mail catcher). The conference options and consents are configured in [`config/conference.json`](config/conference.json); see the backend README.

## Quick start (local stack)

Prerequisites: Docker with Compose v2, and JDK 21 to package the backend jar that the image copies.

1. In the repository root, copy `01_input/01_project/00_setup/secrets.env.example` to `.env` and fill in `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (random, at least 16 characters) and `ORGANIZER_EMAILS`. Locally, any address works: all mail goes to Mailpit. `.env` is ignored by git.
2. Build the backend jar, then start the stack from `02_output/`:

   ```sh
   cd 02_output
   ./backend/mvnw -B -f backend/pom.xml package -DskipTests
   docker compose --env-file ../.env up -d --build --wait
   ```

3. Open the form at <http://127.0.0.1:8080> and the received emails at <http://127.0.0.1:8025>. Locally, the captcha is the test-mode checkbox "I am not a robot (test mode)".
4. Download the export with the organizer credentials from `.env` (loaded into the shell, never printed):

   ```sh
   set -a; . ../.env; set +a
   curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" -o registrations.xlsx http://127.0.0.1:8080/api/registrations/export
   ```

5. Stop with `docker compose --env-file ../.env down`. The data stays in the named volumes `pgdata` and `json-copies`; `down -v` deletes it.

To use other host ports, set `FRONTEND_HOST_PORT` and `MAILPIT_HOST_PORT`. To keep this stack apart from another one on the same machine, set `COMPOSE_PROJECT_NAME`, which also gives it its own volumes. Keep the same `COMPOSE_PROJECT_NAME` exported when you run the end-to-end tests.

## Tests

| Suite | Command |
|---|---|
| Backend: unit, integration, architecture, acceptance (needs Docker) | `cd backend && ./mvnw -B test` |
| Frontend: unit, component, acceptance | `cd frontend && npm test` |
| End-to-end, against the running local stack | `cd frontend && npx playwright test`, with the organizer variables exported (see the frontend README) |

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | Specification: architecture, API, validation, storage, emails, export, security, deployment |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, JSON schemas, database schema, email texts, UI texts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | Test levels, how to run them, coverage and mutation figures |
| [docs/06_verification-report.md](docs/06_verification-report.md) | Verification: runtime demonstration, security evidence, scanners, findings |
| [docs/release-notes.md](docs/release-notes.md) | What is delivered, known limitations, what must be tested manually |
| [docs/decisions-log.md](docs/decisions-log.md) | Every decision of the run |
| [logs/](logs/) | Raw output of builds, tests, scans and demonstrations |
