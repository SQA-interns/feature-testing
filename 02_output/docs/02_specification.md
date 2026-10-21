# Specification

> Written in: phase 2 · Source: `project/01_requirements/*`, `project/02_design/*`, `project/00_setup/tech-stack.md`, `environments.md`, `general/security/*` · Agent: writes

Contracts: [`02_contracts/`](02_contracts/) (`openapi.yaml`, `database-schema.sql`, `registration-copy.schema.json`, `conference-config.schema.json`, `interfaces.md`).

## 1. Components

| Component | Folder | Runs as | Talks to |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot 4.1.1 jar in an `eclipse-temurin` JRE image, non-root user, port 8080 | PostgreSQL (JPA), SMTP, reCAPTCHA verify URL, JSON copy volume |
| frontend | `02_output/frontend` | React 19 single page built by Vite, served by `nginx` as a non-root user, port 8080 in the container | backend only via `/api` (AR-01); nginx proxies `/api/` to the backend with a fixed upstream `Host` (SR-08) |
| local stack | `02_output/docker-compose.yml` | `postgres`, `mailpit`, `backend`, `frontend`; ports bound to 127.0.0.1: backend 8080, frontend 8081, Mailpit 8025 | named volumes `pgdata`, `jsoncopies` (NFR-02) |

## 2. Backend architecture (AR-02 declaration)

Ports and adapters with three layers under `si.konferenca.registration`. Chosen because the core rule set (validation, option availability, consents, storage order) must be testable without Spring, PostgreSQL, SMTP or Google, and because each external system (database, file, mail, reCAPTCHA, Excel) is replaceable behind one interface.

| Package | Contents | May depend on |
|---|---|---|
| `domain` | `Registration`, `RegistrationType`, `ConferenceOption`, `OptionCategory`, `Consent`, `ConferenceConfig`, `RegistrationSubmission`, `RegistrationValidator`, `FieldError`, `ValidationException`; for US-009 `SearchCriteria`, `SearchCriteriaParser`, `RegistrationPage` | JDK only (no Spring, Jakarta, Jackson, POI) |
| `application` | `RegistrationService` (use case), `ExportService`, `SearchService` (US-009), ports `RegistrationRepository`, `RegistrationSearch` (US-009), `JsonCopyStore`, `CaptchaVerifier`, `RegistrationNotifier`, `RegistrationExporter`, `Clock` use | `domain`, Spring transaction annotations |
| `adapter.web` | controllers (incl. `SearchController`, US-009), request/response DTOs, `ApiExceptionHandler`, `RateLimitFilter`, `BodySizeLimitFilter`, `OrganizerTransportFilter` | `application`, `domain` |
| `adapter.persistence` | JPA entities, Spring Data repositories, `JpaRegistrationRepository` | `application`, `domain` |
| `adapter.jsoncopy` | `FileJsonCopyStore` | `application`, `domain` |
| `adapter.mail` | `MailRegistrationNotifier` | `application`, `domain` |
| `adapter.captcha` | `TestModeCaptchaVerifier`, `RecaptchaHttpVerifier` | `application`, `domain` |
| `adapter.export` | `ExcelRegistrationExporter` (POI) | `application`, `domain` |
| `config` | `@ConfigurationProperties`, `ConferenceConfigLoader`, `SecurityConfig`, `ProductionGuard`, bean wiring | everything above |

ArchUnit rules (DoD-04), test `ArchitectureTest`:

- A1 `domain` depends on no other package of the application and on no `org.springframework..`, `jakarta..`, `com.fasterxml..`, `tools.jackson..`, `org.apache.poi..` class.
- A2 `application` depends only on `domain` (and JDK, `org.springframework.transaction..`, `org.slf4j..`).
- A3 no adapter package depends on another adapter package or on `config`.
- A4 no package cycles between the slices `si.konferenca.registration.(*)..` and `adapter.(*)` (AR-03).
- A5 only `adapter.persistence` uses `jakarta.persistence..`; only `adapter.export` uses `org.apache.poi..`; only `adapter.mail` uses `org.springframework.mail..` / `jakarta.mail..`.

## 3. Behaviour

### 3.1 Conference configuration (US-003, AR-04)

