# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Conference registration: a public form (external participants and students), storage in PostgreSQL plus a raw JSON copy, confirmation and organizer emails, and an organizer export and search. Criteria are referenced by ID and not repeated here.

## Traceability

| AC / SR / AR | Section |
|---|---|
| AC-001-01, AC-002-01 | 4.2, 5.1 |
| AC-001-02, AC-002-02, AC-001-03 | 4.2 (rules V-01, V-02) |
| AC-001-04, AC-002-03 | 4.2 (V-03) |
| AC-001-05, AC-002-04 | 4.2 (V-05) |
| AC-001-06, AC-002-05 | 4.2 (V-06) |
| AC-001-07, AC-002-06 | 4.2 (V-07), 6.3 |
| AC-001-08, AC-002-07 | 7.1 |
| AC-003-01 … AC-003-03 | 4.1 |
| AC-004-01 … AC-004-04 | 7.1, 7.3 |
| AC-005-01 … AC-005-04 | 5.1, 5.2, 5.3 |
| AC-006-01 … AC-006-03 | 6.1 |
| AC-007-01, AC-007-02 | 6.2 |
| AC-008-01 … AC-008-04 | 4.4, 8.2 |
| AC-009-01 … AC-009-11 | 4.3, 8.2 |
| AC-009-12, AC-009-13 | 7.2 |
| SR-01 | 4.2 (V-07), 6.3 |
| SR-02 | 6.3, 9 |
| SR-03 | 8.3 |
| SR-04 | 4.2 (V-05) |
| SR-05 | 6.1 |
| SR-06 | 8.2 |
| SR-07 | 4.4, 6.2 |
| SR-08 | 10 |
| SR-09 | 7.2 |
| SB-01 | 4.2, 4.3 |
| SB-02 | 8.1 |
| SB-03 | 8.1 |
| SB-04 | 10 |
| SB-05 | 5.1, 6.1, 7.2 |
| SB-06 | 8.3 |
| SB-07 | 8.4 |
| SB-08, SB-09 | 11 |
| SB-10 | 8.5 |
| SB-11 | 10 |
| SB-12, SB-13, SB-14 | 5.4 |
| NFR-01 | 5.3 |
| NFR-02 | 10 |
| NFR-03 | 7.1, 7.3 |
| NFR-04 | 8.6, 10 |
| AR-01 | 3, 7 |
| AR-02, AR-03 | 3 |
| AR-04 | 4.1 |
| AR-05 | 5.2 |
| AR-06 | 5.1 |
| AR-07 | 6.3, 7.1 |
| AR-09 | 2, `02_contracts/openapi.json` |
| ES-01, ES-02 | 9 |
| ES-08 | 5.1 |
| ES-09 | 8.6 |

## 1. Components and interfaces

| Component | Folder | Technology (ids from `tech-stack.md`) | Runs as |
|---|---|---|---|
| backend | `02_output/backend` | java, spring-boot-starter-* , flyway, postgresql, poi-ooxml | `eclipse-temurin` JRE image, user `app` |
| frontend | `02_output/frontend` | react, vite, typescript | `node` build stage, static files served by `nginx` as user `nginx` |
| database | compose service `db` | `postgres` image | named volume `pgdata` |
| mail catcher | compose service `mailpit` | `axllent/mailpit` (local only) | |

| Interface | Contract |
|---|---|
| REST API (`/api`) | `02_contracts/openapi.json` (exactly `api-contract.md`) |
| JSON copy file | `02_contracts/registration-copy.schema.json` |
| Conference options and consents | `02_contracts/conference-config.schema.json` |
| Registration storage (SQL) | `02_contracts/database.md` |
| Emails (SMTP) | `02_contracts/emails.md` |
| reCAPTCHA (widget and verify call) | `02_contracts/recaptcha.md` |
| Registration form and organizer page (UI) | `02_contracts/ui.md` |

Contracts are validated by `02_contracts/validate-contracts.mjs` (JSON parser, local `$ref` resolution, and a comparison of paths, methods, status codes and fields against `api-contract.md`).

## 2. REST API

