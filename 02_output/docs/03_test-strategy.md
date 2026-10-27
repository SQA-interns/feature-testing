# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner and harness | Frozen |
|---|---|---|---|---|
| Acceptance | backend | `02_output/backend/src/test/java/si/konferenca/registration/acceptance/` | JUnit 6 through Surefire (`./mvnw -B test`). The real backend on a random port (`@SpringBootTest`) is called over HTTP with `java.net.http.HttpClient`. PostgreSQL `postgres:16.15-alpine` and Mailpit `axllent/mailpit:v1.31.1` run as Testcontainers. The siteverify mock is the JDK `HttpServer`. Configuration is supplied only under the variable names of specification S7.1. Outcomes are read through the public interfaces: the HTTP API, the storage contract (`database-schema.sql`, plain JDBC), the JSON copy directory and the Mailpit API. | yes |
| Acceptance | frontend | `02_output/frontend/src/acceptance/` | Vitest 4 with jsdom and Testing Library. `App` is rendered and driven by accessible role and name, as in `registration-form-ui.md`. `fetch` is stubbed with the contract shapes of `openapi.yaml`. | yes |
| End-to-end | local stack | `02_output/frontend/e2e/` | Playwright 1.63 Chromium against `docker compose up` in `02_output`, with one test per scenario E2E-01..E2E-05. Storage is checked with `docker compose exec` (psql, the JSON copy), mail through the Mailpit API, and the workbook with a built-in zip reader. | yes |
| Architecture | backend | `src/test/java/.../architecture/ArchitectureTest.java`: the 5 ArchUnit rules of specification S2 (6 rule fields) | JUnit (ArchUnit engine) | no |
| Unit | backend | `src/test/java/.../{domain,config,application,web,adapter}/` | JUnit 6, AssertJ, Mockito; JDK `HttpServer` for siteverify, `@TempDir` for files | no |
| Integration | backend | `web/SecurityWebTest.java`: the MVC slice with the real security chain and filters, use cases mocked | `@WebMvcTest` + MockMvc (`@EnableWebSecurity` in the test configuration, because Boot 4 leaves security out of the slice) | no |
| Unit / component | frontend | `src/*.test.ts(x)` | Vitest, Testing Library, `fetch` and `grecaptcha` stubbed | no |

The frozen set is every file under the `acceptance` and `e2e` paths above, including their `support/` harness files. It is listed in `docs/03_acceptance-manifest.sha256`. Supporting files that are not frozen: `docker-compose.yml`, the Dockerfiles, `nginx.conf`, `config/conference.json`, `playwright.config.ts` and `vite.config.ts`.

### Running

- Backend: `cd 02_output/backend && ./mvnw -B test`. It needs Docker; Testcontainers finds Docker Desktop by itself. The tests must not run as root, because AC-005-03 makes a directory read-only and asserts that it is.
- Frontend: `cd 02_output/frontend && nvm use && npm test`.
- End-to-end: first `./backend/mvnw -B -f backend/pom.xml package -DskipTests`, then `docker compose --env-file ../.env up -d --build --wait` in `02_output`. Then, in `02_output/frontend`, export `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` and `ORGANIZER_EMAILS` from `.env` without printing them (`working-rules.md`, Secrets) and run `npx playwright test`. `E2E_BASE_URL` and `E2E_MAILPIT_URL` override `http://127.0.0.1:8080` and `:8025`. In this run the stack ran as `COMPOSE_PROJECT_NAME=registration-sdd-run2`, because a volume of an earlier run on this machine uses the default project name `registration`. `FRONTEND_HOST_PORT` and `MAILPIT_HOST_PORT` move the published ports.

### Choices the tests make (from the specification, not new behaviour)