- Source: JSON file at `CONFERENCE_CONFIG_PATH` (schema `conference-config.schema.json`); default `classpath:conference.json` allowed only outside production. The shipped file has two options per category, one student-only and one external-only option, one inactive option and the mandatory consent `privacy` with the OQ-02 wording.
- Loaded once at start by `ConferenceConfigLoader`. The backend refuses to start (context failure, non-zero exit) when the file is missing, not valid JSON, has unknown properties, a missing or blank `name`, `id` or consent `text`, an unknown category or registration type, or a duplicate option or consent id (AC-003-03). `active` defaults to `true`, `availableTo` to both types.
- `GET /api/form-config` returns the active options in file order, each with `id`, `name`, `category`, `availableTo`; all consents; `captchaSiteKey`; `captchaTestMode`; `conferenceName` (AC-003-01). A changed file takes effect on the next start without code change (AC-003-02).

### 3.2 Registration (US-001, US-002, US-005)

`POST /api/registrations` → `RegistrationService.register(submission)`:

1. Parse the body as a map of nullable strings and string lists; a non-JSON body, a wrong JSON type or an unknown `type` value is a field error (`type`, or `body` for an unreadable body).
2. Trim every text field (BR-02). Fields of the other type are ignored and stored as `null`.
3. Validate, collecting all errors (one entry per field): required fields non-empty (`firstName`, `lastName`, `email`, plus `organization` or `studyInstitution`, `studyProgramme`, `studentId`); length ≤ 200, email ≤ 254 (D-08); email format (D-09); `options`: every id known, active, available to the type, not listed twice, otherwise one `options` error (SR-04, OQ-01, OQ-04); `consents`: every mandatory consent present (one `consents` error), unknown consent ids rejected; `captchaToken` present.
4. Only when step 3 found no error: verify the token with `CaptchaVerifier`; rejected → `captchaToken` error (SR-01). Errors → HTTP 400 `{"errors":[…]}`, nothing stored, no mail (AC-001-02 … 07, AC-002-02 … 06).
5. In one database transaction: insert the registration, its options and consents (each consent with `givenAt` = registration time), flush; write the JSON copy (`<JSON_COPY_DIR>/<id>.json`, written to a temp file in the same directory, then atomically moved). If the write fails, the transaction rolls back and the API answers 500 with a general message (AC-005-03, AR-05). If the commit fails after the file was written, the file is deleted.
6. After commit: send the participant mail, then the organizer mails (attachment = the JSON copy bytes). Each failure is caught and logged as `mail failed for registration <id>` (OQ-03, AC-006-03).
7. Answer 201 `{"id": "<uuid>"}`.

Registration id: random UUID; time: `Clock` in UTC, stored as `timestamptz`, serialised ISO 8601 with `Z`. Repeat emails are allowed (OQ-05). Text is stored and written as UTF-8 unchanged (AC-005-04, NFR-01): the database uses UTF-8, the JSON copy is written with Jackson as UTF-8.

### 3.3 Emails (US-006, US-007)

As `02_contracts/interfaces.md`, "Emails". SMTP connection and read timeouts 5 s, so an unavailable server does not hold the response.

### 3.4 Export (US-008)

`GET /api/export` with HTTP Basic: the single in-memory user `ORGANIZER_USERNAME` with role `ORGANIZER`; the password from `ORGANIZER_PASSWORD` is BCrypt-hashed at start and only the hash is kept (SB-03). Wrong or missing credentials → 401, empty body, `WWW-Authenticate: Basic` (AC-008-02). Then `OrganizerTransportFilter` (SR-06, AC-008-04): when `ORGANIZER_HTTPS_ONLY=true` (default) and the request is not secure and the client address is not a loopback address → 403 with a general message, no data. Behind the production proxy the scheme and client address come from `X-Forwarded-Proto` / `X-Forwarded-For` as resolved by Tomcat's remote IP handling for trusted internal proxies (`server.forward-headers-strategy=native`).