Exactly as `api-contract.md` (AR-09); the agent's choices where it is silent:

| Topic | Choice |
|---|---|
| Registration id | UUID (string) |
| `GET /api/form-config` extra field | `captchaTestMode` (boolean), so the frontend knows to send `test-pass` instead of rendering the widget |
| Error body for 401, 403, 413, 415, 429, 500 | same shape `{"errors":[{"field":…,"message":…}]}` with `field` `credentials`, `protocol`, `body`, `request` or `server`; never registration data or internals |
| Malformed JSON or wrong field type | 400 with `field` `body` |
| Unknown `type` in a registration | 400 with `field` `type` |
| Search page beyond the last page | 200, `items` empty, `total` and `page` as requested |
| Search `option` | any configured option id, active or inactive (`01_requirements-check.md` G-04); unknown id → 400 `option` |
| Search `type` | exactly `EXTERNAL` or `STUDENT` (case-sensitive), else 400 `type` |
| Search `page` | integer ≥ 1, else (including non-numbers) 400 `page` |
| Search `text` | trimmed; more than 100 characters (before trimming) → 400 `text` |
| Export row order | registration time ascending, then id |
| Organizer over plain HTTP from a non-local client | 403, `field` `protocol` (D-11) |

## 3. Backend architecture (AR-02, AR-03)

Ports and adapters under `si.konferenca.registration`:

| Package | Contains | May depend on |
|---|---|---|
| `domain` | registration model, types, option catalogue, validation rules, search criteria; plain Java | nothing else in the project (no Spring, no Jakarta Persistence) |
| `application` | use cases (`RegisterParticipant`, `SearchRegistrations`, `ExportRegistrations`, `FormConfiguration`) and the ports they need (`RegistrationStore`, `JsonCopyStore`, `CaptchaVerifier`, `Notifier`, `WorkbookWriter`) | `domain` |
| `adapter.persistence` | JPA entities, Spring Data repositories, `RegistrationStore` implementation | `application`, `domain` |
| `adapter.jsoncopy` | file `JsonCopyStore` | `application`, `domain` |
| `adapter.mail` | `Notifier` over Spring Mail | `application`, `domain` |
| `adapter.captcha` | Google verify client and test-mode verifier | `application`, `domain` |
| `adapter.export` | Apache POI `WorkbookWriter` | `application`, `domain` |
| `adapter.web` | controllers, request and response DTOs, error mapping, servlet filters (rate limit, body size, HTTPS-only) | `application`, `domain` |
| `config` | Spring wiring, settings, startup checks, security configuration, options loading | everything |

Justification: the rules (validation, catalogue, search criteria) are the part most worth mutation testing and are kept free of frameworks; each external system sits behind one port, so it can be replaced by a test double in acceptance tests (mail failure, JSON copy failure, captcha). Adapters do not depend on each other. The ArchUnit rules check exactly this table plus no package cycles (slices `si.konferenca.registration.(*)..` and `adapter.(*)..`).

## 4. Business behaviour

### 4.1 Conference configuration (US-003, AR-04, BR-04, OQ-01, OQ-02)

- One JSON file (schema `conference-config.schema.json`) holds the options and consents. Its location is the setting `CONFERENCE_CONFIG` (Spring resource: `classpath:conference.json` or `file:/path`); the shipped `classpath:conference.json` holds the programme and the consent `privacy` with the wording of OQ-02.
- Loaded once at startup. Start-up fails (the application refuses to start) when: the file is missing or not valid JSON; an option or consent id is duplicate, empty or not `[a-z0-9-]{1,64}`; a category is not one of `WORKSHOP`, `EVENT`, `MEAL`, `OTHER`; a name or consent text is missing or blank; `availableTo` contains an unknown type or is empty; `active` or `mandatory` is not a boolean. A missing `availableTo` means both types (OQ-01).
- `GET /api/form-config` lists only active options in file order, with `id`, `name`, `category`, `availableTo`; all consents; `captchaSiteKey` and `captchaTestMode`.
- Changing the programme means editing the file (or pointing `CONFERENCE_CONFIG` at another file) and restarting; the fixed participant fields never come from it (BR-01).

