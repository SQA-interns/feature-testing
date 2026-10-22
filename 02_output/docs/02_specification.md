# Specification

> Written in: phase 2 · Source: `project/01_requirements/*`, `project/02_design/*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `general/security/*` · Agent: writes

Conference registration: a public registration form (external participants and students), storage in PostgreSQL plus a raw JSON copy, confirmation and organizer emails, an organizer Excel export and an organizer search page. The REST API is fixed by `api-contract.md` (AR-09) and described exactly by [`02_contracts/openapi.json`](02_contracts/openapi.json).

## 1. Components and runtime

| Component | Folder | Runs as | Talks to |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot jar in `eclipse-temurin` JRE image, non-root user, port 8080 | PostgreSQL (JDBC), SMTP, reCAPTCHA verify endpoint (HTTPS), JSON copy directory |
| frontend | `02_output/frontend` | React SPA built by Vite, served by `nginx` (non-root, port 8080), which also proxies `/api` to the backend | backend only through `/api` (AR-01) |
| postgres | image | `postgres` 16 with named volume `pgdata` | – |
| mailpit | image | local SMTP catcher (SMTP 1025, web/API 8025) | – |

Local stack: `02_output/docker-compose.yml`, started with `docker compose --env-file ../.env up -d --build`. Published on 127.0.0.1 only: backend `8080`, frontend `8081`, Mailpit `8025`. The compose network has a fixed subnet; the frontend nginx has the fixed address `172.28.0.10`, the backend's only trusted proxy (`TRUSTED_PROXIES`). The backend image is built in a `eclipse-temurin` JDK stage (D-12). Named volumes `pgdata` and `jsoncopies` keep data across container recreation (NFR-02). Production: the same two images behind an external nginx that terminates TLS, serves `/` from the frontend and forwards `/api` to the backend with a fixed upstream `Host` (SR-08); a sample is in the backend README.

## 2. Backend architecture (AR-02, AR-03)

Ports-and-adapters with four layers under `si.konferenca.registration`:

| Package | Contents | May depend on |
|---|---|---|
| `domain` | registration model, types, option and consent definitions, conference configuration model, validation rules, search criteria, field errors | `java.*` only |
| `application` | use cases (`RegistrationService`, `SearchService`, `ExportService`) and the ports they need (`RegistrationStore`, `JsonCopyStore`, `CaptchaVerifier`, `Notifier`, `WorkbookWriter`) | `domain`, Spring stereotypes and transactions |
| `infrastructure.*` | adapters implementing the ports: `persistence` (JPA), `jsoncopy` (files), `mail` (SMTP), `captcha` (reCAPTCHA), `excel` (POI), `config` (options file loading, settings, startup checks) | `application`, `domain`, frameworks |
| `web` | REST controllers, request/response DTOs, error mapping | `application`, `domain` |
| `security` | Spring Security configuration, organizer authentication, HTTPS-only, rate limit and body size filters (servlet filters ahead of Spring Security), headers, CORS | `domain`, `infrastructure.config` (settings), frameworks |

Justification: the business rules (validation, option availability, search semantics) are the code that must be mutation-tested most and change least; keeping them framework-free in `domain` makes them unit-testable and keeps the frameworks replaceable at the edges. `application` holds the one place where the storage order of BR-07/AR-05 is decided.

ArchUnit rules (written in phase 5, they check exactly this table):

1. `domain` depends only on `java..` classes.
2. `application` does not depend on `infrastructure..`, `web..` or `security..`.
3. `web` does not depend on `infrastructure..`.
4. `infrastructure..` does not depend on `web..` or `security..`.
5. No cycles between the slices `si.konferenca.registration.(*)..` (AR-03) and between the `infrastructure.(*)` sub-packages.
6. JPA annotations (`jakarta.persistence..`) appear only in `infrastructure.persistence`.

## 3. Conference configuration (AR-04, US-003, OQ-01, OQ-02)

A JSON file, schema [`02_contracts/conference-config.schema.json`](02_contracts/conference-config.schema.json). Path from setting `CONFERENCE_CONFIG_FILE`; when empty the backend uses the shipped `classpath:conference-config.json` (allowed in local and test only: with `APP_ENVIRONMENT=production` an empty path refuses start).

- `options[]`: `id` (pattern `^[a-z0-9][a-z0-9-]{0,63}$`), `name` (non-blank), `category` (`WORKSHOP`, `EVENT`, `MEAL`, `OTHER`), `active` (default `true`), `availableTo` (non-empty subset of `EXTERNAL`, `STUDENT`; default both).
- `consents[]`: `id` (same pattern), `text` (non-blank), `mandatory` (default `true`). The shipped file has the `privacy` consent with the wording of OQ-02.
- Loaded once at start. Invalid file (unparseable, duplicate option or consent id, unknown category or type, missing or blank name/text, bad id) → the application context fails and the backend refuses to start (AC-003-03). Unknown properties are rejected too.
- Changing the file and restarting changes the form configuration without code changes (AC-003-02).

## 4. REST API (AR-09)

Exactly `api-contract.md`; machine-readable in `openapi.json`. Additions allowed by the contract: `GET /api/form-config` adds `captchaTestMode` (boolean); error responses other than 400 carry `{"message": "<general text>"}`.

| Endpoint | Access | Behaviour |
|---|---|---|
| `GET /api/form-config` | public, rate limited | active options only (AC-003-01), consents, `captchaSiteKey` (from `RECAPTCHA_SITE_KEY`, may be empty in test mode), `captchaTestMode` |
| `POST /api/registrations` | public, rate limited, body ≤ `REQUEST_MAX_BYTES` | validation (§5) → captcha (§6) → store (§7) → emails (§8); 201 `{"id"}` |
| `GET /api/registrations` | organizer, rate limited | search (§9) |
| `GET /api/export` | organizer, rate limited | Excel workbook (§10) |

Status codes: 400 field errors `{"errors":[{"field","message"}]}` (also for malformed JSON: field `body`; wrong content type: 415 with `message`); 401 missing/wrong organizer credentials, body `{"message":"Authentication required"}`, `WWW-Authenticate: Basic realm="organizer"` unless the request carries `X-Requested-With` (so the organizer page gets no browser dialog); 403 organizer request over plain HTTP from a non-localhost client (D-10); 413 body too large; 429 over the rate limit with `Retry-After`; 500/503 `{"message":"…"}` without internals (SB-07) when storage or captcha verification fails.

## 5. Registration validation (BR-01..BR-05, SB-01, SR-04)

Server-side, in `domain`, collecting one error per invalid field (first problem per field):

| Field | Rule | Error field |
|---|---|---|
| `type` | required, `EXTERNAL` or `STUDENT` (unknown string → error, not a parse failure) | `type` |
| `firstName`, `lastName`, `email` | trimmed; non-empty; at most 200 characters (email 254) | same name |
| `email` | format `local@domain.tld`: no whitespace or control characters, exactly one `@`, a dot in the domain, no leading/trailing dot in the domain | `email` |
| `organization` | required for `EXTERNAL`, ignored (stored as null) for `STUDENT` | `organization` |
| `studyInstitution`, `studyProgramme`, `studentId` | required for `STUDENT`, ignored for `EXTERNAL` | same name |
| `options` | null treated as empty; each id is a configured, active option available to the type; no id twice; no null entry | `options` |
| `consents` | every mandatory consent present; unknown ids rejected; no id twice | `consents` |
| `captchaToken` | non-blank, then accepted by the verifier | `captchaToken` |

When the type is missing or unknown, only common fields are checked besides `type`. Validation runs before captcha verification; the captcha is verified only when every other field is valid, and a rejected token yields a `captchaToken` error. Text is stored trimmed (AC-001-03) and otherwise unchanged (Unicode, NFR-01). Duplicate emails are allowed (OQ-05).

## 6. reCAPTCHA (SR-01, SR-02, DoD-P05)

`CaptchaVerifier` port with two implementations chosen at start:

- Test mode (`CAPTCHA_TEST_MODE=true`): only the token `test-pass` is accepted; no network call.
- Production path: HTTPS POST (`application/x-www-form-urlencoded`, `secret`, `response`) to `RECAPTCHA_VERIFY_URL` (default `https://www.google.com/recaptcha/api/siteverify`) with 5 s timeouts; accepted only if the JSON answer has `"success": true`. A network or parse failure → 503, nothing stored. Tested against a mocked endpoint (accepting and rejecting).
- Test mode is off unless configured. With test mode off, an empty `RECAPTCHA_SECRET_KEY` or `RECAPTCHA_SITE_KEY` refuses start; with `APP_ENVIRONMENT=production`, test mode on refuses start.

Frontend: in test mode it sends `test-pass` without a widget and shows a "test mode" note; otherwise it loads the Google v2 widget with the site key and sends its token, and resets the widget after every response.

## 7. Storage (BR-07, AR-05, AR-06, ES-08, US-005)

Order inside `RegistrationService.register`, one database transaction:

1. Build the registration: new UUID, `registeredAt` = now truncated to milliseconds (UTC), each given consent with `givenAt` = `registeredAt`.
2. Insert the rows and flush.
3. Write the JSON copy `<id>.json` to `JSON_COPY_DIR`: written to a temporary file in the same directory, then atomically moved. Any failure throws → the transaction rolls back → 500, no row (AC-005-03).
4. Commit. If the commit fails, the JSON copy is deleted and the request fails.
5. Only then: 201 and emails (§8).

Schema (Flyway `V1__registration.sql`, AR-06): `registration` (`id uuid` PK, `seq bigserial` unique for stable newest-first order, `type`, `first_name`, `last_name`, `email`, `organization`, `study_institution`, `study_programme`, `student_id`, `search_text`, `registered_at timestamptz`), `registration_option` (`registration_id`, `position`, `option_id`), `registration_consent` (`registration_id`, `consent_id`, `given_at`). Text columns are `varchar` in a UTF-8 database. `search_text` holds the five searchable fields lower-cased with `Locale.ROOT` and joined by U+001F, so case-insensitive matching does not depend on the database collation (Slovenian characters, AC-009-11).

JSON copy: schema [`02_contracts/registration-copy.schema.json`](02_contracts/registration-copy.schema.json), UTF-8 without BOM: `id`, `type`, the eight participant fields (fields of the other type `null`), `options` (ids in submitted order), `consents` (`[{id, givenAt}]`), `registeredAt`.

## 8. Emails (US-006, US-007, SR-05, SR-07, OQ-03)

Sent synchronously after the commit through `Notifier` (Spring `JavaMailSender`; SMTP timeouts 5 s). Each send is caught separately: a failure is logged as `Email <kind> failed for registration <id>` with no personal data; the response stays 201 (AC-006-03).

- Participant: to the participant's address; subject `Registration confirmation: <CONFERENCE_NAME>`; `text/plain; charset=UTF-8` body naming the conference, the participant's name and the selected options by display name.
- Organizer: one message per address in `ORGANIZER_EMAILS`; subject `New registration <id>`; plain-text body with every submitted field, options, consents and time; attachment `registration-<id>.json` (`application/json`) whose bytes are exactly the JSON copy's bytes (AC-007-02). Only registration data (SR-07).
- Injection safety (AC-006-02): no user input in any header except the participant's validated address (no whitespace or control characters possible); plain-text bodies only, so markup stays literal.

Settings: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SMTP_STARTTLS`, `MAIL_FROM`, `CONFERENCE_NAME`, `ORGANIZER_EMAILS` (§13).

## 9. Search (US-009, BR-09, BR-10)

`GET /api/registrations?text&type&option&page`, organizer only.

- Validation (AC-009-08): `text` more than 100 characters, raw (D-11) → `text`; `type` not `EXTERNAL`/`STUDENT` → `type`; `option` not a configured option id (active or inactive) → `option`; `page` not an integer ≥ 1 → `page`. Empty parameters count as absent.
- Matching: text trimmed, lower-cased (`Locale.ROOT`), U+001F removed; `search_text LIKE %text%` with `\`, `%`, `_` escaped; empty text matches all. Type and option filters combine with AND (AC-009-05).
- Order `registered_at DESC, seq DESC`; page size 20; `total` = number of matches; a page past the end gives an empty `items`.
- Item fields: `id`, `type`, `firstName`, `lastName`, `email`, `organization`, `studyInstitution`, `options`, `registeredAt`.

## 10. Export (US-008, BR-08, SR-07)

`GET /api/export`, organizer only: XSSF workbook, sheet `Registrations`, header row exactly the contract's 12 columns, one row per registration ordered oldest first; all cells are string cells (no formulas, so cell text starting with `=` is not evaluated); `options`/`consents` joined with `, `; cells of the other type empty; `registeredAt` ISO 8601. `Content-Disposition: attachment; filename="registrations.xlsx"`.

## 11. Security (SB-01..SB-14, SR-01..SR-09)

- Organizer authentication (BR-08, BR-09, SB-02, SB-03): Spring Security HTTP Basic on `GET /api/registrations` and `GET /api/export`; one in-memory user from `ORGANIZER_USERNAME`/`ORGANIZER_PASSWORD`, password hashed with BCrypt at start and only the hash kept. Stateless, no session, CSRF off (no cookies, no ambient credentials). Everything else under `/api` is public; `/actuator/health/**` public; all other paths denied.
- HTTPS only (SR-06, `ORGANIZER_HTTPS_ONLY`, default `true`): an organizer request that is not secure and does not come from a loopback address is refused with 403 before authentication (D-10). Client address and scheme come from the connection, or from `X-Forwarded-For`/`X-Forwarded-Proto` only when the connection comes from a trusted proxy (`TRUSTED_PROXIES`, a regular expression of proxy IPs, default none: Tomcat `RemoteIpValve`). `APP_ENVIRONMENT=production` refuses start with `ORGANIZER_HTTPS_ONLY=false`.
- Rate limits (SR-03, SB-06): fixed one-minute window per client address and endpoint (method + path) for every `/api` endpoint; limit `RATE_LIMIT_PER_MINUTE` (default 60); over the limit → 429 with `Retry-After`. Request bodies over `REQUEST_MAX_BYTES` (default 16384) → 413 (by `Content-Length` and by counting a body without length).
- Headers (SB-10): backend sends `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. The frontend nginx sends a CSP allowing only self plus the Google reCAPTCHA script and frame origins, `frame-ancestors 'none'`, nosniff and `Referrer-Policy`.
- CORS: off unless `CORS_ALLOWED_ORIGIN` is set (local development only; production refuses start with it set).
- Output encoding (SB-05, SR-09): React renders participant data as text, never as HTML (`dangerouslySetInnerHTML` not used); JPA parameterised queries only; emails plain text; Excel string cells.
- Errors and logs (SB-07, ES-07): global handler maps every unexpected exception to a general message; logs carry registration ids, never personal data or secrets.
- Proxy `Host` (SR-08): the frontend nginx and the documented production nginx set the upstream `Host` themselves (`proxy_set_header Host backend:8080` / the upstream name), never `$host`/`$http_host`.
- Least privilege (SB-11): both images run as non-root users.
- Personal data (SB-12..SB-14): only the fields of BR-01's Data table, consents with time; retention per OQ-06 in the release notes; consents never preselected (BR-05).
- Dependencies and source scanning (SB-08, SB-09): Dependency-Check, `npm audit`, Semgrep, Gitleaks in phase 6.

## 12. Frontend (US-001, US-002, US-004, US-009, NFR-03)

UI contract: [`02_contracts/ui-contract.md`](02_contracts/ui-contract.md). Routes by path: `/` registration, `/organizer` organizer search page (nginx falls back to `index.html`).

- Registration page: loads `/api/form-config`; a choice "External participant" / "Student"; after the choice exactly that type's fields, the options available to the type grouped under Workshops, Events, Meals, Other activities (empty groups hidden), unchecked consent checkboxes, the captcha, a "Register" button.
- Client validation before any request (AC-004-03): the form has `noValidate` (no browser bubbles) and its own check on submit: required fields non-blank, email format, mandatory consents; errors next to their fields, nothing sent.
- Backend 400: each error next to its field; values kept (AC-004-02); errors for fields without an input (`type`, `captchaToken`, `body`) in the form's error area.
- 201: a confirmation with the participant's name and selected option names replaces the form (AC-004-01, BR-06).
- Any other status or network failure: a general message without technical details; no confirmation (AC-004-04).
- Organizer page: username/password form; credentials kept in memory only and sent as Basic with `X-Requested-With`; wrong credentials → error and no data; then a search field with a Search button, type filter (All, External, Student), option filter (All + configured options from form-config), a table of results, total, page navigation (Previous/Next, "Page n of m").

## 13. Configuration settings (ES-01)

| Setting | Default | Notes |
|---|---|---|
| `APP_ENVIRONMENT` | `local` | `production` enables the start-up refusals above |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | `jdbc:postgresql://localhost:5432/registration`, `registration`, none | Spring Boot's standard settings; defaults for local/test only; compose passes the secret `POSTGRES_PASSWORD` as `SPRING_DATASOURCE_PASSWORD` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SMTP_STARTTLS` | `localhost`, `1025`, empty, empty, `false` | Spring Boot's standard mail settings (local Mailpit defaults); compose passes `SMTP_USERNAME`/`SMTP_PASSWORD` in production |
| `MAIL_FROM`, `CONFERENCE_NAME` | `registration@konferenca.si`, `Conference` | |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | none | from `.env` |
| `CONFERENCE_CONFIG_FILE` | empty (shipped file) | required in production |
| `JSON_COPY_DIR` | `./data/json-copies` | compose: `/data/json-copies` on volume `jsoncopies` |
| `CAPTCHA_TEST_MODE` | `false` | compose sets `true` locally |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `RECAPTCHA_VERIFY_URL` | empty, empty, Google URL | |
| `ORGANIZER_HTTPS_ONLY` | `true` | compose sets `false` locally |
| `TRUSTED_PROXIES` | none | regex of proxy IPs |
| `CORS_ALLOWED_ORIGIN` | empty | local development only |
| `RATE_LIMIT_PER_MINUTE`, `REQUEST_MAX_BYTES` | `60`, `16384` | |

## 14. Health (NFR-04, ES-09)

Actuator `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` (readiness includes the database; the mail indicator is off so a mail outage does not make the backend unready). Compose health checks: backend readiness, PostgreSQL `pg_isready`, frontend `wget` of `/`; `depends_on` with `condition: service_healthy`.

## 15. Traceability

### Acceptance criteria

| AC | Spec | AC | Spec |
|---|---|---|---|
| AC-001-01 | §4, §5, §7 | AC-005-01 | §7 |
| AC-001-02 | §5 | AC-005-02 | §7 |
| AC-001-03 | §5, §7 | AC-005-03 | §7 |
| AC-001-04 | §5 | AC-005-04 | §7 |
| AC-001-05 | §5 (options) | AC-006-01 | §8 |
| AC-001-06 | §5 (consents) | AC-006-02 | §8 |
| AC-001-07 | §5, §6 | AC-006-03 | §8 |
| AC-001-08 | §12 | AC-007-01 | §8 |
| AC-002-01 | §4, §5, §7 | AC-007-02 | §7, §8 |
| AC-002-02 | §5 | AC-008-01 | §10 |
| AC-002-03 | §5 | AC-008-02 | §11 |
| AC-002-04 | §3, §5 | AC-008-03 | §7, §10 |
| AC-002-05 | §5 | AC-008-04 | §11 (HTTPS only) |
| AC-002-06 | §5, §6 | AC-009-01 | §9 |
| AC-002-07 | §12 | AC-009-02 | §9 |
| AC-003-01 | §3, §4 | AC-009-03..07 | §9 |
| AC-003-02 | §3 | AC-009-08 | §9 |
| AC-003-03 | §3 | AC-009-09 | §11 |
| AC-004-01 | §12 | AC-009-10 | §11 (HTTPS only) |
| AC-004-02 | §12 | AC-009-11 | §7 (`search_text`), §9 |
| AC-004-03 | §12 | AC-009-12 | §12 |
| AC-004-04 | §4, §12 | AC-009-13 | §11 (output encoding), §12 |

### Security, quality and architecture

| ID | Spec | ID | Spec |
|---|---|---|---|
| SR-01 | §6 | SB-01 | §5, §9 |
| SR-02 | §6 | SB-02 | §11 |
| SR-03 | §11 (rate limits) | SB-03 | §11 (BCrypt), §13 |
| SR-04 | §5 | SB-04 | §1 (TLS at nginx), §11 (HTTPS only) |
| SR-05 | §8 | SB-05 | §11 (output encoding) |
| SR-06 | §11 (HTTPS only) | SB-06 | §11 (rate limits) |
| SR-07 | §8, §10 | SB-07 | §4, §11 |
| SR-08 | §1, §11 | SB-08, SB-09 | §11 (scanning) |
| SR-09 | §11, §12 | SB-10 | §11 (headers) |
| NFR-01 | §5, §7, §8, §10 | SB-11 | §1, §11 |
| NFR-02 | §1 (named volumes) | SB-12..SB-14 | §11 (personal data) |
| NFR-03 | §12 | AR-01 | §1, §12 |
| NFR-04 | §14 | AR-02, AR-03 | §2 |
| AR-04 | §3 | AR-05 | §7 |
| AR-06 | §7 | AR-07 | §6, §12 (site key from form-config) |
| AR-09 | §4, `openapi.json` | ES-01, ES-09 | §13, §14 |

## 16. Contracts

| Interface | Contract | Validated by |
|---|---|---|
| REST API | [`02_contracts/openapi.json`](02_contracts/openapi.json) (OpenAPI 3.1) | `node 02_output/docs/02_contracts/validate.mjs` (JSON parse, `$ref` resolution, contract path/status check) |
| JSON copy | [`02_contracts/registration-copy.schema.json`](02_contracts/registration-copy.schema.json) (JSON Schema 2020-12) | same script |
| Conference configuration | [`02_contracts/conference-config.schema.json`](02_contracts/conference-config.schema.json) | same script; the shipped configuration is checked against it in tests |
| Emails | [`02_contracts/email-contract.md`](02_contracts/email-contract.md) | review |
| UI (registration form, organizer page) | [`02_contracts/ui-contract.md`](02_contracts/ui-contract.md) | review |
| Registration storage | §7 schema; Flyway migration | Flyway at start |
| Anti-automation | §6 | tests with mocked endpoint |
