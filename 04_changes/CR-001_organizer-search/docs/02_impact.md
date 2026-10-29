# Impact: CR-001

> Written in: phase 2 of the change run, before any production code · Source: `cr/change.md`, `cr/acceptance-criteria.md`, the baseline code at `4d664d2` · Agent: writes (predictions are never edited; additions go under "Found during build")

Paths are relative to `02_output/`. A folder row covers the files under it.

| Path | Action (add / modify / delete) | Because (ids) |
|---|---|---|
| `backend/src/main/java/si/konferenca/registration/domain/SearchText.java` | add | BR-11, OQ-07, AC-009-08..16 |
| `backend/src/main/java/si/konferenca/registration/domain/RegistrationFilter.java` | add | BR-11, BR-12, AC-009-18..20 |
| `backend/src/main/java/si/konferenca/registration/domain/SearchRejectedException.java` | add | BR-11, BR-12, AC-009-16, AC-009-22 |
| `backend/src/main/java/si/konferenca/registration/domain/ErrorCode.java` | modify | AC-009-22 (codes for an unknown type and an option of another category) |
| `backend/src/main/java/si/konferenca/registration/application/SearchRegistrations.java` | add | US-009, BR-12, BR-13, AC-009-06, AC-009-21..25 |
| `backend/src/main/java/si/konferenca/registration/application/SearchCriteria.java` | add | BR-11, BR-12 |
| `backend/src/main/java/si/konferenca/registration/application/SearchResult.java` | add | BR-13, AC-009-24, AC-009-25 |
| `backend/src/main/java/si/konferenca/registration/web/OrganizerSearchController.java` | add | US-009, SR-07, SR-09, AC-009-03, AC-009-07 |
| `backend/src/main/java/si/konferenca/registration/web/OrganizerSearchResponse.java` | add | BR-10, SR-07, AC-009-07, AC-009-24 |
| `backend/src/main/java/si/konferenca/registration/web/SecurityConfiguration.java` | modify | SR-09, BR-08, AC-009-03 |
| `backend/src/main/java/si/konferenca/registration/web/JsonAuthenticationEntryPoint.java` | modify | AC-009-03, AC-009-04, D-26 |
| `backend/src/main/java/si/konferenca/registration/web/HttpsOnlyFilter.java` | modify | SR-06, AC-009-05 |
| `backend/src/main/java/si/konferenca/registration/web/RateLimitFilter.java` | modify | SR-03, D-23 |
| `backend/src/main/java/si/konferenca/registration/web/ApiExceptionHandler.java` | modify | AC-009-16, AC-009-22 |
| `backend/src/main/java/si/konferenca/registration/web/FieldMessages.java` | modify | AC-009-16, AC-009-22 |
| `backend/src/main/java/si/konferenca/registration/config/AppProperties.java` | modify | SR-03, D-23 |
| `backend/src/main/resources/application.yml` | modify | SR-03, D-23 |
| `backend/src/test/java/si/konferenca/registration/acceptance/OrganizerSearchAcceptanceTest.java` | add | AC-009-03, 05..16, 18..20, 22, 24, 25, SR-07 |
| `backend/src/test/java/si/konferenca/registration/acceptance/OrganizerSearchRateLimitAcceptanceTest.java` | add | SR-03 |
| `backend/src/test/java/si/konferenca/registration/acceptance/support/OrganizerSearch.java` | add | harness for the search API |
| `backend/src/test/java/si/konferenca/registration/domain/` | add / modify | unit tests of `SearchText`, `RegistrationFilter` (phase 5) |
| `backend/src/test/java/si/konferenca/registration/application/` | add / modify | unit tests of `SearchRegistrations` (phase 5) |
| `backend/src/test/java/si/konferenca/registration/web/` | add / modify | unit and slice tests of the controller, filters, entry point, messages (phase 5) |
| `frontend/src/main.tsx` | modify | BR-09 (mounts the page chosen by path) |
| `frontend/src/Root.tsx` | add | BR-09, AC-009-01 |
| `frontend/src/OrganizerPage.tsx` | add | US-009, BR-09, SR-09, AC-009-01, 04, 17, 26, 27, 30 |
| `frontend/src/OrganizerResults.tsx` | add | BR-10, BR-13, AC-009-06, 07, 21, 23, 24, 28, 29 |
| `frontend/src/api.ts` | modify | AR-01, SR-09 (the only module that calls `fetch`) |
| `frontend/src/types.ts` | modify | the search contract types |
| `frontend/src/messages.ts` | modify | organizer page texts |
| `frontend/src/styles.css` | modify | organizer page layout |
| `frontend/src/acceptance/us009-organizer-search.test.tsx` | add | AC-009-01, 02, 04, 06, 07, 17, 21, 23, 24, 26..30 |
| `frontend/src/acceptance/support/organizer.ts` | add | harness: stubbed search API |
| `frontend/src/*.test.ts`, `frontend/src/*.test.tsx` | add / modify | unit and component tests (phase 5) |
| `frontend/e2e/e2e-06-organizer-search.spec.ts` | add | E2E-06 |

## Baseline test files changed by MODIFIED or REMOVED ids

None: CR-001 has no MODIFIED or REMOVED criterion or scenario.

## Found during build

| Path | Action | Reason |
|---|---|---|
| `backend/src/test/java/si/konferenca/registration/acceptance/OrganizerSearchTextAcceptanceTest.java` | add | Phase 3: the search-text criteria (AC-009-08..16) split from `OrganizerSearchAcceptanceTest` to keep each commit under 400 lines |
| `backend/src/test/java/si/konferenca/registration/acceptance/support/OrganizerSearchTestBase.java` | add | Phase 3: the harness is a base class for the three search classes, named instead of the predicted `support/OrganizerSearch.java` |
| `backend/src/test/resources/spring.properties` | add | Phase 3: test context cache capped; three more acceptance contexts exceeded PostgreSQL's 100 connections |
| `frontend/src/acceptance/us009-organizer-filters.test.tsx` | add | Phase 3: split from `us009-organizer-search.test.tsx` to keep each commit under 400 lines |
| `frontend/src/App.tsx` | modify | Phase 3: `App` routes by path (S6.3), so the tests can open `/organizer` through the existing entry; replaces the predicted `Root.tsx` |
| `frontend/src/routes.ts` | add | Phase 4: the path check `isOrganizerPath` in its own module, since a component file exports only components (eslint `react-refresh`); `Root.tsx` and the `main.tsx` change were not needed |