### 4.2 Registration (US-001, US-002, SB-01)

Validation on the backend, all errors of one request collected (one entry per field):

| Rule | Behaviour |
|---|---|
| V-01 | Every text field is trimmed (leading and trailing Unicode whitespace) before validation and storage (AC-001-03). |
| V-02 | Required fields per type (BR-01): both types `firstName`, `lastName`, `email`; `EXTERNAL` `organization`; `STUDENT` `studyInstitution`, `studyProgramme`, `studentId`. Missing, empty or blank → error for the field. Fields of the other type are ignored and stored as null. Maximum lengths: names 100, `organization` and `studyInstitution` 200, `studyProgramme` 200, `studentId` 50; longer → error. |
| V-03 | `email`: at most 254 characters, one `@`, a non-empty local part and a domain with a dot, no whitespace or control characters; otherwise error `email`. |
| V-04 | `type` missing or not `EXTERNAL`/`STUDENT` → error `type` (then only the common fields are checked). |
| V-05 | `options`: at most 50 entries; each must be an active configured option available to the type; a duplicate id → error `options` (SR-04, OQ-04). |
| V-06 | `consents`: every mandatory consent must be listed; unknown ids → error `consents` (BR-05). |
| V-07 | `captchaToken` missing or blank → error `captchaToken`; otherwise the verifier is asked (6.3) and a rejection is error `captchaToken`. The verifier is asked even when other fields fail, so one response reports everything. |

Text fields accept any Unicode text (BR-03), including line breaks, header-like text and markup (AC-006-02): they are stored as given (trimmed) and made harmless where they are output (6.1, 7.2). Only `email` is restricted (V-03). A valid request is stored (5.2), then emails are sent (6), then 201 `{"id"}` is returned.

### 4.3 Search (US-009, BR-09, BR-10)

- Parameters validated as in section 2. Matching: `text` (trimmed, lower-cased) is a substring of lower-cased `firstName`, `lastName`, `email`, `organization` or `studyInstitution`; `LIKE` wildcards in the text are escaped; empty text matches all. `type` and `option` filters are combined with AND.
- Order `registeredAt` descending, then `id` descending; page size 20; response `total`, `page`, `items` with the fields of `api-contract.md` (`options` as ids).
- Parameterised queries only (SB-05).

### 4.4 Export (US-008, BR-08, SR-07)

- `GET /api/export` returns an `.xlsx` (`Content-Disposition: attachment; filename="registrations.xlsx"`), first sheet `registrations`, header row and columns exactly as `api-contract.md`; every value is a string cell (never a formula), `registeredAt` as ISO 8601 UTC text, cells of the other type empty.
- Only the listed registration fields; no internal columns.

## 5. Storage

### 5.1 Database (AR-06, ES-08, SB-05)

Tables `registration`, `registration_option`, `registration_consent` created by Flyway `V1__registration.sql` (contract `database.md`); JPA with `spring.jpa.hibernate.ddl-auto=validate`. Each consent is stored with the time it was given (= registration time); `registered_at` is the server's time in UTC.

### 5.2 One unit of work (AR-05, BR-07, OQ-03)

1. Validate (4.2).
2. In one database transaction: insert the registration rows; write the JSON copy to `<JSON_COPY_DIR>/<id>.json.tmp`, flush and sync, atomically rename to `<id>.json`.
3. If writing the copy fails, the transaction rolls back and the API answers 500 (AC-005-03). If the commit fails after the copy was written, the copy is deleted.
4. Only after the commit are the emails sent (6). A mail failure is logged with the registration id only and does not change the 201 (AC-006-03).

### 5.3 JSON copy and Unicode (NFR-01)

The copy (schema `registration-copy.schema.json`) is the registration as accepted: `id`, `type`, the eight participant fields (null where not applicable), `options`, `consents` (`id`, `givenAt`), `registeredAt`. UTF-8 without escaping non-ASCII letters, so č, š, ž appear unchanged; the database uses UTF-8; emails use UTF-8; the workbook stores Unicode strings.

### 5.4 Personal data (SB-12 … SB-14)