Workbook: one sheet `registrations`, row 1 the 12 headers of `api-contract.md` in order, then one row per registration ordered by `registeredAt`, `id`; every cell a string cell (no formulas, so no formula injection); `options` and `consents` ids joined by `, `; cells of the other type empty (AC-008-01, AC-008-03). Content type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="registrations.xlsx"`. Only registration fields are exported (SR-07).

### 3.4a Organizer search (US-009)

`GET /api/registrations` with the organizer's HTTP Basic credentials. Access exactly as the export (BR-09, 3.4): the same in-memory organizer, `OrganizerAccess` with `ORGANIZER_HTTPS_ONLY` (SR-06, AC-009-10: 403, no data); no or wrong credentials → 401 without registration data (AC-009-09). A request carrying `X-Requested-With: XMLHttpRequest` (sent by the frontend) gets its 401 without `WWW-Authenticate`, so the browser shows no native login dialog over the organizer page (Spring Security's HTTP Basic default for such requests); every other client still receives `WWW-Authenticate: Basic`.

1. `SearchController` reads the query parameters `text`, `type`, `option`, `page` (first value of each; other parameters are ignored) and passes them as nullable strings to `SearchCriteriaParser.parse(text, type, option, page, conferenceConfig)`.
2. Parsing (SB-01, AC-009-08, D-26, D-27), collecting one error per invalid parameter, each with the parameter name as `field`:
   - `text`: absent → empty; trimmed (BR-10); more than 100 code points after trimming → error `text`.
   - `type`: absent → no filter; otherwise exactly `EXTERNAL` or `STUDENT`, else error `type` (blank and lower case included).
   - `option`: absent → no filter; otherwise the id of a configured option (active or inactive, G-8), else error `option`.
   - `page`: absent → 1; otherwise 1 to 6 decimal digits with value ≥ 1, else error `page`.
   Errors → 400 `{"errors":[…]}` as the registration endpoint.
3. `SearchService.search(…)` passes the criteria to the port `RegistrationSearch.search(criteria)` (a port of its own, implemented by `JpaRegistrationRepository`), which reads 20 rows from offset (page − 1) × 20 and returns `RegistrationPage(total, page, items)`.
4. Matching in PostgreSQL (`JpaRegistrationRepository`, one JPQL count and one JPQL page query, all values bound as parameters, SB-05): the text matches when `lower(column) LIKE lower('%' || escaped text || '%') ESCAPE '\'` for any of `first_name`, `last_name`, `email`, `organization`, `study_institution` (`%`, `_` and `\` in the text are escaped, so they match literally); an empty text skips the condition. The database is UTF-8 and `lower()` folds Č/č, Š/š, Ž/ž (AC-009-11, NFR-01). Type filter: `type = :type`. Option filter: an `EXISTS` over `registration_option` with `option_id = :option`. All conditions are combined with AND (BR-10, AC-009-05).
5. Order: `registered_at` descending, then `id` descending (stable paging). Page beyond the last: empty `items`, real `total` (G-9).
6. Response 200 (AR-09): `{"total": n, "page": p, "items": [...]}`; each item `id`, `type`, `firstName`, `lastName`, `email`, `organization` (null for students), `studyInstitution` (null for externals), `options` (selected option ids, sorted), `registeredAt` (ISO 8601 UTC with `Z`). Only these registration fields leave the backend (SR-07): no study programme, student ID or consents. Values are returned unchanged; encoding for display is the frontend's job (SR-09).

Rate limiting applies as for every `/api` endpoint; the window key is client address, method and path, so `GET` and `POST /api/registrations` are separate endpoints (G-10, SR-03).

### 3.5 Errors, limits and headers

