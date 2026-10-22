# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API of the conference registration (`/api`, contract in [`../docs/02_contracts/openapi.json`](../docs/02_contracts/openapi.json)). Root package `si.konferenca.registration`: `domain` (rules), `application` (use cases and ports), `infrastructure` (database, JSON copies, mail, reCAPTCHA, Excel, configuration), `web` (controllers), `security` (organizer access, HTTPS-only, rate and size limits).

## Prerequisites

- JDK 21 (the Maven wrapper downloads Maven 3.9.9; nothing else to install)
- Docker, for the tests (Testcontainers starts PostgreSQL and Mailpit) and for the container image

## Commands (ES-05)

| Purpose | Command |
|---|---|
| build | `./mvnw -B package -DskipTests` (jar in `target/`) |
| test | `./mvnw -B verify` (all tests; coverage report in `target/site/jacoco/`) |
| check | `./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check` |
| run (local stack) | `docker compose --env-file ../.env up -d --build` in `02_output/` |
| run (jar, against your own PostgreSQL and SMTP) | set the settings below as environment variables, then `java -jar target/registration-backend-1.0.0.jar` |
| mutation score | `./mvnw -B test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage` (report in `target/pit-reports/`, about 30 minutes) |
| dependency scan | `NVD_API_KEY=… ./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` |

On Windows use `mvnw.cmd`.

## Configuration

Every environment-specific value is a setting (environment variable); secrets have no default.

| Setting | Default | Source / notes |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | `jdbc:postgresql://localhost:5432/registration`, `registration` | compose sets them |
| `SPRING_DATASOURCE_PASSWORD` | none | secret `POSTGRES_PASSWORD` from `.env` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | `localhost`, `1025` | SMTP server; compose uses Mailpit |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SMTP_STARTTLS` | empty, empty, `false` | production SMTP account (secrets `SMTP_USERNAME`, `SMTP_PASSWORD`) |
| `MAIL_FROM`, `CONFERENCE_NAME` | `registration@konferenca.si`, `Conference` | sender and conference name in emails |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none | secrets from `.env`; the password is kept only as a BCrypt hash |
| `ORGANIZER_EMAILS` | empty | comma-separated notification recipients |
| `CONFERENCE_CONFIG_FILE` | shipped `conference-config.json` | options and consents (schema [`../docs/02_contracts/conference-config.schema.json`](../docs/02_contracts/conference-config.schema.json)); required in production |
| `JSON_COPY_DIR` | `./data/json-copies` | compose: `/data/json-copies` on the volume `jsoncopies` |
| `CAPTCHA_TEST_MODE` | `false` | `true` accepts only the token `test-pass` (local and test only) |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty | required unless test mode |
| `RECAPTCHA_VERIFY_URL` | Google siteverify URL | |
| `ORGANIZER_HTTPS_ONLY` | `true` | organizer requests over plain HTTP from other hosts get 403 |
| `TRUSTED_PROXIES` | none | regular expression of proxy IP addresses whose `X-Forwarded-For`/`-Proto` are trusted |
| `RATE_LIMIT_PER_MINUTE`, `REQUEST_MAX_BYTES` | `60`, `16384` | per client and endpoint; request body limit |
| `CORS_ALLOWED_ORIGIN` | empty | local development only |
| `APP_ENVIRONMENT` | `local` | `production` refuses to start with test mode, plain-HTTP organizer access, CORS or no options file |

Changing the options: edit the configuration file (or point `CONFERENCE_CONFIG_FILE` to another one) and restart; an invalid file stops the start with a message naming the problem.

## Production reverse proxy (SR-08)

The backend container listens on HTTP 8080 behind an nginx that terminates TLS. The proxy sets the upstream `Host` itself and the backend trusts only its address:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host backend:8080;          # never $host or $http_host
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Start the backend with `APP_ENVIRONMENT=production`, `TRUSTED_PROXIES=<nginx IP as regex>`, real reCAPTCHA keys and SMTP settings.

## Health

`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` (readiness includes the database; a mail outage does not make the backend unready).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| start fails with "Could not resolve placeholder 'ORGANIZER_USERNAME'" | the organizer secrets are missing; set them (compose reads them from `.env`) |
| start fails with "Refusing to start: …" | the message names the unsafe setting (for example reCAPTCHA keys missing without test mode) |
| start fails with "Invalid conference configuration: …" | fix the named problem in the options file |
| tests fail with "Could not find a valid Docker environment" | start Docker; Testcontainers needs it |
| registrations answer 500 | the JSON copy directory is not writable; check `JSON_COPY_DIR` and the volume |
| 429 answers | over `RATE_LIMIT_PER_MINUTE` for one client and endpoint; raise it for load tests |
