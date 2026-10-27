# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Contracts in `docs/02_contracts/`: `openapi.yaml` (REST), `registration-form-ui.md` (UI), `database-schema.sql` (SQL), `registration-copy.schema.json` (JSON copy), `conference-config.schema.json` (options and consents), `emails.md` (SMTP), `recaptcha-siteverify.schema.json` (Google verify). Validation: `out/logs/02_contract-validation.log`, `out/logs/02_contract-sql.log`. Decisions applied: D-11..D-14.

## Traceability

| AC / SR / SB / AR / NFR / BR | Section |
|---|---|
| AC-001-01, AC-002-01 | S3.2, S4.1 |
| AC-001-02, AC-002-02 | S4.2 (required, trimming) |
| AC-001-03 | S4.2 (trimming) |
| AC-001-04, AC-002-03 | S4.2 (email) |
| AC-001-05, AC-002-04 | S4.3 |
| AC-001-06, AC-002-05 | S4.4 |
| AC-001-07, AC-002-06 | S4.5 |
| AC-001-08, AC-002-07 | S6.1, `registration-form-ui.md` |
| AC-003-01 | S3.1, S5 |
| AC-003-02, AC-003-03 | S5 |
| AC-004-01..04 | S6.2 |
| AC-005-01 | S4.6, `database-schema.sql` |
| AC-005-02, AC-005-03 | S4.6 |
| AC-005-04 | S4.6, S9 |
| AC-006-01..03 | S4.7, `emails.md` |
| AC-007-01, AC-007-02 | S4.7, `emails.md` |
| AC-008-01, AC-008-03 | S4.8 |
| AC-008-02, AC-008-04 | S7.2 |
| SR-01, SR-02 | S4.5 |
| SR-03 | S7.3 |
| SR-04 | S4.3 |
| SR-05 | S4.7, `emails.md` |
| SR-06 | S7.2 |
| SR-07 | S4.7, S4.8 |
| SR-08 | S8 |
| SB-01 | S4 |
| SB-02, SB-03 | S7.2 |
| SB-04 | S8 |
| SB-05 | S4.6, S4.7, S4.8, S6.1 |
| SB-06 | S7.3 |
| SB-07 | S7.4 |
| SB-08, SB-09 | S10 |
| SB-10 | S7.5 |
| SB-11 | S8 |
| SB-12, SB-13, SB-14 | S9 |
| AR-01 | S1, S6 |
| AR-02, AR-03 | S2 |
| AR-04 | S5 |
| AR-05 | S4.6 |
| AR-06 | S4.6 |
| AR-07 | S4.5, S6 |
| NFR-01 | S9 |
| NFR-02 | S8 |
| NFR-03 | S6.2 |
| NFR-04 | S8 |
| BR-01..BR-05 | S4.2..S4.4 |
| BR-06 | S6.2 |
| BR-07 | S4.6 |
| BR-08 | S7.2 |
| DoD-P01..P04 | S8 (local stack), S10 |
| DoD-P05 | S4.5 |

## S1 Components

| Component | Folder | Role |
|---|---|---|
| backend | `02_output/backend` | Spring Boot REST API under `/api`, PostgreSQL via JPA, JSON copies on a volume, SMTP, Excel export |
| frontend | `02_output/frontend` | React single page served by nginx; talks to the backend only through `/api` (AR-01) |
| local stack | `02_output/docker-compose.yml` | backend, frontend, PostgreSQL, Mailpit (`environments.md`, local) |

## S2 Backend architecture (AR-02, AR-03)

Ports and adapters, so that the business rules are testable without Spring and every external system sits behind an interface. Packages under `si.konferenca.registration`:

| Package | Contains | May depend on |
|---|---|---|
| `domain` | Registration model, registration types, option and consent definitions, validation rules, field errors | JDK only |
| `application` | Use cases (`RegisterParticipant`, `GetFormConfiguration`, `ExportRegistrations`) and the ports they call (`RegistrationStore`, `JsonCopyStore`, `CaptchaVerifier`, `RegistrationNotifier`, `RegistrationWorkbook`) | `domain`; Spring `stereotype` and `transaction` annotations |
| `config` | Loading and checking the conference configuration and the typed application settings | `domain`, Spring |
| `adapter.persistence` | JPA entities, Spring Data repositories, `RegistrationStore` implementation | `application`, `domain`, JPA |
| `adapter.jsoncopy` | `JsonCopyStore` on the file system | `application`, `domain` |
| `adapter.captcha` | Google siteverify client and test-mode verifier | `application`, `config` |
| `adapter.mail` | `RegistrationNotifier` with Spring Mail | `application`, `domain`, `config` |
| `adapter.export` | `RegistrationWorkbook` with Apache POI | `application`, `domain` |
| `web` | Controllers, request and response DTOs, error mapping, security, rate-limit and size filters | `application`, `domain`, `config` |

