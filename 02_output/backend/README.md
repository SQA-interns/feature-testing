# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

Spring Boot REST API of the conference registration (contract: [`../docs/02_contracts/openapi.yaml`](../docs/02_contracts/openapi.yaml)):

- `GET /api/form-config`: the active options, consents and captcha mode.
- `POST /api/registrations`: validates, verifies the captcha, stores the registration in PostgreSQL and as a JSON copy, then sends the emails.
- `GET /api/registrations/export`: the Excel workbook, for the organizer only (HTTP Basic).
- `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`: health and readiness.

The code follows ports and adapters under `si.konferenca.registration` (`domain`, `application`, `config`, `adapter.*`, `web`); ArchUnit tests enforce the rules of specification S2.

## Prerequisites

- JDK 21. Maven comes through the wrapper `./mvnw` (Maven 3.9.9, downloaded on first use).
- Docker for the tests (Testcontainers starts PostgreSQL and Mailpit) and for the local stack. Run the tests as a normal user, not as root: one test makes a directory read-only.

## Configuration

Every setting is an environment variable (specification S7.1). Secrets have no default; the local stack takes them from `.env` in the repository root (template: `01_input/01_project/00_setup/secrets.env.example`), and `../docker-compose.yml` sets the rest.

| Variable | Meaning | Default | Source |
|---|---|---|---|
| `APP_ENVIRONMENT` | `local`, `test` or `production` | required | compose: `local` |
| `DATABASE_URL`, `DATABASE_USERNAME` | PostgreSQL JDBC URL and user | required | compose |
| `POSTGRES_PASSWORD` | Database password | required | `.env` (secret) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS` | Mail server; STARTTLS required when `true` | required | compose: Mailpit |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Mail account, optional | empty | `.env` in production (secret) |
| `MAIL_FROM` | Sender address | `registration@localhost` | compose |
| `CONFERENCE_NAME` | Shown in the form and the emails | `Conference` | compose |
| `CONFERENCE_CONFIG_FILE` | Options and consents file | required | compose mounts `../config/conference.json` |
| `JSON_COPY_DIR` | Directory for the JSON copies, on a persistent volume | `/data/json-copies` | compose volume `json-copies` |
| `RECAPTCHA_TEST_MODE` | Deterministic test mode (token `test-pass`) | `false` | compose: `true` |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | Google reCAPTCHA v2 keys | empty | `.env` in production (secret) |
| `RECAPTCHA_VERIFY_URL` | Verification endpoint | Google siteverify | default |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | Organizer login for the export | required | `.env` (secret) |
| `ORGANIZER_EMAILS` | Notification recipients, comma separated | required | `.env` |
| `ORGANIZER_HTTPS_ONLY` | Refuse the export over plain HTTP except from localhost (403) | `true` | compose: `false` (local plain HTTP only) |
| `CORS_ALLOWED_ORIGIN` | Origin allowed to call the API from another host | empty (no CORS) | not set |
| `RATE_LIMIT_FORM_CONFIG_PER_MINUTE`, `RATE_LIMIT_REGISTRATION_PER_MINUTE`, `RATE_LIMIT_EXPORT_PER_MINUTE` | Requests per client and minute | 60, 10, 10 | default |
| `MAX_REQUEST_BYTES` | Request body limit | 16384 | default |

With `APP_ENVIRONMENT=production` the backend refuses to start when reCAPTCHA test mode is on or a key is empty (SR-02). It also refuses an unknown environment, an invalid conference file, and missing organizer settings.

### Conference options and consents

`CONFERENCE_CONFIG_FILE` names a JSON file in the format of [`../docs/02_contracts/conference-config.schema.json`](../docs/02_contracts/conference-config.schema.json). Each option has an id, a name, a category (`WORKSHOP`, `EVENT`, `MEAL`, `OTHER`), an `active` flag and an optional `availableTo` (default: both types). Each consent has an id, a text and a `mandatory` flag. Edit the file and restart the backend; an invalid file stops the startup with a message that names the problem. The shipped file is [`../config/conference.json`](../config/conference.json).

## Commands

Run from this folder.

| Purpose | Command |
|---|---|
| Build (jar in `target/`) | `./mvnw -B package` (add `-DskipTests` to skip the tests) |
| Test (all levels; needs Docker) | `./mvnw -B test` |
| Check (format, lint, static analysis) | `./mvnw -B spotless:check pmd:check compile spotbugs:check` |
| Format the code | `./mvnw -B spotless:apply` |
| Run in the local stack | from `02_output/`: `docker compose --env-file ../.env up -d --build --wait backend` (needs the jar from the build) |
| Run on the host | `./mvnw spring-boot:run` with the variables above exported and a reachable PostgreSQL and SMTP server |
| Coverage (JaCoCo, `target/site/jacoco/`) | `./mvnw -B clean verify` |
| Mutation testing (PIT, `target/pit-reports/`) | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` (about 25 minutes) |
| Dependency scan | `NVD_API_KEY=… ./mvnw -B org.owasp:dependency-check-maven:check` |

## Troubleshooting

- **Tests fail with "Could not find a valid Docker environment".** Start Docker. Testcontainers finds Docker Desktop and the default socket by itself.
- **AC-005-03 fails when run as root.** The test makes a directory read-only, and root can still write to it. Run the tests as a normal user.
- **The backend container stays `unhealthy`.** Check `docker compose logs backend`. Common causes are a wrong `POSTGRES_PASSWORD` for an existing `pgdata` volume, an invalid conference file, or missing organizer variables.
- **"production needs RECAPTCHA_TEST_MODE=false and both reCAPTCHA keys".** Set the real keys and turn test mode off. This is the SR-02 startup check.
- **The export answers 403 "Organizer access requires HTTPS."** The request came over plain HTTP from another host. Use HTTPS through the reverse proxy, or set `ORGANIZER_HTTPS_ONLY=false` only for local plain HTTP.
- **429 "Too many requests".** The per-client limit was reached. Wait for the `Retry-After` seconds.
