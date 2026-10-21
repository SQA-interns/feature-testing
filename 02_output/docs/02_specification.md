# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Conference registration: participants register through a React form; a Spring Boot API validates, stores (PostgreSQL row + JSON copy), mails and exports registrations. Versions: `tech-stack.md` as amended by D-09. Contracts: [`02_contracts/`](02_contracts/).

## 1. Components and deployment

| Component | Folder | Runs as | Local port (127.0.0.1 only) |
|---|---|---|---|
| backend | `02_output/backend` | `eclipse-temurin` JRE image, user `app` (uid 10001), jar built by `./mvnw package` | 8080 |
| frontend | `02_output/frontend` | multi-stage: `node` image builds, `nginx` image serves `dist/` as user `nginx` on port 8080 | 3000 |
| postgres | image | `postgres` image, database and user `registration`, named volume `pgdata` | not published |
| mailpit | image | `axllent/mailpit`, SMTP 1025 inside the network | 8025 (web and API) |

- `02_output/docker-compose.yml` starts all four; JSON copies live on the named volume `json-copies` mounted at `/data/json-copies` (NFR-02). Health checks: backend `wget -qO- http://127.0.0.1:8080/actuator/health/readiness`, frontend `wget -qO- http://127.0.0.1:8080/healthz`, postgres `pg_isready`; `depends_on: condition: service_healthy` orders start-up (NFR-04, ES-09).
- nginx serves `/` from the build and proxies `/api/` to `http://backend:8080` with `proxy_set_header Host backend:8080` (never the client's `Host`, SR-08), `X-Forwarded-For $remote_addr`, `X-Forwarded-Proto $scheme`; it adds the frontend security headers (section 7). Production puts the external TLS proxy in front (`environments.md`); its `Host` rule is in the release notes.
- `docker-compose.yml` passes `CAPTCHA_TEST_MODE` (default `true`), `RATE_LIMIT_PER_MINUTE` (default `30`) and `ORGANIZER_HTTPS_ONLY` (default `false`, local plain HTTP) from the environment, plus the `.env` secrets via `--env-file .env` (AR-09).

## 2. Backend architecture (AR-02, AR-03)

Ports-and-adapters with four layers under `si.konferenca.registration`. Reason: the rules that matter (validation, storage order, captcha) stay framework-free and testable, and each external system (database, file, SMTP, Google, Excel) sits behind one port, so test substitutes replace only adapters.

| Package | Contains | May depend on |
|---|---|---|
| `domain` | records and enums (`Registration`, `RegistrationType`, `OptionCategory`, `ConferenceOption`, `ConsentDefinition`, `ConferenceCatalog`, `FieldError`), `RegistrationValidator`, `ConferenceConfigException` | `java..` only |
| `application` | use cases `RegisterParticipant`, `RegistrationStorage` (transactional), `FormConfiguration`, `ExportRegistrations`; ports `RegistrationRepository`, `JsonCopyStore`, `CaptchaVerifier`, `RegistrationNotifier`, `WorkbookWriter`; exceptions `RegistrationRejectedException`, `StorageFailedException` | `domain`, `java..`, `org.slf4j..`, `org.springframework.stereotype..`, `org.springframework.transaction..` |
| `adapter.persistence`, `adapter.jsoncopy`, `adapter.mail`, `adapter.captcha`, `adapter.export`, `adapter.catalog` | JPA entities and repositories, file writer, mail sender, test-mode and Google verifiers, POI workbook writer, configuration loader | `application`, `domain`, frameworks; never `web` or another adapter |
| `web` | REST controllers, DTOs, `ApiExceptionHandler`, `RateLimitFilter`, `RequestSizeFilter` | `application`, `domain`, frameworks; never `adapter` |
| `config` | `AppProperties` (`@ConfigurationProperties("app")`), `StartupGuard`, `SecurityConfig`, `OrganizerHttpsOnlyFilter`, bean wiring | everything (composition root) |

ArchUnit rules (`architecture` tests, phase 3 freezes none of them; written in phase 5 as architecture tests): exactly the "May depend on" column, plus `slices().matching("si.konferenca.registration.(*)..").should().beFreeOfCycles()` (AR-03) and "`web` does not access `jakarta.persistence`" .

## 3. Configuration (ES-01, AR-04)

Spring profiles `local` (compose) and `test` (automated tests) are the only environments where defaults marked "local/test" apply; production runs without a profile. Property names are under `app.` and bound from the environment variables below.

| Environment variable | Property | Default | Notes |
|---|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `spring.datasource.*` | local/test only (`jdbc:postgresql://localhost:5432/registration`, `registration`) | compose maps `DB_PASSWORD` from `POSTGRES_PASSWORD`; no password default |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS` | `spring.mail.host`, `.port`, `mail.smtp.starttls.enable/required` | local/test only (`localhost`, `1025`, `false`); production default TLS `true` | connect/read/write timeouts 5 s |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.username/password` | empty | production only |
| `MAIL_FROM` | `app.mail.from` | `registration@konferenca.example` | |
| `CONFERENCE_NAME` | `app.conference.name` | `Conference` | used in email subjects and bodies |
| `CONFERENCE_CONFIG` | `app.conference.config-location` | local/test only: `classpath:conference-config.json` | Spring resource location (`file:/…` in production) |
| `JSON_COPY_DIR` | `app.json-copy.directory` | `/data/json-copies` | created on first write, not at start |
| `CAPTCHA_TEST_MODE` | `app.captcha.test-mode` | `false` | `true` only in profiles local/test (startup guard) |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | `app.captcha.site-key`, `.secret-key` | empty | required when test mode is off |
| `RECAPTCHA_VERIFY_URL` | `app.captcha.verify-url` | `https://www.google.com/recaptcha/api/siteverify` | tests point it to a mock |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | `app.organizer.username`, `.password` | none (required) | password hashed with BCrypt at start; only the hash is kept (SB-03) |
| `ORGANIZER_EMAILS` | `app.organizer.emails` | none (required) | comma separated |
| `ORGANIZER_HTTPS_ONLY` | `app.organizer.https-only` | `true` | `false` only in profiles local/test |
| `CORS_ALLOWED_ORIGIN` | `app.cors.allowed-origin` | empty (no CORS) | non-empty only in profile local |
| `RATE_LIMIT_PER_MINUTE` | `app.rate-limit.per-minute` | `30` | per client address and endpoint |
| `MAX_REQUEST_BYTES` | `app.request.max-bytes` | `16384` | |

**Startup guard** (`StartupGuard`, runs before the web server accepts requests): the backend refuses to start (context fails) when (a) test mode is on outside local/test, (b) test mode is off and a reCAPTCHA key is empty, (c) `ORGANIZER_HTTPS_ONLY=false` or `CORS_ALLOWED_ORIGIN` is set outside local/test, (d) organizer username, password or emails are empty, (e) the conference configuration is missing or invalid (AC-003-03).

**Conference configuration** (AR-04, US-003): a JSON file following [`conference-config.schema.json`](02_contracts/conference-config.schema.json), read once at start by `adapter.catalog.JsonConferenceCatalogLoader` into a `domain.ConferenceCatalog`. Validation in `ConferenceCatalog` (domain): duplicate option id, duplicate consent id, unknown category or type, blank id/name/text, empty `availableTo` → `ConferenceConfigException` → start fails. Another file plus a restart changes the options without a code change (AC-003-02). The shipped `classpath:conference-config.json` contains:

| id | name | category | availableTo | active |
|---|---|---|---|---|
| `ws-ai` | AI workshop | WORKSHOP | both | yes |
| `ws-security` | Secure coding workshop | WORKSHOP | both | yes |
| `ev-reception` | Welcome reception | EVENT | both | yes |
| `ev-industry-tour` | Industry tour | EVENT | EXTERNAL | yes |
| `meal-lunch` | Lunch | MEAL | both | yes |
| `meal-dinner` | Conference dinner | MEAL | EXTERNAL | yes |
| `other-certificate` | Certificate of attendance | OTHER | both | yes |
| `ws-legacy` | Legacy systems workshop | WORKSHOP | both | no |

and one consent `privacy` (mandatory) with the wording of OQ-02.

## 4. REST API (AR-09, `api-contract.md`)

Exactly [`openapi.json`](02_contracts/openapi.json). Responses carry `Content-Type: application/json;charset=UTF-8` (`server.servlet.encoding.force-response=true`).

### `GET /api/form-config`
Active options only, each with `id`, `name`, `category`, `availableTo` (AC-003-01); all consents; `captchaSiteKey`; additional field `captchaTestMode`.

### `POST /api/registrations` — validation (`domain.RegistrationValidator`, SB-01)
All checks run and every failing field gets one entry (first failing rule per field), so the frontend can show them together.

| Field | Rule (in order) | Message |
|---|---|---|
| body | unreadable JSON or wrong JSON types → only this error | `Request body is not valid JSON.` |
| `type` | missing or not `EXTERNAL`/`STUDENT` (D-13) → only this error | `Registration type must be EXTERNAL or STUDENT.` |
| `firstName`, `lastName`, `email`, and `organization` (EXTERNAL) or `studyInstitution`, `studyProgramme`, `studentId` (STUDENT) | trimmed (`String.strip()`, BR-02); null/empty after trim → required; longer than 200 (email 254) code points → too long (D-12) | `This field is required.` / `This value is too long.` |
| `email` | after trim must match `EMAIL_PATTERN` below (BR-03; no whitespace, CR or LF) | `Enter a valid email address.` |
| fields of the other type | ignored, stored as null | — |
| `options` | null = none; an id listed twice (OQ-04), unknown, inactive (BR-04, SR-04) or not available to the type (OQ-01) → one error naming the first offending id | `Option '<id>' is not available.` / `Option '<id>' is selected more than once.` |
| `consents` | a mandatory consent missing (BR-05) or an id not in the configuration (D-11); duplicates collapse | `Consent '<id>' is required.` / `Consent '<id>' is unknown.` |
| `captchaToken` | null/blank → required; otherwise `CaptchaVerifier.verify(token)` false → rejected; verified only when every other field is valid | `This field is required.` / `The reCAPTCHA check failed. Please try again.` |

`EMAIL_PATTERN` (backend and frontend):

```
^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$
```

Any error → HTTP 400 `{"errors":[…]}`, nothing stored, no email (AC-001-02..07, AC-002-02..06).

### `POST /api/registrations` — acceptance flow (AR-05, BR-07, OQ-03)
1. `RegisterParticipant` validates and verifies the captcha.
2. `RegistrationStorage.store` (`@Transactional`): creates `Registration` (UUID, `registeredAt` = now truncated to milliseconds UTC, every given consent with `givenAt = registeredAt`), saves it through `RegistrationRepository` and flushes, then `JsonCopyStore.write` serialises it to [`registration-copy.schema.json`](02_contracts/registration-copy.schema.json) and writes `<id>.json` (temp file in the same directory, then atomic move). An exception rolls the row back; a rollback after the file was written deletes the file (transaction synchronisation). Any storage failure → `StorageFailedException` → HTTP 500 `{"message":"The registration could not be stored. Please try again later."}` (AC-005-03).
3. After the commit, `RegistrationNotifier` sends the participant confirmation and one organizer notification per address ([`emails.json`](02_contracts/emails.json)), synchronously with 5 s timeouts; each failure is caught and logged as `Email for registration <id> failed` only (no personal data, ES-07).
4. HTTP 201 `{"id":"<uuid>"}` with `Location`.

### `GET /api/export` (BR-08, US-008)
`ExportRegistrations` reads every registration ordered by `registeredAt`, `id`; `adapter.export.PoiWorkbookWriter` writes an XSSF workbook, sheet `Registrations`, header row of the 12 contract columns, one row per registration, all string cells (no formula evaluation of user text), `options`/`consents` joined by `, `, non-applicable cells empty, `registeredAt` as `Instant.toString()`. `Content-Disposition: attachment; filename="registrations.xlsx"`.

### Errors (SB-07, ES-07)
`ApiExceptionHandler`: 400 as above; 404/405/415 and every unexpected exception → `{"message": "<generic text>"}` without stack traces, class names or SQL; Spring's whitelabel and `server.error.include-*` disabled. Log lines carry the registration id or request path, never field values.

## 5. Security (SB-01..SB-11, SR-01..SR-08, ASVS 5.0 L1)

- **Organizer access (SB-02, BR-08, SR-06, SR-07)**: Spring Security, stateless, HTTP Basic with one in-memory user (`ROLE_ORGANIZER`, BCrypt hash, SB-03). `/api/export` requires the role; `GET /api/form-config`, `POST /api/registrations`, `GET /actuator/health/**` are public; everything else is denied. Wrong or missing credentials → 401 with `WWW-Authenticate: Basic realm="organizer"` and no body data. `OrganizerHttpsOnlyFilter` runs after authentication on `/api/export`: when `https-only` is on, the request is not secure (`request.isSecure()`, with `server.forward-headers-strategy=native` honouring `X-Forwarded-Proto` from trusted internal proxies) and the client address is not loopback → 403 `{"message":"Organizer access requires HTTPS."}`, no data (AC-008-04). CSRF is disabled (no cookies or sessions; Basic credentials are not sent automatically cross-site by the form).
- **reCAPTCHA (SR-01, SR-02, DoD-P05)**: [`recaptcha.json`](02_contracts/recaptcha.json). `adapter.captcha.TestModeCaptchaVerifier` (token `test-pass`) or `GoogleCaptchaVerifier` (Spring `RestClient`, form POST to `app.captcha.verify-url`) selected by `app.captcha.test-mode`; the Google path is tested against a mock HTTP server, accepted and rejected token.
- **Rate and size limits (SR-03, SB-06)**: `RateLimitFilter` — fixed one-minute window per (client address, endpoint `METHOD path`) for every `/api/**` request, including failed logins; over the limit → 429 with `Retry-After`; old windows evicted. Client address = `request.getRemoteAddr()` after the native forward-header handling. `RequestSizeFilter` — `Content-Length` above `max-bytes` → 413; bodies without a length are read through a counting stream that fails at the limit → 413.
- **Input and output (SB-01, SB-05, SR-04, SR-05)**: server-side validation above; JPA with bound parameters; option ids checked against the active set; emails plain text, fixed subjects, no user text in headers; the export writes string cells.
- **Headers (SB-10)**: backend — `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx — `Content-Security-Policy: default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`, plus `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: same-origin`; `server_tokens off`. (`'unsafe-inline'` for styles only: the reCAPTCHA widget sets inline styles.)
- **CORS**: none by default; `CORS_ALLOWED_ORIGIN` (local only) allows that origin for `/api/**` GET/POST.
- **TLS (SB-04)**: outside the machine through the external reverse proxy (`environments.md`); organizer access enforced as above.
- **Least privilege (SB-11)**: both images run as non-root users; the database user owns only its database.
- **Secrets (SB-03, ES-02)**: only from the environment / `.env`; no defaults in code; `.env` git-ignored.
- **Scanning (SB-08, SB-09)**: Dependency-Check, `npm audit`, Semgrep, Gitleaks, SpotBugs, PMD in phase 6.
- **Personal data (SB-12..SB-14)**: exactly the fields of `security-requirements.md` "Personal data"; retention OQ-06 (operator deletes rows and copies at most 12 months after the conference, stated in the release notes); consents never preselected, stored with `givenAt`.

## 6. Storage (BR-07, AR-06, ES-08, NFR-01)

[`database.json`](02_contracts/database.json): Flyway migration `V1__create_registration_tables.sql`; `spring.jpa.hibernate.ddl-auto=validate`. JPA entity `RegistrationEntity` with element collections for options (ordered) and consents. Database encoding UTF8; the JSON copy is UTF-8 written by Jackson; mails UTF-8; POI strings are Unicode — Slovenian characters survive every step.

## 7. Frontend (US-001..US-004, NFR-03, AR-01, AR-07)

React 19 single page following [`ui-registration-form.json`](02_contracts/ui-registration-form.json).

| Module | Responsibility |
|---|---|
| `src/api/client.ts` | `fetchFormConfig()`, `submitRegistration(payload)` → `{kind:'created',id} \| {kind:'invalid',errors} \| {kind:'failed'}`; relative `/api/...` URLs only |
| `src/domain/validation.ts` | client rules mirroring section 4 (required after trim, email pattern, lengths, mandatory consents, captcha) |
| `src/domain/payload.ts` | builds the request: trimmed values, only the fields of the chosen type, selected option ids in display order, consent ids, token |
| `src/components/RegistrationForm.tsx` | type chooser, fields, option groups, consents, captcha, submit, field and general errors |
| `src/components/OptionGroups.tsx`, `Captcha.tsx`, `Confirmation.tsx` | as named |
| `src/App.tsx` | loads the configuration, shows loading/error, form or confirmation |

Behaviour: no type chosen → only the chooser; choosing a type shows exactly that type's fields and the active options available to it grouped by category (AC-001-08, AC-002-07); client validation blocks the request (AC-004-03); 400 → backend errors next to fields, values kept (AC-004-02); 201 → confirmation with the participant's name and selected option names, form removed (AC-004-01); other status or network error → general error, no details (AC-004-04). The only key in the frontend is the site key from `/api/form-config` (AR-07). Dev server proxies `/api` to `127.0.0.1:8080`; production nginx proxies `/api/` (section 1).

## 8. Tests and measurement

Levels, tools and locations (frozen in phase 3; `general/phases.md`):

| Level | Backend | Frontend |
|---|---|---|
| acceptance | `src/test/java/si/konferenca/registration/acceptance/**` — JUnit 6 + Spring Boot test + MockMvc, PostgreSQL and Mailpit through Testcontainers, a JDK `HttpServer` as the reCAPTCHA mock | `tests/acceptance/**` — Vitest + Testing Library + jsdom, `fetch` stubbed |
| e2e | — | `e2e/**` — Playwright (Chromium) against `docker compose up` (frontend `http://127.0.0.1:3000`, backend `:8080`, Mailpit `:8025`), one test per E2E-01..05 |
| unit / integration / architecture | phase 5, `src/test/java/...` outside `acceptance` | phase 5, `src/**/*.test.ts(x)` |

Coverage JaCoCo / V8, mutation PIT / Stryker over the scopes of `quality-requirements.md`.

## Traceability

| AC / SR / AR / NFR / SB | Section |
|---|---|
| AC-001-01, AC-002-01 | 4 (acceptance flow) |
| AC-001-02, AC-002-02 | 4 (validation: required) |
| AC-001-03 | 4 (validation: trim) |
| AC-001-04, AC-002-03 | 4 (validation: email) |
| AC-001-05, AC-002-04 | 4 (validation: options), 3 (catalog) |
| AC-001-06, AC-002-05 | 4 (validation: consents) |
| AC-001-07, AC-002-06 | 4 (validation: captcha), 5 (reCAPTCHA) |
| AC-001-08, AC-002-07 | 7, `ui-registration-form.json` |
| AC-003-01 | 4 (`GET /api/form-config`) |
| AC-003-02, AC-003-03 | 3 (conference configuration, startup guard) |
| AC-004-01 .. AC-004-04 | 7 |
| AC-005-01, AC-005-02 | 4 (acceptance flow), 6 |
| AC-005-03 | 4 (acceptance flow step 2) |
| AC-005-04, AC-008-03 | 6 |
| AC-006-01, AC-006-02 | 4 step 3, `emails.json`, 5 (input and output) |
| AC-006-03 | 4 step 3 |
| AC-007-01, AC-007-02 | 4 step 3, `emails.json` |
| AC-008-01 | 4 (`GET /api/export`) |
| AC-008-02, AC-008-04 | 5 (organizer access) |
| SR-01, SR-02 | 5 (reCAPTCHA), 3 (startup guard) |
| SR-03 | 5 (rate and size limits) |
| SR-04, SR-05 | 4 (validation), 5 (input and output), `emails.json` |
| SR-06, SR-07 | 5 (organizer access), 4 (export), `emails.json` |
| SR-08 | 1 (nginx) |
| AR-01 | 7, 1 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 |
| AR-05 | 4 step 2 |
| AR-06 | 6 |
| AR-07 | 7, `recaptcha.json` |
| AR-09 | 4, `openapi.json` |
| NFR-01 | 6 |
| NFR-02 | 1 (named volumes) |
| NFR-03 | 7 |
| NFR-04 | 1 (health checks), 5 |
| SB-01, SB-05 | 4, 5 |
| SB-02, SB-03 | 5 (organizer access, secrets) |
| SB-04 | 5 (TLS) |
| SB-06 | 5 (rate and size limits) |
| SB-07 | 4 (errors) |
| SB-08, SB-09 | 5 (scanning) |
| SB-10 | 5 (headers) |
| SB-11 | 1, 5 (least privilege) |
| SB-12, SB-13, SB-14 | 5 (personal data) |
| DoD-P01 .. DoD-P04 | 8 (e2e), phase 6 runtime demonstration |
| DoD-P05 | 5 (reCAPTCHA), 8 |
