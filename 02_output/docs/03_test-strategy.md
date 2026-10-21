# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner and substitutes | Command |
|---|---|---|---|---|
| acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/**`, `backend/src/test/resources/acceptance/**` | JUnit Jupiter 6 + Spring Boot test (whole application, MockMvc on the public HTTP API), PostgreSQL `postgres:16.15-alpine` and Mailpit `axllent/mailpit:v1.31.1` through Testcontainers, a JDK `HttpServer` as the reCAPTCHA verification mock (DoD-P05); storage observed through JDBC, the JSON copy directory and the Mailpit API | `./mvnw test` |
| acceptance | frontend | `frontend/tests/acceptance/**` | Vitest 4.1.11 + Testing Library + jsdom; `fetch` stubbed with the contract's responses | `npm test` |
| e2e | both | `frontend/e2e/**` (one test per E2E-01..05) | Playwright 1.63.0 (Chromium) against `docker compose up` (frontend `:3000`, backend `:8080`, Mailpit `:8025`); database and JSON copies read with `docker compose exec`; organizer settings exported from `.env` | `npx playwright test -c e2e/playwright.config.ts` |
| unit, integration, architecture | both | phase 5 | — | — |

AC → test (test names carry the AC id):

| AC | Tests |
|---|---|
| AC-001-01..07 | `ExternalRegistrationAcceptanceTest` (`ac_001_0n_*`), `CaptchaVerificationAcceptanceTest` (`ac_001_07_*`, production reCAPTCHA path) |
| AC-001-08 | `external-form.acceptance.test.tsx` |
| AC-002-01..06 | `StudentRegistrationAcceptanceTest`, `CaptchaVerificationAcceptanceTest` (`ac_002_06_*`) |
| AC-002-07 | `student-form.acceptance.test.tsx` |
| AC-003-01 | `FormConfigAcceptanceTest` |
| AC-003-02 | `ChangedOptionsConfigAcceptanceTest` |
| AC-003-03 | `InvalidOptionsConfigAcceptanceTest` |
| AC-004-01..04 | `confirmation.acceptance.test.tsx` |
| AC-005-01, 02, 04 | `StorageAcceptanceTest` |
| AC-005-03 | `JsonCopyFailureAcceptanceTest` |
| AC-006-01, 02 | `ParticipantEmailAcceptanceTest` |
| AC-006-03 | `MailUnavailableAcceptanceTest` |
| AC-007-01, 02 | `OrganizerNotificationAcceptanceTest` |
| AC-008-01..03 | `ExportAcceptanceTest` |
| AC-008-04 | `OrganizerHttpsOnlyAcceptanceTest` |
| E2E-01..05 | `external-registration`, `student-registration`, `field-errors`, `slovenian-characters`, `organizer-export` (`.e2e.ts`) |

Harness self-check before the freeze (skill step 5): the Mailpit client was checked against a real Mailpit container (summary `Cc`/`Bcc` are `null`, details `[]`; text ends with CRLF); the e2e helpers (`sql`, `registrationsByEmail`, `storedOptions`, `jsonCopy`, `mailsTo`, `jsonAttachments`) against a scratch compose project with the same service names and Slovenian data; the e2e xlsx reader against a workbook written by POI 5.5.1 (entities, Slovenian text, missing cells). `TestStack` limits each cached Spring context to 2 database connections. `InvalidOptionsConfigAcceptanceTest` passes settings as command-line arguments so no profile file can override the invalid file, and has a positive control. Timestamps are read as `OffsetDateTime` (`getObject(..., OffsetDateTime.class)`); byte prefixes are compared with `Arrays.copyOf`.

## Phase 3 run against the bootstrap skeleton

Run on 2026-10-21 against the phase 0 skeletons (no production code beyond them). Logs: `out/logs/03_backend-acceptance-run.log`, `out/logs/03_frontend-acceptance.log`, `out/logs/03_frontend-e2e.log`, `out/logs/03_frontend-e2e-02.log`.

| Suite | Tests | Failed | Passed | Reason for the failures |
|---|---|---|---|---|
| backend acceptance | 96 | 94 | 2 | 77 × `POST /api/registrations` answers 403 (the skeleton's default Spring Security rejects the POST) instead of 201/400; 8 × 401 instead of 200 on `/api/form-config` and `/api/export` (no public endpoint, no organizer login); 6 × `relation "registration" does not exist` (no migration); 3 × an invalid options file does not stop the start (no configuration loader) |
| frontend acceptance | 13 | 13 | 0 | the skeleton renders no "Registration type" radiogroup (no form) |
| e2e | 5 | 5 | 0 | E2E-01, 03, 04: no "Registration type" radiogroup; E2E-02: `GET /api/form-config` answers 401; E2E-05: `GET /api/export` answers 401 instead of 200 (stack: skeleton backend jar on `:8080` with a scratch PostgreSQL, skeleton frontend dev server on `:3000`; the compose stack is built in phase 4) |

Tests that pass only because bootstrap code already provides the behaviour:

| Test | Why it passes on the skeleton |
|---|---|
| `InvalidOptionsConfigAcceptanceTest.ac_003_03_sameSettingsWithValidConfigurationStart` | positive control: the skeleton starts with any settings |
| `OrganizerHttpsOnlyAcceptanceTest.ac_008_04_missingCredentialsOverPlainHttpFromRemoteClientAreStillUnauthorized` | the skeleton's default Spring Security answers 401 to every unauthenticated request |

One harness change after the first e2e run: E2E-02 now asserts the status of `GET /api/form-config` before parsing it, so a missing endpoint fails as an assertion rather than as a JSON parse error.

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | `@SpringBootApplication` entry point, no logic (`quality-requirements.md` mutation scope) |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app |
| `frontend/src/**/*.d.ts` | type declarations, no runtime code |
| `frontend/src/**/*.test.{ts,tsx}` | test files colocated with the source (phase 5) |

Nothing else is excluded; no generated code exists in either component.

## First complete run (before any fix)

Phase 5, 2026-10-21, on the code of the phase 4 gate, before any test or code change (`out/logs/05_backend-first-run.log`, `out/logs/05_frontend-first-run.log`, `out/logs/04_frontend-e2e.log`).

| Component | Level | Passed | Failed | Line coverage (non-unit) | Branch coverage (non-unit) |
|---|---|---|---|---|---|
| backend | acceptance | 96 | 0 | 87.2 % (681/781, JaCoCo 0.8.12) | 71.0 % (169/238) |
| frontend | acceptance | 13 | 0 | 80.3 % (V8, Vitest 4.1.11) | 69.3 % |
| both | e2e | 5 | 0 | not instrumented (browser against the built bundle) | — |

No failure, so nothing to classify (`general/quality/test-strategy.md`, "First complete run").

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
