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

## Equivalent mutants

None yet (phase 5).
