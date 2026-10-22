# Test strategy

> Written in: phases 3 and 5 · Source: `general/quality/test-strategy.md`, `general/quality/measurement-scope.md`, `project/02_design/quality-requirements.md` · Agent: writes

## Levels and locations

| Level | Component | Location | Runner | Interface used |
|---|---|---|---|---|
| Acceptance (frozen) | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` | JUnit 6 via `./mvnw test` | HTTP against the started application (random port) with PostgreSQL and Mailpit Testcontainers; the database schema, JSON copy directory and settings of `02_specification.md` §7/§13 |
| Acceptance (frozen) | frontend | `frontend/tests/acceptance/` | Vitest + Testing Library (`npm test`) | the rendered `App` with `fetch` replaced by a contract-shaped fake backend (`support/mockApi.ts`) |
| End-to-end (frozen) | both | `frontend/tests/e2e/` | Playwright 1.63 Chromium (`npm run e2e`) | the browser against the running local stack (`docker compose --env-file ../.env up -d --build` in `02_output/`); database via `docker compose exec postgres psql`, JSON copy via `docker compose exec backend cat`, Mailpit API, backend API |
| Unit / integration | backend | `backend/src/test/java/si/konferenca/registration/**` outside `acceptance` | JUnit 6, Mockito, ArchUnit | phase 5 |
| Unit / component | frontend | `frontend/src/**/*.test.{ts,tsx}` | Vitest | phase 5 |

Acceptance test names (JUnit `@DisplayName`, Vitest `it` titles) and e2e test titles start with the AC or E2E id. One e2e test per scenario E2E-01..E2E-06, no other e2e test.

### Acceptance criteria → tests

| Story | Backend acceptance | Frontend acceptance | E2E |
|---|---|---|---|
| US-001 | `Us001ExternalRegistrationAcceptanceTest` (AC-001-01..07) | `us001-external-form.test.tsx` (AC-001-08) | E2E-01 |
| US-002 | `Us002StudentRegistrationAcceptanceTest` (AC-002-01..06) | `us002-student-form.test.tsx` (AC-002-07) | E2E-02 |
| US-003 | `Us003ConferenceOptionsAcceptanceTest` (AC-003-01..03) | – | – |
| US-004 | – | `us004-confirmation.test.tsx` (AC-004-01..04) | E2E-03 |
| US-005 | `Us005StorageAcceptanceTest` (AC-005-01, 02, 04), `Us005JsonCopyFailureAcceptanceTest` (AC-005-03) | – | E2E-04 |
| US-006 | `Us006ParticipantEmailAcceptanceTest` (AC-006-01, 02), `Us006MailUnavailableAcceptanceTest` (AC-006-03) | – | – |
| US-007 | `Us007OrganizerNotificationAcceptanceTest` (AC-007-01, 02) | – | – |
| US-008 | `Us008ExportAcceptanceTest` (AC-008-01..03), `OrganizerHttpsOnlyAcceptanceTest` (AC-008-04) | – | E2E-05 |
| US-009 | `Us009SearchAcceptanceTest` (AC-009-01..09, 11), `OrganizerHttpsOnlyAcceptanceTest` (AC-009-10) | `us009-organizer-page.test.tsx` (AC-009-12, 13) | E2E-06 |

### Harness notes

- Backend instances with special settings (JSON copy directory that is a file, unreachable SMTP port, HTTPS-only with a trusted proxy, other option configurations) are started by `support/RunningApp` with command-line arguments, so they override the application's configuration files the way environment variables do.
- Rejections check the error field and that no row, no JSON copy and no email to the participant's unique address appeared.
- Test data is tagged with random tokens or unique emails, so tests do not depend on each other or on order; counts are compared before/after.
- DoD-P05 (production reCAPTCHA path against a mocked endpoint) has no AC; it is an integration test in phase 5.

## Measurement exclusions (decided in phase 3, before any test was written)

| Component | Excluded from coverage and mutation | Reason |
|---|---|---|
| backend | `si.konferenca.registration.RegistrationApplication` | application entry point (`quality-requirements.md` mutation scope) |
| frontend | `src/main.tsx` | bootstrap file that only mounts the app |
| frontend | `src/test-setup.ts`, `src/**/*.test.{ts,tsx}`, `**/*.d.ts` | test code and type declarations |

Nothing else is excluded. Scope: backend `si.konferenca.registration.**`, frontend `src/**`.

## Phase 3 run (bootstrap skeleton, before any production code)

2026-10-22, skeleton commits `9ef7746` (backend) and `ce8048c` (frontend), local stack built from the skeleton.

| Suite | Tests | Passed | Failed | Reason for the failures |
|---|---|---|---|---|
| Backend acceptance | 105 | 0 | 105 | assertion failures: the skeleton has no endpoints; Spring Security's default answers 401 to every `/api` request; no schema, no JSON copy, no email; AC-003-03: the skeleton starts with any configuration |
| Frontend acceptance | 18 | 0 | 18 | assertion failures: the skeleton `App` renders only a heading (no type choice, form, organizer page) |
| End-to-end | 6 | 0 | 6 | assertion failures: the "External participant"/"Student" radio and the organizer form do not exist; the backend answers 401 to `/api/registrations` and `/api/form-config` |

No test passes on bootstrap code, and no failure comes from a build, configuration or harness error. Logs: `02_output/logs/03_backend-acceptance-run.log`, `03_backend-runningapp.log` (rerun after the harness fix below), `03_frontend-acceptance-first.log`, `03_e2e-run.log`.

Harness fixes before the freeze: `RunningApp` first passed settings as default properties, which the application's `application.yml` overrides (port 8080 clashed with the running stack, so AC-003-03 passed for the wrong reason); it now passes command-line arguments. E2E-02 now checks the form-config status before parsing. The Playwright action timeout is 15 s.

## Self-check before the freeze

- A correct implementation can pass every assertion: each one checks a behaviour the AC, `api-contract.md` or the phase 2 contracts state; where an AC leaves a choice open the test accepts every option (AC-005-03: any 5xx; AC-008-04/AC-009-10: any status ≥ 400 without data; options and consents in exports compared as sets).
- No test can pass for the wrong reason: refusal tests carry positive controls (valid credentials succeed; HTTPS-only tests also show an allowed HTTPS and localhost request), rejection tests also check that nothing was stored; AC-003-03 fails if the application starts.
- Helpers checked: `timestamptz` read with `getObject(…, OffsetDateTime.class)`; attachment bytes compared with `byte[]` equality (`isEqualTo`), not `doesNotContain`; Mailpit summaries checked for To/Cc/Bcc; the xlsx reader handles shared and inline strings; JSON fields read with Jackson 3 `asString()`.
- Formatting and lint: backend tests formatted with Spotless (google-java-format); frontend tests pass Prettier, ESLint and `tsc --noEmit`.

## Phase 5: first full run

2026-10-22, before any unit test or fix, on the phase 4 code (backend `0245a8d`, frontend `654f085`, stack rebuilt): 129 tests, 129 passed, 0 failed (backend acceptance 105, frontend acceptance 18, e2e 6). Nothing to classify.

## Phase 5: unit and integration tests

| Component | Area | Test classes / files |
|---|---|---|
| backend | domain rules | `RegistrationValidatorTest`, `EmailAddressTest`, `ConferenceConfigurationTest`, `SearchCriteriaTest`, `RegistrationModelTest` |
| backend | application | `RegistrationServiceTest` (storage order, rollback, copy deletion, email failures), `QueryServicesTest` |
| backend | adapters | `ConfigTest`, `RecaptchaVerifierTest` (DoD-P05: mocked siteverify endpoint, accepted and rejected token), `SmtpNotifierTest`, `MailTextsTest`, `FileJsonCopyStoreTest`, `PoiWorkbookWriterTest`, `LikeEscapeTest` |
| backend | security and web | `FiltersTest` (rate limit, body size, HTTPS-only, entry point), `ApiExceptionHandlerTest` |
| backend | architecture | `ArchitectureTest` (the six rules of `02_specification.md` §2) |
| frontend | logic | `validation.test.ts`, `api.test.ts`, `recaptcha.test.ts` |
| frontend | components | `Captcha.test.tsx`, `RegistrationPage.test.tsx`, `OrganizerPage.test.tsx` |

## Coverage (JaCoCo 0.8.12 / Vitest coverage-v8 4.1.11)

Scope: backend `si.konferenca.registration.**` without `RegistrationApplication`; frontend `src/**` without `main.tsx`, `test-setup.ts`, tests and declarations. Each figure from its own clean run.

| Component | Level | Tests | Line | Branch |
|---|---|---|---|---|
| backend | unit (incl. architecture) | 183 | 75.72 % (633/836) | 95.83 % (253/264) |
| backend | nonUnit (acceptance) | 105 | 86.96 % (727/836) | 72.35 % (191/264) |
| backend | all | 288 | **98.09 %** (820/836) | 99.24 % (262/264) |
| frontend | unit | 75 | 99.54 % (218/219) | 96.32 % (131/136) |
| frontend | nonUnit (acceptance) | 18 | 78.08 % (171/219) | 70.58 % (96/136) |
| frontend | all | 93 | **100 %** (219/219) | 98.52 % (134/136) |

The e2e tests exercise both components inside containers; their coverage is not instrumented, so nonUnit is the acceptance level only. Line gate (95 %) met for both components after the unit areas above.

## Mutation testing

### Backend (PIT 1.30.0, pitest-junit5-plugin 1.2.3, default mutators)

One full run, 2026-10-22 20:25–20:51 UTC, scope `si.konferenca.registration.*` without `RegistrationApplication`, tests: all 288 JUnit tests (unit, architecture, acceptance; D-15). Log `logs/05_pit-full.log`, mutants `logs/05_pit-full-mutations.csv`.

| Generated | Killed | Survived | No coverage | Equivalent | Raw score | Adjusted score | Covered score |
|---|---|---|---|---|---|---|---|
| 365 | 340 | 18 | 7 | 0 | **93.15 %** | **93.15 %** | 94.97 % (340/358) |

The gate (≥ 90 %) is met by the first full run: no survivor round, no further backend tests (stop rule).

Survivors in security, validation and persistence code, classified individually (`severity-scale.md`):

| Class.method:line | Mutation | Why it survives | Severity |
|---|---|---|---|
| `RegistrationValidator.text:89`, `email:98` | return "" instead of null for an invalid field | the value is never used once an error was recorded (the result carries no data when errors exist) | Low |
| `StartupChecks.verify:18` | check call removed | the checks themselves are tested through `StartupChecks.check`; only the Spring hook is unasserted | Low |
| `RateLimitFilter.doFilterInternal:46-47` (5) | purge threshold and purge predicate | purging stale windows only bounds memory; the window arithmetic decides the limit and is killed | Low |
| `RequestSizeFilter$LimitedStream.read:79, 88` | `>` vs `>=` on the running count | boundary of a body without declared length; such bodies are rejected either way one byte later | Low |
| `SecurityFilters.corsConfigurationSource:51-59` (2 survived, 7 no coverage) | CORS origin branch | CORS is only for local development and off by default; production refuses start with it | Low |
| `SecurityFilters.register:42`, `JsonAuthenticationEntryPoint.writeJson:35` | `setOrder` / `setCharacterEncoding` removed | order and charset match the container defaults in these tests | Low |
| `JpaRegistrationStore.save:24` | `flush()` removed | the commit flushes anyway; the explicit flush only surfaces errors before the JSON copy is written | Low |
| `FileJsonCopyStore.write:46` | cleanup of the temporary file after a failed write removed | in the tested failure the temporary file is never created (the directory is a file), so nothing is left either way | Low |
| `SmtpNotifier.notifyOrganizers:66` | `setFrom` removed | the sender is taken from the mail session default in the test; the From address is asserted only for the participant email | Low |
| `ApiExceptionHandler.causeName:71` | log text "" | log content only | Low |

### Frontend (Stryker 10.0.0 with the Vitest runner 4.1.11)

Scope `src/**` without `main.tsx`, `test-setup.ts`, tests and declarations; tests: all Vitest tests (unit and acceptance; D-15). The tool check (`validation.ts` alone, 73.5 % at 89 % line coverage) showed that mutants are activated.

| Run | Tests | Files mutated | Mutants | Killed | Survived | Score |
|---|---|---|---|---|---|---|
| full, 20:51–21:00 UTC | 93 | all 7 | 535 | 459 | 76 | 85.79 % |
| survivor round 1, 21:02–21:10 UTC | 109 | `RegistrationPage.tsx`, `OrganizerPage.tsx`, `Captcha.tsx`, `App.tsx`, `api.ts` | 452 | 419 | 33 | 92.70 % |

Recomputed after round 1 (classes not re-run keep their full-run counts):

| File | Killed / total | Source |
|---|---|---|
| `RegistrationPage.tsx` | 192 / 209 | round 1 |
| `OrganizerPage.tsx` | 93 / 103 | round 1 |
| `api.ts` | 93 / 95 | round 1 |
| `Captcha.tsx` | 38 / 42 | round 1 |
| `App.tsx` | 3 / 3 | round 1 |
| `validation.ts` | 68 / 68 | full run |
| `recaptcha.ts` | 15 / 15 | full run |
| **component** | **502 / 535 = 93.83 %** raw = adjusted (no equivalents claimed) | |

93.83 % is more than 1.0 point above 90 %: gate met after round 1, no confirming full run, no further frontend tests. Round 1 tests: `RegistrationPage.fields.test.tsx`, `OrganizerPage.state.test.tsx`, two cases in `Captcha.test.tsx`.

The 33 survivors left are in UI state handling, not in validation (`validation.ts` 100 %), security or persistence code: initial values that later code ignores (`useState` defaults of unused lists, effect dependency arrays), `preventDefault` calls (jsdom does not navigate), the space text between a checkbox and its label (the accessible name is trimmed), catch blocks returning `undefined` instead of `null` (both falsy to the caller), and guards that the UI cannot reach (submit without a type). All Low.

## Code size and duplication (cloc 1.98 in image `aldanial/cloc:2.10`, D-04)

| Component | Production code lines | Test code lines | Duplication |
|---|---|---|---|
| backend | 2061 (Java 1931, YAML 69, JSON 32, SQL 29) | 3805 | PMD CPD (100 tokens): 0 |
| frontend | 782 (TypeScript 719, CSS 63) | 2195 (unit, acceptance, e2e) | jscpd 5.4.0 on `src` without tests: 0 % |

## Equivalent mutants

None claimed in either component (adjusted score = raw score).