- "Rejected" is checked as 400, the expected `fieldErrors[].field`, no row, an empty JSON copy directory and no message in Mailpit (`AcceptanceTestBase.assertRejected`).
- AC-001-07 and AC-002-06 run in test mode (token `test-fail`) and also on the production path against the mocked siteverify (`RecaptchaVerificationAcceptanceTest`, DoD-P05). The production path covers an accepted token, a rejected token, an HTTP 500 from the verifier, and the test-mode token while test mode is off.
- AC-005-03 makes the JSON copy directory read-only during the request and expects a 5xx with no row (specification S4.6).
- AC-008-04 sends the request through the trusted proxy headers (`X-Forwarded-For: 203.0.113.10`). It first checks a control: the same client with `X-Forwarded-Proto: https` gets 200. The plain-HTTP request must then get a 4xx with no data. The exact status is not frozen, because D-11 is pending review.
- AC-003-02 and AC-003-03 start a second backend instance with another configuration file. AC-003-03 passes only if startup throws.
- E2E-03 makes the real backend reject a submission by adding the inactive `ws-legacy` to the form configuration the browser receives (`page.route`). The backend answers `INACTIVE_OPTION`.
- E2E-05 creates its own registration through the API first, so it does not depend on the order of the tests.

### Notes for the implementation (phase 4)

- The form must set `noValidate`. Otherwise the browser's own email check blocks the contract's messages (AC-004-03, E2E-03).
- The JSON attachment must arrive byte for byte equal to the stored copy (AC-007-02). Send it base64-encoded, so that SMTP does not rewrite line endings.
- Export cells must be string cells with the exact header texts of specification S4.8, `Registered at` and `Consents` among them.
- `server.forward-headers-strategy=native` is needed for AC-008-04 (specification S7.2).

## Changes to frozen tests

| Date | Decision | Commit | Files | Change | Re-frozen in |
|---|---|---|---|---|---|
| 2026-10-27 | D-16 (human-approved, option 1) | `0f86268` | `AcceptanceTestBase.java` and the 9 backend acceptance classes | Spring Framework 7.0.9 registers a base class's `@DynamicPropertySource` after the subclass's, so the per-class overrides of `RecaptchaVerificationAcceptanceTest` (production reCAPTCHA path) and `MailServerDownAcceptanceTest` (SMTP down) were silently ignored: 4 tests could not pass, and AC-006-03 passed without the mail server being down. The base now offers `register(registry, overrides)` and each concrete class declares one `@DynamicPropertySource` calling it. No assertion, input or expected value changed. After the change: 89/89 backend acceptance tests pass, and the mail-down run logs "email not sent" (`out/logs/04_backend-run-3.log`). | the manifest commit after `0f86268` |

## Measurement exclusions

Decided from `project/02_design/quality-requirements.md` ("Mutation scope"), with no exclusion beyond it. The table was written after the first acceptance tests rather than before them (divergence recorded in `run-log.json`), but it adds nothing to the inputs' defaults.

