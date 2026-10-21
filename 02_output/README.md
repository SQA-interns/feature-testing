# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. External participants and students fill in their own form, choose workshops, events, meals and other activities from a configurable programme, and give the required consent. Every accepted registration is stored in PostgreSQL and as a raw JSON copy; the participant gets a confirmation email and every organizer a notification with the JSON attached. Organizers download all registrations as an Excel workbook, and search and filter them on an organizer page (text, registration type, selected option, 20 per page). Registrations are protected by Google reCAPTCHA v2.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot 4.1 REST API under `/api` (Java 21): form configuration, registrations, export, organizer search | [backend/README.md](backend/README.md) |
| frontend | React 19 single page built with Vite, served by nginx, which also forwards `/api/` to the backend: the registration form at `/`, the organizer search page at `/organizer` | [frontend/README.md](frontend/README.md) |
| local stack | `docker-compose.yml`: backend, frontend, PostgreSQL 16, Mailpit (mail catcher) | this file |

## Quick start (local stack)

Prerequisites: Docker Engine with Docker Compose, and a `.env` file at the repository root copied from `01_input/01_project/00_setup/secrets.env.example` with `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters) and `ORGANIZER_EMAILS` filled in.

```sh
cd 02_output
docker compose --env-file ../.env up -d --build
```

| URL | What |
|---|---|
| http://127.0.0.1:8081 | registration page |
| http://127.0.0.1:8081/organizer | organizer search page (sign in with `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD`) |
| http://127.0.0.1:8080/api/form-config | API (backend directly) |
| http://127.0.0.1:8080/api/export | Excel export (HTTP Basic with `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD`) |
| http://127.0.0.1:8080/api/registrations?text=novak&type=STUDENT | organizer search API (same HTTP Basic login; parameters `text`, `type`, `option`, `page`) |
| http://127.0.0.1:8080/actuator/health/readiness | readiness |
| http://127.0.0.1:8025 | Mailpit: every email sent by the stack |

Locally the reCAPTCHA runs in test mode (a "test mode" checkbox on the page). The settings `CAPTCHA_TEST_MODE` (default `true` locally), `RATE_LIMIT_PER_MINUTE` (default `60`) and `ORGANIZER_HTTPS_ONLY` (default `false` locally, D-11) can be overridden when starting, for example `RATE_LIMIT_PER_MINUTE=1000 docker compose --env-file ../.env up -d`.

Stop with `docker compose --env-file ../.env down`; registrations and JSON copies stay on the named volumes `pgdata` and `jsoncopies` (`down -v` deletes them).

The end-to-end tests run against this stack: see [frontend/README.md](frontend/README.md#end-to-end-tests).

If the backend keeps restarting with `password authentication failed for user "registration"`, the `pgdata` volume was created earlier with another `POSTGRES_PASSWORD` (PostgreSQL keeps the first one). Either delete the old data with `docker compose --env-file ../.env down -v`, or keep it and run the stack under another project name, for example `export COMPOSE_PROJECT_NAME=konferenca-us009` before every `docker compose` command (D-24).

On Windows, clone into a short path or with `git clone -c core.longpaths=true …`: some Java test paths exceed 260 characters below a deep folder.

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | architecture, behaviour, settings, deployment, traceability |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, database schema, JSON copy and configuration schemas, emails, reCAPTCHA, UI |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, coverage and mutation results |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, security evidence, findings |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision of the run |
| [docs/01_requirements-check.md](docs/01_requirements-check.md), [docs/00_preflight-report.md](docs/00_preflight-report.md) | requirements check, environment check |