Only the fields of BR-01 plus options, consents and their time are collected; purposes and retention as in `security-requirements.md` and OQ-06 (no automatic deletion; release notes tell the operator to delete rows and copies within 12 months after the conference). Consent checkboxes are never preselected (7.1).

## 6. External services

### 6.1 Participant email (US-006, SR-05)

Plain text (`text/plain; charset=UTF-8`), contract `emails.md`. Subject from configuration only (`Registration confirmed: <CONFERENCE_NAME>`); recipient the validated email; body names the conference, the participant and the selected options by display name. No user input goes into a header; markup is literal text in a plain text body. SMTP connect, read and write timeouts 5 s.

### 6.2 Organizer notification (US-007, SR-07)

One message per address in `ORGANIZER_EMAILS` (each organizer is the only recipient of their copy), plain text listing the registration's fields, with the attachment `registration-<id>.json` (`application/json`) whose bytes are the stored JSON copy.

### 6.3 reCAPTCHA (SR-01, SR-02, AR-07)

- Production path: `POST` form `secret`, `response` to `RECAPTCHA_VERIFY_URL` (default `https://www.google.com/recaptcha/api/siteverify`); accepted only when the JSON answer has `"success": true`; timeouts, errors and other answers reject. Tested against a mocked endpoint (DoD-P05).
- Test mode (`CAPTCHA_TEST_MODE=true`): only the token `test-pass` is accepted; no network call.
- The site key comes from `RECAPTCHA_SITE_KEY` through `GET /api/form-config`; the secret key never leaves the backend.

## 7. Frontend

Single page app, routes by path: `/` registration, `/organizer` organizer page; nginx falls back to `index.html`. Only `/api` is called (AR-01), same origin through the nginx proxy. Contract `ui.md`.

### 7.1 Registration page (AC-001-08, AC-002-07, AC-004-01, NFR-03)

- Loads `GET /api/form-config`; a choice "External participant" / "Student"; after the choice it shows exactly that type's fields, the active options available to the type in four groups (Workshops, Events, Meals, Other activities; empty groups hidden), every consent unchecked, and the captcha (widget or test-mode notice).
- Client validation mirrors V-01 … V-03 and mandatory consents; on error it shows each message next to its field and sends nothing (AC-004-03).
- On 201 it replaces the form with a confirmation showing the participant's name and the selected options' names (AC-004-01).

### 7.2 Organizer page (AC-009-12, AC-009-13, SR-09)

- A login form (username, password); credentials are kept in memory only and sent as HTTP Basic with `X-Requested-With: XMLHttpRequest` (so no browser login dialog appears).
- On success: search field, type filter (all / external / student), option filter (all configured options from the form configuration plus "all"), a result table, total count and previous/next page navigation. 401 → "Wrong username or password." and no data.
- All data is rendered as React text nodes (never `dangerouslySetInnerHTML`), so markup is literal (SR-09).

### 7.3 Errors (AC-004-02, AC-004-04, SB-07)

400 field errors are shown next to their fields and the entered values stay; any other failure or network error shows "Your registration could not be sent. Please try again later." without technical details.

## 8. Security

### 8.1 Organizer access (SB-02, SB-03, PA-04)

Spring Security HTTP Basic on `GET /api/export` and `GET /api/registrations`, one in-memory user from `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD`; the password is hashed with BCrypt at start-up and only the hash is kept. Everything else under `/api` is public; `/actuator/health/**` is public; all other paths are denied. Stateless, no session, CSRF off (no cookies are used).

### 8.2 HTTPS-only organizer access (SR-06, AC-008-04, AC-009-10)

With `ORGANIZER_HTTPS_ONLY=true` (default), a request to an organizer endpoint is answered 403 before credentials are checked unless it is secure or local:
- secure: the connection is TLS, or the direct peer is a trusted proxy (loopback or private address) and sent `X-Forwarded-Proto: https`;
- local: the client address is loopback, where the client address is the direct peer, or, when the peer is a trusted proxy that sent `X-Forwarded-For`, the last address in it.
Production refuses to start with `ORGANIZER_HTTPS_ONLY=false`.