ArchUnit rules, which check exactly this declaration:

1. `domain` depends on no other project package and on no `org.springframework..`, `jakarta.persistence..` or `org.apache.poi..` class.
2. `application` depends only on `domain` among project packages.
3. No `adapter..` package depends on `web`, and no adapter depends on another adapter.
4. `web` depends on no `adapter..` package.
5. No package cycles between the top-level slices of `si.konferenca.registration` (AR-03).

## S3 API (`openapi.yaml`)

### S3.1 `GET /api/form-config`

Returns the conference name, the captcha mode and site key, the active options (each with id, name, category and `availableTo`), and the consents. Inactive options are never included (AC-003-01).

### S3.2 `POST /api/registrations`

The JSON body follows `RegistrationRequest`. It is validated in full, and all field errors are returned together as `400 ValidationError`. On acceptance the response is `201` with a `Location` header and `RegistrationAccepted`, which holds the UUID id, time, type, names and selected options. The status and error shapes are the ones in the contract: 413 for a body over 16 KB, 415 when the body is not JSON, 429 for the rate limit, and 500 with a general message.

### S3.3 `GET /api/registrations/export`

HTTP Basic for the organizer role (S7.2). The response is an `.xlsx` (S4.8).

## S4 Registration processing (backend, SB-01)

Order in `RegisterParticipant`: validate fields, options and consents (S4.2..S4.4), then verify the captcha (S4.5), then store (S4.6), then notify (S4.7). The captcha is verified last, so an invalid form does not use up a token. Every check runs before anything is written. A rejected submission writes no row and no JSON copy, and sends no email.

### S4.1 Types (BR-01)

`EXTERNAL` takes first name, last name, email and organization. `STUDENT` takes first name, last name, email, study institution, study programme and student ID. A missing or unknown `type` gives the field error `type`/`REQUIRED`. A non-blank field of the other type gives `<field>`/`NOT_ALLOWED`.

### S4.2 Fields (BR-02, BR-03, D-14)

- Each text value is trimmed with `String.strip()` (Unicode whitespace) before any check, and only the trimmed value is stored (AC-001-03).
- A field that is missing, null, empty or blank after trimming gives `REQUIRED` (AC-001-02, AC-002-02).
- Lengths are counted in Unicode code points after trimming: 200 for names, organization, study institution and programme; 254 for email; 50 for student ID. A longer value gives `TOO_LONG`.
- Email: one address of the form `local@domain`. The local part is 1..64 characters from `A-Za-z0-9!#$%&'*+/=?^_\`{|}~.-`, with no leading, trailing or double dot. The domain has at least two labels of `A-Za-z0-9-`, does not start or end a label with `-`, and its last label is at least two letters. No spaces, CR, LF or commas are allowed. Anything else gives `INVALID_FORMAT` (AC-001-04, AC-002-03). The same rule runs in the frontend (S6.2).
- Text accepts any Unicode letters, including č, š, ž (BR-03). Control characters other than tab are rejected with `INVALID_FORMAT` in name, institution, programme and student ID fields.

### S4.3 Options (BR-04, SR-04, OQ-01, OQ-04)

`optionIds` may be absent or empty. Each id must exist in the configuration (`UNKNOWN_OPTION`), be active (`INACTIVE_OPTION`) and be available to the type (`OPTION_NOT_AVAILABLE`, applied to both types, G-01). An id listed twice gives `DUPLICATE_OPTION`. All option errors use the field `optionIds`. There is no limit per category.

### S4.4 Consents (BR-05, OQ-02)

`consents` lists the ids the participant gave. Every mandatory consent must be present, otherwise the error is `consents.<id>`/`CONSENT_REQUIRED`. An id that is not configured gives `consents.<id>`/`UNKNOWN_CONSENT`. Each given consent is stored with the registration time as its `givenAt`.

