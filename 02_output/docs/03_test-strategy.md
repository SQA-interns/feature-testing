# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Through | Frozen |
|---|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` (12 test classes, 102 tests) | JUnit 6 via `./mvnw test` | HTTP to the app on a random port (`@SpringBootTest`); PostgreSQL 16.15 and Mailpit 1.31.1 in Testcontainers; a JDK `HttpServer` as the mocked reCAPTCHA verify endpoint | yes |
| Acceptance | frontend | `frontend/src/acceptance/` (2 test files, 9 tests) | Vitest 4 + Testing Library, jsdom | the whole `<App />`; `fetch` replaced at the `/api` boundary | yes |
| End-to-end | all | `frontend/e2e/*.e2e.spec.ts` (5 tests, one per E2E-01 … E2E-05) | Playwright 1.63, Chromium | the browser and REST API against `docker compose` (`02_output/docker-compose.yml`); PostgreSQL via `psql` in the container, JSON copies via the backend container, mail via the Mailpit API, the workbook via a zip/XML reader | yes |
| Unit, integration, architecture | backend, frontend | any other test path | as above | internals | no (phase 5) |

AC coverage: every one of the 35 criteria is named by at least one acceptance test (backend for the API criteria, frontend for AC-001-08, AC-002-07, AC-004-01 … 04). DoD-P05 is covered by `CaptchaVerificationAcceptanceTest`.

### Harness assumptions (from the contracts; the implementation follows them)

| # | Assumption | Source |
|---|---|---|
| H-1 | The backend reads its settings by the names of spec section 4 (`DB_URL`, `SMTP_PORT`, `CONFERENCE_CONFIG_PATH`, …) as Spring placeholders, so tests pass them as properties. | spec §4 |
| H-2 | `CONFERENCE_CONFIG_PATH` accepts a plain absolute file path (and `classpath:` for the default). | spec §3.1 |
| H-3 | The JSON copy directory is not required at start; a write into it fails when the path is a regular file. | spec §3.2 step 5 |
| H-4 | The backend trusts `X-Forwarded-For` / `X-Forwarded-Proto` from loopback and private proxies (`server.forward-headers-strategy=native`). | spec §3.4 |
| H-5 | Option groups are `fieldset`/`legend` groups named "Workshops", "Events", "Meals", "Other activities"; option and consent checkboxes are labelled by their name or text. | interfaces "Registration form" 4, accessible names |
| H-6 | In test mode the captcha is a checkbox whose name contains "test mode" and yields the token `test-pass`. | interfaces "reCAPTCHA" |
| H-7 | A field error is linked to its input with `aria-describedby` and has `role="alert"`; an input without an error has no description. | interfaces "Registration form" |
| H-8 | Mailpit lists envelope-only recipients as `Bcc`; the injection test therefore checks the recipient set, not the absence of a `Bcc` header. | checked against Mailpit 1.31.1 in phase 3 |
| H-9 | The e2e run gets `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD` from the environment (exported from `.env` without printing). | working-rules "Secrets" |

### Self-check before the freeze

- Mailpit API field names, attachment download and delete-all were verified against a real `axllent/mailpit:v1.31.1` message (python `smtplib`).
- The e2e workbook reader was run on a workbook written by POI 5.5.1 (shared strings, empty cells, `&`, `<`, č š ž): rows came back exactly.
- Timestamps are read with `getObject(…, OffsetDateTime.class)`; JSON-copy vs database time is compared with 1 ms tolerance (PostgreSQL rounds to microseconds).
- The AC-003-03 cases first start the same settings with a valid file, so a refusal cannot come from another cause; on the skeleton they fail on that control start.
- Byte comparisons use `isEqualTo(byte[])` / `Buffer.equals` (whole content), never element-wise `doesNotContain`.

## Measurement exclusions

Fixed in phase 0 in the tool configuration (`backend/pom.xml` JaCoCo and PIT, `frontend/vite.config.ts` coverage), before any test was written; they equal the defaults of `quality-requirements.md` "Mutation scope".

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | `@SpringBootApplication` entry point, no logic |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app |
| `frontend/src/**/*.d.ts` | type declarations |
| `frontend/src/**/*.test.{ts,tsx}`, `frontend/src/acceptance/**`, `frontend/src/test/**`, `frontend/e2e/**` | test code |

## First complete run (before any fix)

Phase 3 run on the bootstrap skeleton (commit before the freeze), logs in `out/logs/`.

| Suite | Passed | Failed | Reason (one line per group) |
|---|---|---|---|
| backend acceptance (`phase3-backend-first-run.log`) | 0 | 102 | the application context cannot start: the skeleton does not read the spec §4 settings (`DB_URL`), so no datasource ("Failed to determine a suitable driver class"); every behaviour is missing |
| frontend acceptance (vitest) | 0 | 9 | the skeleton page has no registration-type radio buttons (`findByRole("radio")` fails) |
| end-to-end (`phase3-e2e-first-run.log`) | 0 | 5 | E2E-01, 03, 04: no registration-type radio on the page; E2E-02: `/api/form-config` answers the proxy error page because the backend does not start; E2E-05: backend not listening on 8080 for the same reason |

No test passes on bootstrap code alone. Containers, compilation, type check, lint and formatting of all tests succeed, so no failure is a build or harness error.

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