### 8.3 Rate and size limits (SR-03, SB-06)

- Fixed one-minute window per client address (as in 8.2) and endpoint (method and path) for every `/api` endpoint; over `RATE_LIMIT_PER_MINUTE` (default 60) → 429. Failed organizer logins count against the same limit.
- Request bodies over `MAX_REQUEST_BYTES` (default 16384) → 413, checked from `Content-Length` and while reading.

### 8.4 Errors and logs (SB-07, ES-07)

One error handler maps exceptions to the error body of section 2; 500 says only "Internal error". Logs name registration ids, never names, emails or other personal data, tokens or credentials.

### 8.5 Headers (SB-10)

Backend: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx: CSP `default-src 'self'` allowing the reCAPTCHA script and frame origins (`https://www.google.com/recaptcha/`, `https://www.gstatic.com/recaptcha/`), `frame-ancestors 'none'`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`.

### 8.6 Health (ES-09, NFR-04)

Actuator `health` only, with liveness and readiness groups; readiness includes the database and a check that the JSON copy directory is writable; the mail health indicator is off (a mail outage must not make the service unready).

## 9. Configuration (ES-01, ES-02, SR-02)

| Setting | Default | Notes |
|---|---|---|
| `APP_ENVIRONMENT` | `production` | `local`, `test` or `production`; the unsafe settings below are refused in production |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | none | compose and tests set them |
| `POSTGRES_PASSWORD` | none (secret) | |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS`, `SMTP_USERNAME`, `SMTP_PASSWORD` | none, `587`, `true`, empty, empty | compose: Mailpit, TLS off |
| `MAIL_FROM` | `registration@konferenca.si` | |
| `CONFERENCE_NAME` | `Conference` | |
| `CONFERENCE_CONFIG` | none | compose and tests: `classpath:conference.json`; required |
| `JSON_COPY_DIR` | `/data/json-copies` | named volume `json-copies` |
| `CAPTCHA_TEST_MODE` | `false` | production refuses `true` |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty | production refuses empty |
| `RECAPTCHA_VERIFY_URL` | Google siteverify | overridden only in tests |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | none (secrets) | required in every environment |
| `ORGANIZER_HTTPS_ONLY` | `true` | production refuses `false`; compose default `false` (local plain HTTP) |
| `CORS_ALLOWED_ORIGIN` | empty (no CORS) | production refuses a value |
| `RATE_LIMIT_PER_MINUTE`, `MAX_REQUEST_BYTES` | `60`, `16384` | |

Secrets have no default in code or in committed files; `.env` is git-ignored (ES-02).

## 10. Deployment (NFR-02, NFR-04, SB-04, SB-11, SR-08)

`02_output/docker-compose.yml`: `db` (postgres, volume `pgdata`, `pg_isready` health check), `mailpit` (ports `127.0.0.1:8025`, `127.0.0.1:1025`), `backend` (image from `backend/Dockerfile` with the jar built by `./mvnw package`, port `127.0.0.1:8080`, volume `json-copies`, readiness health check, user `app`), `frontend` (multi-stage `frontend/Dockerfile`, nginx as user `nginx` on port 8080 published as `127.0.0.1:8081`, proxies `/api` to `backend:8080` with `Host` set to the upstream and `X-Forwarded-For` set to `$remote_addr`, health check). Secrets come from `.env` via `--env-file`; `CAPTCHA_TEST_MODE`, `RATE_LIMIT_PER_MINUTE` and `ORGANIZER_HTTPS_ONLY` are passed from the environment. Production: an external nginx terminates TLS (SB-04), sets `Host`, `X-Forwarded-For` and `X-Forwarded-Proto` itself (SR-08); the backend README gives the required proxy settings.

## 11. Quality and scanning (SB-08, SB-09)

Format (Spotless, Prettier), lint (PMD, ESLint), type check (`tsc`), static analysis (SpotBugs, PMD, Semgrep), duplication (CPD, jscpd), dependency scan (Dependency-Check, `npm audit`), secret scan (Gitleaks); coverage JaCoCo and V8; mutation PIT and Stryker, scopes as in `quality-requirements.md`.