### S4.5 Captcha (SR-01, SR-02, AR-07, DoD-P05)

- `CaptchaVerifier` is always called on the backend. A missing or blank token, or a rejected one, gives `recaptchaToken`/`CAPTCHA_FAILED` (400, nothing stored; AC-001-07, AC-002-06).
- Production verifier: POST to `RECAPTCHA_VERIFY_URL` (default Google siteverify) with the secret and the token, with a 5 s connect and read timeout. The token is accepted only on HTTP 200 with `success: true` (`recaptcha-siteverify.schema.json`). Tests run this path against a mock HTTP server, including a rejected token and an error status.
- Test mode (`RECAPTCHA_TEST_MODE=true`): no network call. Token `test-pass` is accepted and anything else is rejected. The form configuration reports `captcha.mode = TEST`.
- At startup, `APP_ENVIRONMENT=production` with test mode on, or with an empty site or secret key, stops the application (SR-02). Test mode is off unless the variable sets it.
- The site key is the only key sent to the frontend. The secret stays in the backend (AR-07).

### S4.6 Storage (BR-07, AR-05, AR-06)

1. One database transaction inserts the `registration` row with its options (id, name and category as accepted) and consents (`database-schema.sql`), then flushes.
2. Still inside the transaction, the JSON copy (`registration-copy.schema.json`) is written as UTF-8 to `<JSON_COPY_DIR>/<id>.tmp`, forced to disk, and moved atomically to `<id>.json`.
3. If step 2 fails, the transaction rolls back and the API answers 500 with a general message. No row is left (AC-005-03). If the commit fails after step 2, the file is deleted.
4. Only after the commit does the use case return the accepted registration, so the 201 always means both copies exist (AR-05).

The schema is created only by Flyway migration `V1__create_registration.sql`, which equals `database-schema.sql`, with Hibernate `ddl-auto=validate` (AR-06, ES-08). Queries go through JPA, so they are parameterised (SB-05). Ids are random UUIDs. JSON is written by Jackson. The database uses UTF-8 encoding, so text is stored unchanged (AC-005-04).

### S4.7 Emails (US-006, US-007, SR-05, SR-07, OQ-03)

After the commit, the backend sends the participant confirmation and one notification per address in `ORGANIZER_EMAILS`, synchronously, with SMTP connect, read and write timeouts of 5 s. Each send is wrapped separately. A failure is logged with the registration id only, and the response is still 201 (AC-006-03). The content and the injection rules are in `emails.md`: plain text only, a fixed subject, the parsed single `To` address, and CR/LF in body values replaced. The attachment is the stored JSON copy byte for byte (AC-007-02).

### S4.8 Export (US-008, SR-07, AC-008-01, AC-008-03)

There is one sheet, `Registrations`, with a header row and one row per registration ordered by registration time. Columns: Registration ID, Type, Registered at (UTC, ISO 8601), First name, Last name, Email, Organization / institution, Study institution, Study programme, Student ID, Workshops, Events, Meals, Other activities (option names joined with `; `), and Consents (`<id> (<givenAt>)` joined with `; `). Every cell is written as a string cell (`setCellValue(String)`), never as a formula. Text therefore stays unchanged (AC-008-03), and a value such as `=1+1` is not evaluated. The workbook includes no internal field (SR-07). The file name is `registrations-<yyyy-MM-dd>.xlsx`.

## S5 Conference configuration (AR-04, US-003, OQ-01, OQ-02)

- The options and consents come from the JSON file named by `CONFERENCE_CONFIG_FILE`, which follows `conference-config.schema.json`. The conference name comes from `CONFERENCE_NAME`. The file is read once at startup, so changing the file and restarting changes the form with no code change (AC-003-02). The participant fields (BR-01) cannot be configured.
- At startup the file must parse and follow the schema, and every option id and every consent id must be unique. Otherwise the application stops with a message that names the problem and its position (AC-003-03).
- `availableTo` defaults to both types (OQ-01).
- The shipped file `02_output/config/conference.json` is the example in the schema (D-12): one active option per category for both types, `ws-industry` for external participants only, the inactive `ws-legacy`, and the `privacy` consent with the OQ-02 wording. Tests use their own files.

