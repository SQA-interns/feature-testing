# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Through | Frozen |
|---|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` (`Us0nn…AcceptanceTest`, harness in `support/`), config `backend/src/test/resources/acceptance/conference.json` | JUnit 6 via `./mvnw test` | real HTTP to the backend on a random port (`java.net.http`), Testcontainers PostgreSQL (`postgres:16.15-alpine`) and Mailpit (`axllent/mailpit:v1.31.1`); the database is read with plain JDBC, emails through the Mailpit API, JSON copies from the configured directory | yes |
| Acceptance | frontend | `frontend/tests/acceptance/` | Vitest (jsdom) via `npm test` | the whole `App` rendered with Testing Library; `fetch` replaced by a fake backend that answers as `api-contract.md` | yes |
| End-to-end | all | `frontend/tests/e2e/` (`e2e-0n-….spec.ts`, helpers in `support.ts`) | Playwright Chromium via `npm run test:e2e` | the browser against the local stack (`docker compose up` in `02_output`), frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:8080`, Mailpit `http://127.0.0.1:8025`; PostgreSQL and the JSON copy volume through `docker compose exec` | yes |
| Unit / integration, architecture | backend, frontend | written in phase 5 | | | no |

Backend acceptance contexts (one Spring context each): standard settings; SMTP port with nothing listening (AC-006-03); JSON copy directory under a regular file (AC-005-03); reCAPTCHA test mode off against a local stand-in for the siteverify endpoint (AC-001-07, AC-002-06, DoD-P05). AC-003-02 and AC-003-03 start the backend a further time with their own options file.

Prerequisites the frozen tests fix for the build (phase 4):
- settings are read from the names in `docs/02_specification.md` section 9 (the tests set `CONFERENCE_CONFIG`, `JSON_COPY_DIR`, `SMTP_HOST`, `SMTP_PORT`, `CAPTCHA_TEST_MODE`, `RECAPTCHA_VERIFY_URL`, `ORGANIZER_*`, `RATE_LIMIT_PER_MINUTE`, … as Spring properties, and `spring.datasource.*` directly);
- the UI uses the accessible names of `docs/02_contracts/ui.md`;
- the local stack: compose services `db` (database and user `registration`) and `backend` (JSON copies in `/data/json-copies`); frontend published on `127.0.0.1:8081`; the shipped `conference.json` has at least one active option per category for external participants and a mandatory consent;
- the e2e run needs `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` exported from `.env` and `CAPTCHA_TEST_MODE=true` on the stack.

## Measurement exclusions

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`quality-requirements.md` mutation scope) |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app |
| `frontend/src/test-setup.ts` | test setup (jest-dom matchers), not shipped code |

## Phase 3 run (bootstrap code, before any production code)

| Suite | Tests | Passed | Failed | Reason |
|---|---|---|---|---|
| backend acceptance | 98 | 0 | 98 | every request answered 401 by the bootstrap's default Spring Security (95 tests); AC-003-03 control start serves 401 (3); AC-005-03 finds no schema (1). No error, no setup failure: all contexts and containers started (`out/logs/03_backend-acceptance-run.log`) |
| frontend acceptance | 12 | 0 | 12 | the bootstrap page has no registration type choice (8) and no organizer login (4) (`out/logs/03_frontend-acceptance-run.log`) |
| end-to-end | 6 | 0 | 6 | against the bootstrap frontend served on 8081: no registration type choice (5); E2E-02 first reads `GET /api/form-config` and finds no backend on 8080, because the local stack (`docker-compose.yml`) is a phase 4 deliverable (`out/logs/03_e2e-run.log`) |

No test passes on bootstrap code. Acceptance criteria: 48; every criterion has at least one test (test names carry the AC id); every e2e scenario has exactly one test.

## First complete run (before any fix)

Phase 5 start, 2026-10-22, on the phase 4 code (commit `7d22f72`), before any unit test was written:

| Suite | Passed | Failed | Log |
|---|---|---|---|
| backend acceptance (`./mvnw test`) | 98 | 0 | `out/logs/04_gate-backend.log` |
| frontend acceptance (`npx vitest run`) | 12 | 0 | phase 4 gate |
| end-to-end (`npx playwright test`, local stack) | 6 | 0 | `out/logs/04_e2e-run.log` |

No failure to classify. Defects found earlier, while building in phase 4 (frozen tests failing on new code, fixed in the code): JSON attachment not byte-identical on the Linux container (E2E-01, fixed in `ea12aac`).

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