| Path | Reason |
|---|---|
| `02_output/backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | Application entry point (`@SpringBootApplication`), no logic |
| `02_output/frontend/src/main.tsx` | Bootstrap file that only mounts the app |
| `02_output/frontend/src/**/*.d.ts` | Type declarations |
| Test files: `src/test/**`, `src/**/*.test.{ts,tsx}`, `src/acceptance/**`, `e2e/**` | Test code never counts |

## Phase 3 run (on the bootstrap skeleton, before any production code)

Run on 2026-10-27 against commits up to `6ec6548`. Logs are in `out/logs/03_backend-acceptance-run.log`, `03_frontend-acceptance-run.log` and `03_e2e-run.log`.

| Suite | Tests | Passed | Failed | Reason for the failures |
|---|---|---|---|---|
| Backend acceptance | 89 | 0 | 89 | Behaviour missing. The skeleton's default Spring Security answers 401 to every `/api` call, so every expected 201, 200 or 400 gets 401. AC-003-03 fails because the skeleton starts with an invalid configuration. No context or startup error, and no harness error. |
| Frontend acceptance | 7 | 0 | 7 | Behaviour missing: there is no `Registration type` radio, so no form is rendered. |
| End-to-end | 5 | 0 | 5 | Behaviour missing. E2E-01..04 time out waiting for the `Registration type` radio. E2E-05 gets 401 instead of 201 when creating its registration. The stack itself was healthy: all four services passed their health checks. |

No test passes on bootstrap code. AC-008-02, which on its own would match the skeleton's default 401, fails at its precondition (registering through the API), and AC-008-04 has its HTTPS control.

## First complete run (before any fix)

Phase 5 started with the full suite on the final production code of phase 4, before any unit test was written (`out/logs/05_backend-first-run.log`, `05_frontend-first-run.log`; end-to-end from the phase 4 gate, `04_e2e-gate.log`). Backend 89/89, frontend 7/7, end-to-end 5/5: no failure, so nothing to classify. No production code changed in phase 5.

## Final run (phase 6)

Run on 2026-10-27 on the code of phase 5 (no production code or test changed since): backend 301/301 (acceptance 89, unit 201, integration 5, architecture 6; `out/logs/06_backend-final-run.log`), frontend 74/74 (acceptance 7, unit/component 67; `06_frontend-final-run.log`), end-to-end 5/5 against the rebuilt stack (`06_e2e-final-run.log`). No failure, no flaky or skipped test. Coverage and mutation are not re-measured (`measurement-scope.md`: nothing changed since phase 5).

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | unit | 201 / 0 unit, 5 / 0 integration (MVC slice), 6 / 0 architecture | 93.47 % (830/888), with the slice and architecture tests | 95.31 % (264/277) | n/a | JUnit 6, JaCoCo 0.8.12 (`out/logs/05_coverage/backend-unit.csv`) |
| backend | non-unit | 89 / 0 acceptance; 5 / 0 end-to-end (not instrumented, D-17) | 90.54 % (804/888) | 71.48 % (198/277) | n/a | JUnit 6, JaCoCo 0.8.12 (`out/logs/05_backend-first-run.log`, same production code) |
| backend | all levels | 301 / 0 | **99.44 %** (883/888) | 96.39 % (267/277) | raw **92.71 %** (318 killed of 343; 23 survived, 2 no coverage), adjusted 92.71 % (0 equivalent), covered 93.26 % (318/341); scope `si.konferenca.registration.*` minus `RegistrationApplication`; tests: the full JUnit suite (acceptance, unit, integration, architecture) | JaCoCo 0.8.12 (`out/logs/05_coverage/backend-all.csv`), PIT 1.30.0 + junit5 plugin 1.2.3 (`out/logs/05_mutation/backend-pit-mutations.xml`) |
| frontend | unit | 67 / 0 | 100 % (156/156) | 97.01 % (130/134) | n/a | Vitest 4.1.11, v8 (`out/logs/05_coverage/frontend-unit.json`) |
| frontend | non-unit | 7 / 0 acceptance; 5 / 0 end-to-end (not instrumented, D-17) | 76.28 % (119/156) | 62.68 % (84/134) | n/a | Vitest 4.1.11, v8 (`out/logs/05_frontend-first-run.log`, same production code) |
| frontend | all levels | 74 / 0 | **100 %** (156/156) | 97.01 % (130/134) | raw **90.30 %** (428 killed of 474 generated; 40 survived, 4 no coverage, 2 runtime errors counted as not killed; Stryker's own score without the 2 errors 90.68 %), adjusted 90.30 % (0 equivalent), covered 91.45 % (428/468); scope `src/**/*.{ts,tsx}` minus `main.tsx`, `*.d.ts`, tests; tests: the full Vitest suite (acceptance, unit/component) | Vitest v8, Stryker 10.0.0 vitest-runner (`out/logs/05_mutation/frontend-stryker-mutation.json`) |

Quality gate (`project/02_design/quality-requirements.md`): line >= 95 %, adjusted mutation >= 90 %. Both components meet it with the one full mutation run; no survivor round was needed (`mutationRounds` 0), no survivor is counted as equivalent, and no further tests were added after the runs. Tool checks before the full runs: PIT on `EmailAddress` with its unit test killed 9/10 (`05_pit-sanity.log`), Stryker on `validation.ts` scored 94.29 % (`05_stryker-sanity.log`). PIT ran 1335 tests for 343 mutants; Stryker 4.15 tests per mutant.

Code size (`aldanial/cloc:2.10`, code lines): backend production 2084 (61 files, sources and resources), backend tests 3598 (31); frontend production 659 (11: `src` without tests, `index.html`, `nginx.conf`), frontend tests 1641 (18, incl. end-to-end).