## S6 Frontend (`registration-form-ui.md`)

React 19 with TypeScript, built by Vite and served as static files. One page: `App` loads the form configuration, `TypeChooser` picks the type, `RegistrationForm` shows the fields, `OptionGroups`, `Consents` and `Captcha`, `Confirmation` shows the result, and `api.ts` is the only module that calls `fetch`, only on relative `/api/...` paths (AR-01). The frontend holds no secret. The reCAPTCHA site key comes from the form configuration (AR-07). React escapes rendered text (SB-05), and no `dangerouslySetInnerHTML` is used.

### S6.1 Form (AC-001-08, AC-002-07)

The form shows exactly the fields of the chosen type. It lists the active options for that type in the four category groups, all unchecked, and every consent unchecked.

### S6.2 Submission and messages (BR-06, NFR-03, AC-004-01..04)

- The frontend validates with the same rules as S4.2 to S4.4 (required, trim, email, lengths, mandatory consents, captcha token present). If anything fails, it shows field errors and sends nothing (AC-004-03).
- On 201 it replaces the form with the confirmation, using the name and options from the response (AC-004-01, BR-06).
- On 400 it shows each `fieldErrors` entry at its field and keeps the values (AC-004-02).
- On any other status or a network error it shows the general alert from the UI contract and keeps the values (AC-004-04).

## S7 Cross-cutting backend behaviour

### S7.1 Configuration (ES-01)

| Variable | Meaning | Default |
|---|---|---|
| `APP_ENVIRONMENT` | `local`, `test` or `production` | none (required) |
| `DATABASE_URL`, `DATABASE_USERNAME`, `POSTGRES_PASSWORD` | Database | none (compose sets local values) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS` | Mail server; STARTTLS required when true | none (compose sets Mailpit) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Mail account, optional | empty |
| `MAIL_FROM` | Sender address | `registration@localhost` |
| `CONFERENCE_NAME` | Shown in the form and emails | `Conference` |
| `CONFERENCE_CONFIG_FILE` | Options and consents file | none (compose mounts `config/conference.json`) |
| `JSON_COPY_DIR` | Directory for JSON copies on a persistent volume | `/data/json-copies` |
| `RECAPTCHA_TEST_MODE` | Test mode | `false` |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | Keys | empty; production refuses empty |
| `RECAPTCHA_VERIFY_URL` | Verify endpoint | `https://www.google.com/recaptcha/api/siteverify` |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | Organizer credentials | none (required) |
| `ORGANIZER_EMAILS` | Notification recipients, comma separated | none (required) |
| `ORGANIZER_HTTPS_ONLY` | SR-06 check | `true` |
| `CORS_ALLOWED_ORIGIN` | Allowed origin for the Vite dev server | empty (no CORS) |
| `RATE_LIMIT_FORM_CONFIG_PER_MINUTE`, `RATE_LIMIT_REGISTRATION_PER_MINUTE`, `RATE_LIMIT_EXPORT_PER_MINUTE` | Per-client limits | 60, 10, 10 |
| `MAX_REQUEST_BYTES` | Body limit | 16384 |

### S7.2 Organizer access (BR-08, SB-02, SB-03, SR-06, D-11)

- Spring Security, stateless, with no session and no cookies. CSRF protection is therefore off: no endpoint relies on ambient credentials. `/api/form-config` and `POST /api/registrations` are public. `GET /api/registrations/export` requires the role `ORGANIZER` through HTTP Basic. `/actuator/health/**` is public. Everything else is denied.
- At startup the password from `ORGANIZER_PASSWORD` is hashed with BCrypt (strength 12). Only the hash is kept in memory, and comparisons use the BCrypt encoder (SB-03).
- A missing or wrong credential gives 401 with `WWW-Authenticate: Basic realm="registration-export"` and an `Error` body with no data (AC-008-02).
- HTTPS-only check (SR-06, AC-008-04): when `ORGANIZER_HTTPS_ONLY=true` and a request to the export arrives that is not secure (`request.isSecure()` after the trusted `X-Forwarded-Proto`) from a client address that is not loopback, the backend answers 403 `Error` before it reads the credentials, with no redirect (D-11). Tomcat's `RemoteIpValve` (`server.forward-headers-strategy=native`) trusts forwarded headers only from the default internal proxy ranges.