- `ApiExceptionHandler`: validation → 400 with field errors; unreadable body → 400 field `body`; body too large → 413; anything else → 500 `{"message":"The request could not be processed."}`. No stack traces, exception messages or personal data in responses or logs (SB-07, ES-07); the log line carries the registration id or the exception class only.
- Rate limit (SR-03, SB-06): `RateLimitFilter` on every `/api/**` request, fixed one-minute window per client address and endpoint (method and path, US-009), limit `RATE_LIMIT_PER_MINUTE` (default 60, D-10); over the limit → 429 `{"message":"Too many requests. Please try again later."}` with `Retry-After`. It runs before authentication, so failed logins are limited too.
- Body size (SR-03): `BodySizeLimitFilter` rejects a body over 16 KiB (`REQUEST_MAX_BYTES`, default 16384) with 413, by `Content-Length` and by counting streamed bytes.
- Headers (SB-10): Spring Security defaults (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`) plus `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` and `Referrer-Policy: no-referrer` on the API. nginx adds for the frontend: CSP `default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; img-src 'self' data:; style-src 'self' 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`.
- CSRF is disabled and sessions are stateless: the API uses no cookies; the only credential is HTTP Basic on the two organizer GETs (export, search).
- CORS: only `ALLOWED_ORIGIN` (unset by default, set only in local development with the Vite dev server); allowed request headers `Content-Type`, `Authorization` (the Vite dev server proxies `/api`, so the organizer page needs no CORS).
- Health (NFR-04, ES-09): `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` public, no details; every other actuator endpoint off. Readiness includes the database. Compose health checks use readiness.

### 3.6 Frontend

As `02_contracts/interfaces.md`, "Registration form" and "Organizer search page". Modules: `api.ts` (fetch wrappers, the only place that knows URLs, incl. `searchRegistrations`), `validation.ts` (client rules mirroring 3.2 step 3), `RegistrationForm.tsx`, `OptionGroups.tsx`, `Captcha.tsx`, `Confirmation.tsx`, `OrganizerPage.tsx` (US-009: sign-in, search form, result table, paging), `App.tsx` (path `/organizer` shows the organizer page, every other path the registration form). The frontend holds no secret; the site key comes from `/api/form-config` (AR-07); organizer credentials are typed in by the organizer, kept only in component memory (no storage, no cookie, no URL) and sent only as the `Authorization` header to `/api/registrations`. All text, including stored participant data on the organizer page, is rendered through React as text nodes; no `dangerouslySetInnerHTML`, no URL or attribute built from participant data (SB-05, SR-09).

## 4. Configuration (ES-01)

| Setting | Default | Allowed default in | Purpose |
|---|---|---|---|
| `APP_ENVIRONMENT` | `local` | local, test | `production` enables `ProductionGuard` |
| `DB_URL`, `DB_USER`, `POSTGRES_PASSWORD` | `jdbc:postgresql://localhost:5432/registration`, `registration`, none | local, test (password: none anywhere) | database |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost`, `1025`, `false`, empty, empty | local, test | mail |
| `MAIL_FROM` | `registration@konferenca.local` | all | sender |
| `ORGANIZER_EMAILS` | none | — | organizer recipients |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none | — | export login; start fails if empty |
| `CONFERENCE_CONFIG_PATH` | `classpath:conference.json` | local, test | options and consents (conference name is in the file) |
| `JSON_COPY_DIR` | `./data/json-copies` (compose: `/data/json-copies` on volume `jsoncopies`) | all | JSON copies |
| `CAPTCHA_TEST_MODE` | `false` | all (on only in test and local) | test mode |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty | local, test | reCAPTCHA keys |
| `RECAPTCHA_VERIFY_URL` | `https://www.google.com/recaptcha/api/siteverify` | all | verify endpoint (mocked in tests, DoD-P05) |
| `ORGANIZER_HTTPS_ONLY` | `true` (compose local: `false`, D-11) | all | SR-06 |
| `ALLOWED_ORIGIN` | empty | local | CORS |
| `RATE_LIMIT_PER_MINUTE` | `60` (D-10) | all | SR-03 |
| `REQUEST_MAX_BYTES` | `16384` | all | SR-03 |

`ProductionGuard` (SR-02, `environments.md`): with `APP_ENVIRONMENT=production` the backend refuses to start when `CAPTCHA_TEST_MODE=true`, either reCAPTCHA key is empty, `ORGANIZER_HTTPS_ONLY=false`, `ALLOWED_ORIGIN` is set, or `DB_URL`, `SMTP_HOST` or `CONFERENCE_CONFIG_PATH` is left at its default.

## 5. Deployment

- Backend image: build stage `eclipse-temurin:21.0.10_7-jdk-alpine` (D-12) runs `./mvnw -DskipTests package`; runtime `eclipse-temurin:21.0.10_7-jre-alpine`, user `app` (uid 10001), `/data/json-copies` owned by it (SB-11); `HEALTHCHECK` on readiness.
- Frontend image: build stage `node:24.13.0-alpine` (`npm ci`, `npm run build`); runtime `nginx:1.30.5-alpine` running as user `nginx` on port 8080, `proxy_pass http://backend:8080` for `/api/` with `proxy_set_header Host backend:8080` and `X-Forwarded-For $remote_addr`, `X-Forwarded-Proto $scheme` (SR-08: the client `Host` is never forwarded).
- Production: the external nginx terminates TLS (SB-04), serves `/` from the frontend, forwards `/api` to the backend, sets the upstream `Host` itself (SR-08) and `X-Forwarded-Proto https`.
- Schema only through Flyway (`db/migration/V1__registration.sql` = `database-schema.sql`, AR-06, ES-08); `spring.jpa.hibernate.ddl-auto=validate`.

## 6. Traceability

### Acceptance criteria

| AC | Spec | AC | Spec |
|---|---|---|---|
| AC-001-01 | 3.2 steps 1–7 | AC-004-01 | 3.6, interfaces "Registration form" 8 |
| AC-001-02 | 3.2 step 3 | AC-004-02 | interfaces "Registration form" 9 |
| AC-001-03 | 3.2 step 2 | AC-004-03 | interfaces "Registration form" 7 |
| AC-001-04 | 3.2 step 3 (D-09) | AC-004-04 | interfaces "Registration form" 10, 3.5 |
| AC-001-05 | 3.2 step 3 (options) | AC-005-01 | 3.2 step 5, `database-schema.sql` |
| AC-001-06 | 3.2 step 3 (consents) | AC-005-02 | 3.2 step 5, `registration-copy.schema.json` |
| AC-001-07 | 3.2 step 4 | AC-005-03 | 3.2 step 5 |
| AC-001-08 | interfaces "Registration form" 2–5 | AC-005-04 | 3.2 (UTF-8) |
| AC-002-01 | 3.2 steps 1–7 | AC-006-01 | 3.3, interfaces "Emails" |
| AC-002-02 | 3.2 step 3 | AC-006-02 | interfaces "Emails" (safety) |
| AC-002-03 | 3.2 step 3 (D-09) | AC-006-03 | 3.2 step 6, 3.3 |
| AC-002-04 | 3.2 step 3 (availability) | AC-007-01 | 3.2 step 6, interfaces "Emails" |
| AC-002-05 | 3.2 step 3 (consents) | AC-007-02 | interfaces "Emails" (attachment) |
| AC-002-06 | 3.2 step 4 | AC-008-01 | 3.4 |
| AC-002-07 | interfaces "Registration form" 2–5 | AC-008-02 | 3.4 |
| AC-003-01 | 3.1 | AC-008-03 | 3.4 |
| AC-003-02 | 3.1 | AC-008-04 | 3.4 (`OrganizerTransportFilter`) |
| AC-003-03 | 3.1 | | |
| AC-009-01 | 3.4a steps 3, 5, 6 | AC-009-08 | 3.4a step 2 (D-26, D-27) |
| AC-009-02 | 3.4a step 4 (text) | AC-009-09 | 3.4a (401) |
| AC-009-03 | 3.4a step 4 (type) | AC-009-10 | 3.4a (`OrganizerAccess`, 403) |
| AC-009-04 | 3.4a step 4 (option) | AC-009-11 | 3.4a step 4 (UTF-8, `lower()`) |
| AC-009-05 | 3.4a step 4 (AND) | AC-009-12 | 3.6, interfaces "Organizer search page" |
| AC-009-06 | 3.4a steps 5, 6 | AC-009-13 | 3.6 (React text nodes), interfaces "Organizer search page" |
| AC-009-07 | 3.4a steps 3, 5 | | |

### Security requirements and baseline

| ID | Spec | ID | Spec |
|---|---|---|---|
| SR-01 | 3.2 step 4, interfaces "reCAPTCHA" | SB-04 | 5 (TLS at the external proxy) |
| SR-02 | 4 `ProductionGuard` | SB-05 | 3.4 string cells, 3.6 React escaping, JPA parameters |
| SR-03 | 3.5 rate limit and body size; 3.4a (search endpoint) | SB-06 | 3.5 rate limit |
| SR-04 | 3.2 step 3 | SB-07 | 3.5 errors |
| SR-05 | interfaces "Emails" (safety) | SB-08 | Dependency-Check, `npm audit` (phases 0 and 6) |
| SR-06 | 3.4 `OrganizerTransportFilter`; 3.4a (search) | SB-09 | Semgrep, Gitleaks, SpotBugs, PMD (phase 6) |
| SR-07 | 3.4, 3.4a step 6, interfaces "Emails" | SB-10 | 3.5 headers |
| SR-08 | 5 (nginx upstream `Host`) | SB-11 | 5 (non-root images) |
| SR-09 | 3.6 (React text nodes), 3.4a step 6 | | |
| SB-01 | 3.2 (server-side validation) | SB-12 | data collected = `business-rules.md` Data only |
| SB-02 | 3.4, 3.4a (export and search are the only non-public operations) | SB-13 | `security-requirements.md` table; retention OQ-06 in release notes |
| SB-03 | 3.4 BCrypt; 4 secrets without defaults | SB-14 | interfaces "Registration form" 5 |

### Non-functional requirements and architecture

| ID | Spec |
|---|---|
| NFR-01 | 3.2 (UTF-8), 3.3, 3.4, 3.4a step 4; E2E-04 |
| NFR-02 | 1 (named volumes); phase 6 demonstration |
| NFR-03 | 3.6, interfaces "Registration form" 7, 9 |
| NFR-04 | 3.5 health; 5 health checks |
| AR-01 | 1, 3.6 |
| AR-02, AR-03 | 2 |
| AR-04 | 3.1 |
| AR-05 | 3.2 step 5 |
| AR-06 | 5 |
| AR-07 | 3.6 |
| AR-09 | `openapi.yaml` (incl. `GET /api/registrations`) |
| BR-09, BR-10 | 3.4a |
| DoD-P06 | 3.4a, interfaces "Organizer search page"; E2E-06 and the phase 6 demonstration |
