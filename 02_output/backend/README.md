# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API of the conference registration (`docs/02_contracts/openapi.yaml`), root package `si.konferenca.registration`, ports-and-adapters layout (`docs/02_specification.md` section 2).

## Prerequisites

- JDK 21 (pinned: Eclipse Temurin 21.0.10+7; built and tested with Oracle JDK 21.0.11 in the base run, D-02, and Ubuntu OpenJDK 21.0.12.1 in the US-009 run, D-19). Maven comes with the wrapper (`./mvnw`, Maven 3.9.9).
- Docker for the tests (Testcontainers starts PostgreSQL 16.15 and Mailpit 1.31.1).
- For `run`: a PostgreSQL database and an SMTP server, for example the local stack's (`docker compose --env-file ../.env up -d postgres mailpit` in `02_output/`; publish their ports or use the full stack instead).

## Configuration

All settings are environment variables (`docs/02_specification.md` section 4). Secrets come from `.env` (keys listed in `01_input/01_project/00_setup/secrets.env.example`) and have no default.

| Setting | Default | Notes |
|---|---|---|
| `APP_ENVIRONMENT` | `local` | `production` refuses unsafe settings at start (SR-02) |
| `DB_URL`, `DB_USER`, `POSTGRES_PASSWORD` | `jdbc:postgresql://localhost:5432/registration`, `registration`, none (secret) | |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost`, `1025`, `false`, empty, empty | SMTP account only in production (secret) |
| `MAIL_FROM` | `registration@konferenca.local` | |
| `ORGANIZER_EMAILS`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none (secrets) | comma-separated recipients; organizer login for the export and the search |
| `CONFERENCE_CONFIG_PATH` | `classpath:conference.json` | options and consents, schema `docs/02_contracts/conference-config.schema.json`; an invalid file stops the start |
| `JSON_COPY_DIR` | `./data/json-copies` | persistent storage in production |
| `CAPTCHA_TEST_MODE` | `false` | `true` only locally and in tests: token `test-pass` |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty (secrets) | required in production |
| `RECAPTCHA_VERIFY_URL` | Google siteverify | |
| `ORGANIZER_HTTPS_ONLY` | `true` | must stay `true` outside local plain HTTP (SR-06) |
| `ALLOWED_ORIGIN` | empty | CORS for the Vite dev server only |
| `RATE_LIMIT_PER_MINUTE`, `REQUEST_MAX_BYTES` | `60`, `16384` | SR-03 |

## Commands (ES-05)

| Purpose | Command |
|---|---|
| build | `./mvnw -DskipTests package` (jar in `target/`) |
| test | `./mvnw verify` (unit, architecture and acceptance tests; JaCoCo report in `target/site/jacoco/`) |
| check | `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` (format with `./mvnw spotless:apply`) |
| run | `./mvnw spring-boot:run` with the settings above, for example `POSTGRES_PASSWORD=… ORGANIZER_USERNAME=… ORGANIZER_PASSWORD=… ORGANIZER_EMAILS=… CAPTCHA_TEST_MODE=true ./mvnw spring-boot:run` |
| mutation | `./mvnw test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage` (25 to 50 minutes; report in `target/pit-reports/`) |
| dependency scan | `NVD_API_KEY=… ./mvnw org.owasp:dependency-check-maven:12.1.0:check` (online; suppressions in `dependency-check-suppressions.xml`) |
| container | `docker build -t registration-backend .` (multi-stage, runs as uid 10001) |

## Troubleshooting

- `Could not resolve placeholder 'POSTGRES_PASSWORD'` (or an organizer setting): the secret is not set in the environment.
- `Invalid conference configuration: …` at start: the options file does not match its schema; the message names the entry.
- `Refusing to start in production: …`: a setting listed in the message is unsafe for `APP_ENVIRONMENT=production`.
- Tests fail with Docker errors: start Docker; Testcontainers needs it.
- `GET /api/export` or `GET /api/registrations` answers 403 with valid credentials: `ORGANIZER_HTTPS_ONLY=true` and the request is plain HTTP from a non-local address; use HTTPS through the proxy.
- `GET /api/registrations` answers 400: a parameter is invalid (`type` other than `EXTERNAL`/`STUDENT`, an unknown `option`, `page` below 1 or not a number, `text` over 100 characters); the body names it.
- The search does not find `Č`/`č` in either case: the database must use a UTF-8 locale (the pinned PostgreSQL image does); `lower()` folds non-ASCII letters only there.
- 429 responses: more than `RATE_LIMIT_PER_MINUTE` requests per minute from one client to one endpoint.