### S7.3 Rate and size limits (SR-03, SB-06)

- A servlet filter before security gives each client address (after `RemoteIpValve`) a fixed-window counter per endpoint group: form configuration, registration and export, with the limits in S7.1. Requests over the limit get 429 with `Retry-After`. Failed and successful export logins count together, which limits password guessing. Counters live in memory, and stale entries are dropped every window.
- Bodies over `MAX_REQUEST_BYTES` get 413, from `Content-Length` or while streaming. The frontend nginx also sets `client_max_body_size 16k`.

### S7.4 Errors and logs (SB-07, ES-07)

A `@RestControllerAdvice` maps validation to 400 `ValidationError`, unreadable JSON to 400 with the field `type`/`REQUIRED`, wrong media type to 415, and anything else to 500 `{"message":"The request could not be processed."}`. Nothing else is shown: no stack trace and no exception text. Logs contain registration ids, never names, emails or request bodies. Spring's default error pages are disabled.

### S7.5 Security headers (SB-10)

- API responses (Spring Security): `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, and `Cache-Control: no-store` on the export.
- Frontend (nginx): `Content-Security-Policy: default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`, plus `X-Content-Type-Options`, `X-Frame-Options: DENY` and `Referrer-Policy: no-referrer`.

## S8 Deployment and runtime

- Backend image: a multi-stage build that packages with the Maven wrapper and runs on `eclipse-temurin:21.0.10_7-jre-alpine` as a non-root user (uid 10001); the configuration file is mounted read-only and only the JSON copy directory is writable (SB-11).
- Frontend image: builds in `node:24.13.0-alpine` and serves from `nginx:1.30.5-alpine` as non-root on port 8080. Its nginx serves `/` and proxies `/api/` to the backend. It sets `proxy_set_header Host` to the upstream itself and never forwards the client's Host (SR-08). It sets `X-Forwarded-For $remote_addr`, replacing rather than appending, and `X-Forwarded-Proto $scheme`.
- `docker-compose.yml` (local): `postgres:16.15-alpine`, `axllent/mailpit:v1.31.1`, backend and frontend. It publishes only `127.0.0.1:8080` (frontend) and `127.0.0.1:8025` (Mailpit UI and API), and uses named volumes `pgdata` and `json-copies` (NFR-02). It sets `APP_ENVIRONMENT=local`, `RECAPTCHA_TEST_MODE=true` and `ORGANIZER_HTTPS_ONLY=false` (allowed only on local plain HTTP, `environments.md`). Secrets come from `.env` (`--env-file`).
- Health (NFR-04, ES-09): Spring Actuator `/actuator/health/liveness` and `/actuator/health/readiness`, where readiness includes the database. Compose health checks use them for the backend, `pg_isready` for PostgreSQL and `wget` on `/` for the frontend. Dependent services wait for `service_healthy`.
- Production: external nginx terminates TLS (SB-04), serves `/` from the frontend container, forwards `/api` to the backend with its own `Host` header (SR-08), and sets `X-Forwarded-Proto https`.

## S9 Personal data and localisation (SB-12..14, NFR-01)

- Only the fields in `business-rules.md` "Data" and the consents with their time are collected. Their purposes and retention are as in `security-requirements.md` and OQ-06: kept until the operator deletes them, and the release notes ask for deletion within 12 months. Consents are never preselected, and a given consent is stored with its time (SB-14).
- UTF-8 is used end to end: in the HTML `meta charset`, in JSON over HTTP, in a PostgreSQL `UTF8` database, in JSON copies written as UTF-8, in `text/plain; charset=UTF-8` emails, and in POI string cells. No step normalises or transliterates text (NFR-01, AC-005-04, AC-008-03).

## S10 Quality and security tooling

Every tool is as listed in `tech-stack.md`. Backend checks: Spotless, PMD, SpotBugs, JaCoCo, PIT and Dependency-Check. Frontend checks: Prettier, ESLint, tsc, Vitest with v8 coverage, Stryker, jscpd and npm audit. Both components also run Semgrep and Gitleaks, and use Playwright for the end-to-end scenarios against the local stack (DoD-P01..P04). The levels of the acceptance tests are defined in phase 3 (`docs/03_test-strategy.md`).
