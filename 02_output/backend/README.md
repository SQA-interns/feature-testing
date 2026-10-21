# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API under `/api` (`docs/02_contracts/openapi.json`): `GET /api/form-config`, `POST /api/registrations`, `GET /api/export` (organizer, HTTP Basic). Root package `si.konferenca.registration`, ports and adapters (`docs/02_specification.md` section 2).

## Prerequisites

- JDK 21 (the Maven wrapper downloads Maven 3.9.9; no Maven install needed)
- Docker Engine: the tests start PostgreSQL and Mailpit with Testcontainers; the image build needs it too
- For running outside Docker: a PostgreSQL 16 database and an SMTP server (locally Mailpit)

## Configuration

Every setting comes from the environment (ES-01); `docs/02_specification.md` section 3 has the full table.

| Variable | Purpose | Source / default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL | environment; defaults only in profiles `local`/`test`; password always from `.env` (`POSTGRES_PASSWORD`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS`, `SMTP_USERNAME`, `SMTP_PASSWORD` | SMTP | environment; local profile: `localhost:1025` without TLS; SMTP account from `.env` (production only) |
| `MAIL_FROM`, `CONFERENCE_NAME` | sender, name in emails | defaults `registration@konferenca.example`, `Conference` |
| `CONFERENCE_CONFIG` | options and consents file (`docs/02_contracts/conference-config.schema.json`), e.g. `file:/config/conference.json` | required in production; `classpath:conference-config.json` in profiles `local`/`test` |
| `JSON_COPY_DIR` | directory of the JSON copies (persistent volume) | `/data/json-copies` |
| `CAPTCHA_TEST_MODE` | `true` accepts only the token `test-pass` | `false`; `true` refused outside profiles `local`/`test` |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `RECAPTCHA_VERIFY_URL` | Google reCAPTCHA v2 | keys from `.env`, required without test mode; URL defaults to Google |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | organizer login and notification recipients | `.env`, required |
| `ORGANIZER_HTTPS_ONLY` | refuse the export over plain HTTP from non-loopback clients | `true`; `false` only in profiles `local`/`test` |
| `CORS_ALLOWED_ORIGIN` | one origin for cross-origin calls | empty; profile `local` only |
| `RATE_LIMIT_PER_MINUTE`, `MAX_REQUEST_BYTES` | per-client limit, body size limit | `30`, `16384` |
| `SPRING_PROFILES_ACTIVE` | `local` for the compose stack; none in production | — |

The backend refuses to start with an invalid options file or a setting that is allowed only locally (`StartupGuard`).

## Build, check, test, run

| Task | Command (Windows: `mvnw.cmd` instead of `./mvnw`) |
|---|---|
| build | `./mvnw -B -DskipTests package` → `target/registration-backend-1.0.0.jar` |
| check (format, lint, static analysis) | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` (format with `./mvnw spotless:apply`) |
| test (unit, integration, architecture, acceptance) with coverage | `./mvnw -B verify` → `target/site/jacoco/` |
| mutation testing | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` → `target/pit-reports/` |
| dependency scan | `NVD_API_KEY=… ./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` (key from `.env`) |
| run (with the compose stack) | from `02_output/`: `docker compose --env-file ../.env up -d --build backend` |
| run (on the host) | `SPRING_PROFILES_ACTIVE=local DB_PASSWORD=… ORGANIZER_USERNAME=… ORGANIZER_PASSWORD=… ORGANIZER_EMAILS=… CAPTCHA_TEST_MODE=true ORGANIZER_HTTPS_ONLY=false java -jar target/registration-backend-1.0.0.jar` (PostgreSQL on `localhost:5432`, Mailpit on `localhost:1025`) |

Health: `GET /actuator/health`, `/actuator/health/readiness`, `/actuator/health/liveness`.

## Troubleshooting

| Symptom | Cause and remedy |
|---|---|
| Tests fail with "Could not find a valid Docker environment" | Testcontainers needs a running Docker Engine. |
| Start fails with "Refusing to start: …" | A setting allowed only in profiles `local`/`test` is set, or a required secret is empty; the message names it. |
| Start fails with a `ConferenceConfigException` | `CONFERENCE_CONFIG` points to a missing or invalid file (duplicate id, unknown category, missing name). |
| `POST /api/registrations` answers 400 `body` | The body is not UTF-8 JSON; send it from a UTF-8 file (`--data-binary @file.json`) on Windows shells. |
| 429 Too Many Requests | `RATE_LIMIT_PER_MINUTE` per client and endpoint was exceeded; wait for the `Retry-After` seconds or raise the limit locally. |
| Export answers 403 "requires HTTPS" | `ORGANIZER_HTTPS_ONLY=true` and the request came over plain HTTP from another machine; use HTTPS through the reverse proxy. |
