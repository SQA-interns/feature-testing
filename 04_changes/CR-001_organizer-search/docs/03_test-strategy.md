# Test strategy: CR-001

> Written in: phase 3 of the change run, updated in phase 5 · Source: `cr/acceptance-criteria.md`, `02_output/docs/02_contracts/`, `general/quality/test-strategy.md` · Agent: writes

Adds to the baseline's `02_output/docs/03_test-strategy.md` (levels, tools, harness, measurement exclusions), which still holds.

## Acceptance and end-to-end tests of the CR

| Level | Files (under `02_output/`) | Criteria |
|---|---|---|
| Backend acceptance (JUnit, black box over HTTP against PostgreSQL and Mailpit containers) | `backend/src/test/java/si/konferenca/registration/acceptance/OrganizerSearchAcceptanceTest.java` | AC-009-03, 05, 06, 07, 18, 19, 20, 22, 23, 24, 25 |
| | `.../acceptance/OrganizerSearchTextAcceptanceTest.java` | AC-009-08..16 |
| | `.../acceptance/OrganizerSearchRateLimitAcceptanceTest.java` | SR-03 (MODIFIED) |
| | `.../acceptance/support/OrganizerSearchTestBase.java` | harness: query building, organizer credentials, result readers |
| Frontend acceptance (Vitest + Testing Library, `fetch` stubbed at the contract) | `frontend/src/acceptance/us009-organizer-search.test.tsx` | AC-009-01, 02, 04, 06, 07, 28, 29, 30 |
| | `frontend/src/acceptance/us009-organizer-filters.test.tsx` | AC-009-17, 21, 23, 24, 26, 27 |
| | `frontend/src/acceptance/support/organizer.ts` | harness: stubbed search API, sample rows, login and select helpers |
| End-to-end (Playwright against the local stack) | `frontend/e2e/e2e-06-organizer-search.spec.ts` | E2E-06 |

Every ADDED criterion has at least one test, named by its id. There is exactly one end-to-end test for E2E-06 and no other new one. No baseline test file changes (no MODIFIED or REMOVED id). The frozen set is the eight files above, listed in `docs/03_acceptance-manifest.sha256`.

Harness notes:

- The frontend tests open the page as a browser does: `window.history.pushState` to `/organizer`, then render `App` (S6.3). A test that imported a not-yet-written module would fail with a build error, not for a behavioural reason.
- Backend acceptance classes set `RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE=10000`, except the rate-limit class, which sets 3 and sends 7 calls, so the limit is exceeded even if a one-minute window ends during the loop.
- E2E-06 counts exactly two registrations, so it empties the `registration` table of the local stack first (`TRUNCATE ... CASCADE` through `docker compose exec`); it runs last (file order, one worker), and E2E-05 counts against the table itself.
- E2E-06 clears the search and presses Search before it sets the type filter, so it does not depend on D-25.
- Supporting file, not frozen: `backend/src/test/resources/spring.properties` sets `spring.test.context.cache.maxSize=4`. Each acceptance class has its own `@DynamicPropertySource` (D-16), so each gets a context with a 10-connection pool; with the three new classes the cached contexts exceeded PostgreSQL's 100 connections and 8 baseline tests failed with "too many clients already" (`out/logs/cr-001_03_backend-full-run.log`, first attempt). Capping the cache closes evicted contexts and their pools.

## First run (phase 3)

Run on 2026-10-29 against the baseline production code (`out/logs/cr-001_03_backend-full-run.log`, `cr-001_03_frontend-full-run.log`, `cr-001_03_e2e-full-run.log`; first runs of the new files `cr-001_03_backend-acceptance-first.log`, `cr-001_03_frontend-acceptance-first.log`, `cr-001_03_e2e06-first.log`).

| Level | New tests | Pass | Fail | Reason |
|---|---|---|---|---|
| Backend acceptance | 34 | 3 | 31 | Behaviour missing: the path `/api/organizer/registrations` is denied (403, `denyAll`) for valid credentials; the rate-limit test gets only 401s (no organizer-search group). The 3 passing tests are AC-009-03 (no, wrong-password and wrong-username credentials): the baseline's HTTP Basic filter already answers 401 with no data for any path before authorization. |
| Frontend acceptance | 16 | 2 | 14 | Behaviour missing: `/organizer` renders the registration page, so there is no `Organizer registrations` heading and no `Username` field. The 2 passing tests are AC-009-02 (both types): the baseline registration page already has no link to `/organizer`. |
| End-to-end | 1 | 0 | 1 | Behaviour missing: no `Organizer registrations` heading at `/organizer`. |

Baseline tests in the same runs: backend 301/301, frontend 74/74, end-to-end 5/5 (E2E-01..05). Full suite: 380 + 3 + 2 = 385 passed, 31 + 14 + 1 = 46 failed, all in tests of ADDED items.

## Phase 5: first full run and quality gate

Both components changed since `4d664d2`, so the gate applies to both, with the baseline's scope, tools and tests.

First full run on the final production code, before any unit test (`out/logs/cr-001_05_backend-first-run.log`, `cr-001_05_frontend-first-run.log`; end-to-end from the phase 4 run `cr-001_04_e2e-run-1.log` on the same code): backend 335/335, frontend 90/90, end-to-end 6/6. No failure, so nothing to classify; `failedBaseline` 0.

| Component | Line (all levels) | Branch | Mutation, first full run | Rounds | Mutation, final | Changed lines since base |
|---|---|---|---|---|---|---|
| backend | 99.53 % | 96.86 % | 410/431 = 95.13 % (`cr-001_05_pit-full-run.log`) | 0 | 95.13 % | 92/92 killed |
| frontend | 99.63 % (after round 1; 95.23 % before) | 96.33 % | 625/755 = 82.78 % (`cr-001_05_stryker-full-run.log`) | 1 | 689/755 = 91.26 % | 261/281 killed |

Line coverage met the threshold for both before any unit test, so mutation ran once per component. The backend met the gate on its full run; no backend test was added. Frontend round 1 added `src/api.test.ts` cases for `searchRegistrations` and `basicAuthorization`, `src/routes.test.ts` and `src/OrganizerPage.test.tsx` (page states, 400/401/failure handling, stale replies, filter names), then re-ran only the three mutated files (`cr-001_05_stryker-round-1.log`):

| File | Full run | Round 1 |
|---|---|---|
| `src/OrganizerPage.tsx` | 98/153 | 137/153 |
| `src/api.ts` | 69/94 | 92/94 |
| `src/routes.ts` | 8/10 | 10/10 |
| other files (not re-run) | 450/498 | 450/498 |
| frontend | 625/755 = 82.78 % | 689/755 = 91.26 % |

The recomputed score is 1.26 points above the threshold, so no confirming full run followed. Final suite: backend 335, frontend 121 (acceptance 23, unit/component 98), end-to-end 6.
