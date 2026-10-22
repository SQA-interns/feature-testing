# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API for the conference registration: form configuration, registrations (stored in PostgreSQL and as JSON copies, then emailed), organizer search and Excel export. Contract: `../docs/02_contracts/openapi.json`. Packages (ports and adapters, checked by ArchUnit): `domain`, `application`, `adapter.{persistence,jsoncopy,mail,captcha,export,web}`, `config`.

## Prerequisites

- JDK 21 (`java -version`); Maven is not needed, the wrapper `./mvnw` downloads Maven 3.9.9.
- Docker Engine for the tests (Testcontainers starts PostgreSQL and Mailpit) and for the container image.
- For running outside Docker: a PostgreSQL 16 database and an SMTP server.

## Configuration

Every setting is an environment variable (ES-01). Secrets (from `.env`, see `01_input/01_project/00_setup/secrets.env.example`) have no default.

| Variable | Default | Meaning |
|---|---|---|
| `APP_ENVIRONMENT` | `production` | `local`, `test` or `production`; production refuses unsafe settings at start-up |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | none | JDBC URL and user, e.g. `jdbc:postgresql://db:5432/registration` |
| `POSTGRES_PASSWORD` | none (secret) | database password |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS` | none, `587`, `true` | mail server; STARTTLS required when `SMTP_TLS=true` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | empty (secret) | SMTP login, only when the server needs one |
| `MAIL_FROM` | `registration@konferenca.si` | sender address |
| `CONFERENCE_NAME` | `Conference` | name used in the emails |
| `CONFERENCE_CONFIG` | none (required) | options and consents file: `classpath:conference.json` (shipped) or `file:/path/conference.json`; schema `../docs/02_contracts/conference-config.schema.json`; the backend refuses to start with an invalid file |
| `JSON_COPY_DIR` | `/data/json-copies` | directory of the raw JSON copies (persistent volume) |
| `CAPTCHA_TEST_MODE` | `false` | `true` accepts only the token `test-pass` and never calls Google; refused in production |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty (secret) | reCAPTCHA v2 keys; required in production |
| `RECAPTCHA_VERIFY_URL` | Google siteverify | changed only by tests |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none (secret) | the organizer login (HTTP Basic); the password is kept only as a BCrypt hash |
| `ORGANIZER_EMAILS` | none (secret) | comma-separated notification addresses; each gets its own message |
| `ORGANIZER_HTTPS_ONLY` | `true` | refuse organizer requests over plain HTTP from non-local clients (403); must stay `true` in production |
| `CORS_ALLOWED_ORIGIN` | empty | one allowed origin for local development only; refused in production |
| `RATE_LIMIT_PER_MINUTE` | `60` | requests per minute per client and endpoint on `/api` (then 429) |
| `MAX_REQUEST_BYTES` | `16384` | request body limit (then 413) |

To change the programme, edit a copy of `src/main/resources/conference.json`, point `CONFERENCE_CONFIG` at it and restart; no code change.

## Build

```sh
./mvnw -B package -DskipTests          # target/registration-backend-0.1.0.jar
```

## Test

```sh
./mvnw -B verify                       # 227 tests: unit, architecture, acceptance (needs Docker); JaCoCo report in target/site/jacoco
./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage   # mutation testing, report in target/pit-reports (about 25 minutes)
```

## Check

```sh
./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check   # format, lint, duplication, static analysis
./mvnw -B dependency-check:check       # dependency scan; export NVD_API_KEY from .env first
```

`./mvnw spotless:apply` formats the code (google-java-format).

## Run

In the local stack: see `../README.md`. Alone (with your own PostgreSQL and SMTP):

```sh
export APP_ENVIRONMENT=local CONFERENCE_CONFIG=classpath:conference.json CAPTCHA_TEST_MODE=true \
       SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/registration SPRING_DATASOURCE_USERNAME=registration \
       SMTP_HOST=127.0.0.1 SMTP_PORT=1025 SMTP_TLS=false JSON_COPY_DIR=./data/json-copies
# POSTGRES_PASSWORD, ORGANIZER_USERNAME, ORGANIZER_PASSWORD, ORGANIZER_EMAILS from .env
java -jar target/registration-backend-0.1.0.jar
```

The API listens on port 8080; health at `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` (database and JSON copy directory). The container image (`Dockerfile`) runs as user `app` and expects the jar in `target/`.

## Production behind nginx (SR-04, SR-06, SR-08)

The external nginx terminates TLS, serves `/` from the frontend container and forwards `/api` to the backend. It must set the upstream `Host` itself and pass the client address and scheme, so that rate limiting and the HTTPS-only check see the real client:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host backend;                 # never forward the client's Host
    proxy_set_header X-Forwarded-For $remote_addr; # replaces any client-sent value
    proxy_set_header X-Forwarded-Proto $scheme;    # https
    client_max_body_size 32k;
}
```

The backend trusts `X-Forwarded-*` only from loopback or private addresses, so it must not be reachable from the internet except through this proxy.

## Troubleshooting

| Symptom | Cause and remedy |
|---|---|
| Start-up fails with `Invalid configuration: …` | a required or unsafe setting; the message names it (never its value) |
| Start-up fails with an `InvalidConfigurationException` | the options file breaks the schema (duplicate id, unknown category, missing name, …) |
| Readiness `DOWN` | database unreachable or `JSON_COPY_DIR` not writable |
| Registrations answer 500 | the JSON copy could not be written; nothing was stored (AR-05) |
| Organizer requests answer 403 `protocol` | `ORGANIZER_HTTPS_ONLY=true` and the request came over plain HTTP from a non-local client |
| 429 | the rate limit; raise `RATE_LIMIT_PER_MINUTE` for load tests |
| Tests fail at start with Docker errors | Docker is not running; the acceptance tests need it |
