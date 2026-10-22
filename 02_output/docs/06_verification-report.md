# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-22 at commit state after phase 5 (no production code or test changed in phase 6). Raw outputs are in `out/logs/06_*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Backend `./mvnw verify`: 227 tests, 0 failures (123 unit, 6 architecture, 98 acceptance; `out/logs/06_backend-verify.log`); frontend `npx vitest run`: 62 tests, 0 failures (`out/logs/06_frontend-test.log`); e2e `npx playwright test` on the compose stack: 6 passed (`out/logs/06_e2e-run.log`). All 37 hashes of `docs/03_acceptance-manifest.sha256` match after LF normalisation. |
| DoD-02 | met | Spotless check, PMD (0 violations), CPD, SpotBugs (0 bugs after the D-13 exclusions) in `out/logs/06_backend-verify.log`; Prettier, ESLint, `tsc --noEmit` clean (`out/logs/06_frontend-check.log`); jscpd 0 clones; Semgrep findings triaged below (F-01 … F-03). |
| DoD-03 | met | Backend: line 98.32 % (all levels), mutation 91.20 % raw = adjusted (373/409, PIT 1.30.0, full suite of 227 tests, 0 survivor rounds). Frontend: line 100 %, mutation 93.81 % raw = adjusted (515/549, Stryker 10.0.0, 62 tests, 1 survivor round). Scopes, levels and per-file numbers: `docs/03_test-strategy.md`; no code or test changed since phase 5, so nothing was re-measured. |
| DoD-04 | met | `ArchitectureTest` (6 ArchUnit rules: domain free of other layers and frameworks, application only on domain, adapters independent of config and of each other, no package cycles) passes. AR-01: the frontend calls only `/api/...` (`src/api.ts`); AR-05: AC-005-03 test; AR-06: Flyway `V1__registration.sql` with `ddl-auto=validate`; AR-07: the frontend bundle holds no key, the site key comes from `GET /api/form-config`; AR-09: `docs/02_contracts/validate-contracts.mjs` and the acceptance tests use the fixed paths, fields and codes; AR-04: AC-003-01 … 03 tests. |
| DoD-05 | met | No Critical or High finding (Findings below). |
| DoD-06 | met | Runtime demonstration below (`out/logs/06_runtime-demo.log`). |
| DoD-07 | met | Traceability table below: 48 criteria, none without a test or a commit. |
| DoD-08 | phase 7 | The READMEs are a phase 7 deliverable; their clean-checkout check is recorded in phase 7 (`docs/release-notes.md`). |
| DoD-09 | phase 7 | Manual-test list in `docs/release-notes.md` (phase 7). |
| DoD-10 | met | Decisions D-01 … D-16 all resolved except D-11 (pending review, option applied); `docs/00_input-manifest.sha256` (41 files) matches; `git diff 0d62004 -- 01_input AGENTS.md README.md Run_Guide.md 03_statistics/metrics.md 03_statistics/run-log.template.json 03_statistics/tools 03_statistics/usage.md` is empty. |
| DoD-11 | met | Phase 3 commits (`6dfdae7` … `5938486`) change no file under `backend/src/main` or `frontend/src` except tests; the freeze commit `5938486` adds only the manifest; all 37 listed files were committed before it; build commits name their US/AC IDs (trace table); every manifest hash matches. |
| DoD-P01 | met | Runtime demo: one external and one student registration through the frontend proxy, both HTTP 201; E2E-01 and E2E-02 pass. |
| DoD-P02 | met | Runtime demo: both rows in `registration` (psql) and both `<id>.json` files in `/data/json-copies`. |
| DoD-P03 | met | Runtime demo: Mailpit holds for each registration the participant confirmation and the organizer notification with one attachment; E2E-01 compares the attachment with the stored copy. |
| DoD-P04 | met | Runtime demo: export with credentials 200, `.xlsx` with `xl/worksheets/sheet1.xml`; without or with wrong credentials 401; E2E-05 parses the workbook. |
| DoD-P05 | met | `Us001CaptchaVerificationAcceptanceTest`, `Us002CaptchaVerificationAcceptanceTest` run the production verifier against a local siteverify stand-in: accepted token stored, rejected token 400 with nothing stored, secret sent; `CaptchaVerifierTest` covers failure answers. |
| DoD-P06 | met | Runtime demo: search by the external's last name lists exactly it; type `STUDENT` lists only students; without credentials 401; organizer page served; E2E-06 passes in the browser. |

## Runtime demonstration

Stack: `docker compose --env-file ../.env up` in `02_output` (images built from the current code), local environment (`APP_ENVIRONMENT=local`, captcha test mode, organizer HTTPS-only off as `environments.md` allows on local plain HTTP). Log: `out/logs/06_runtime-demo.log` (organizer addresses masked).

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local compose | start; `docker compose ps` health of db, mailpit, backend, frontend | all healthy |
| backend | local | `/actuator/health`, `/liveness`, `/readiness` (NFR-04) | `UP`, readiness includes database and JSON copy volume |
| frontend + backend | local | external and student registration with Slovenian text through `127.0.0.1:8081/api` | 201 both; rows and JSON copies present; 2 emails each in Mailpit (confirmation, organizer notification with JSON attachment) |
| backend | local | export with, without and with wrong credentials | 200 workbook / 401 / 401 |
| backend | local | search by last name, by type, without credentials | exactly the registration / only students / 401 |
| backend | local | registration with a rejected captcha token | 400 `captchaToken`, nothing stored |
| frontend, backend | local | response headers | frontend CSP with reCAPTCHA origins, `X-Frame-Options: DENY`, `nosniff`, `no-referrer`; backend `default-src 'none'`, `no-store` |
| all | local | `docker compose down` (0 containers) then `up` (NFR-02) | 16 rows and 16 JSON copies before and after; demo registration still readable |
| backend | local, `ORGANIZER_HTTPS_ONLY=true` | search through the frontend proxy and export from the host over plain HTTP (SR-06) | 403 `protocol` both; 200 again with the setting off |
| frontend, backend | local | container users (SB-11) | `nginx` and `app` (not root) |
| all | local | e2e suite in Chromium | 6 passed |
| backend | local | container log after the demo | 50 lines, no names, emails, credentials, warnings or errors |

Earlier in phase 4 on the same stack: 130 requests to one endpoint within a minute gave 9 × 200 and 121 × 429 after the window filled (SR-03), and a 20 kB body gave 413.

## Security baseline and project requirements

| ID | Implementation | Check |
|---|---|---|
| SB-01 | `RegistrationValidator`, `SearchRequestValidator` on the backend; frontend checks only add convenience | AC-001-02 … 07, AC-002-02 … 06, AC-009-08 tests; unit tests |
| SB-02 | Spring Security: organizer endpoints need role ORGANIZER, everything else public or denied | AC-008-02, AC-009-09; runtime demo 401 |
| SB-03 | password BCrypt-hashed at start-up (`SecurityConfiguration`), only the hash is kept; secrets only from configuration | inspection; `StartupChecksTest` (required settings) |
| SB-04 | TLS by the external nginx in production; organizer access refused over plain HTTP (SR-06) | release notes manual test; runtime demo 403 |
| SB-05 | JPA Criteria with parameters, escaped `LIKE`; React text nodes; plain-text emails | AC-009-02, AC-009-13, AC-006-02; `RegistrationEntityTest.likeWildcardsAreEscaped` |
| SB-06 | `RateLimitFilter` per client and endpoint on all `/api` endpoints, before authentication | `RequestFiltersTest`; runtime 429 |
| SB-07 | `ApiExceptionHandler` and filters answer without internals; logs carry registration ids only | `ErrorHandlingTest`; container log check; AC-004-04 |
| SB-08 | Dependency-Check and `npm audit` | F-07 … F-10: no Critical or High |
| SB-09 | Semgrep, SpotBugs, PMD; Gitleaks | F-01 … F-06 |
| SB-10 | backend headers via Spring Security; frontend headers in `nginx.conf` | runtime demo headers |
| SB-11 | non-root users `app` and `nginx` in the images | runtime demo `id -un` |
| SB-12 | only the BR-01 fields, options and consents are stored (`database.md`) | inspection; AC-005-01 |
| SB-13 | purposes and retention in `security-requirements.md` and OQ-06; deletion instruction in the release notes | release notes |
| SB-14 | consent checkboxes unchecked, mandatory consents enforced on the backend, consent time stored | AC-001-08, AC-002-07, AC-001-06, AC-005-01 |
| SR-01 | captcha verified on the backend for every registration | AC-001-07, AC-002-06, DoD-P05 |
| SR-02 | `StartupChecks`: production refuses test mode or empty keys | `StartupChecksTest.productionRefusesEveryUnsafeSettingAtOnce` |
| SR-03 | `RateLimitFilter`, `RequestSizeFilter` (16 kB default) | `RequestFiltersTest`; runtime 429 and 413 |
| SR-04 | options checked against active configured options per type | AC-001-05, AC-002-04 |
| SR-05 | `MailNotifier` builds headers from configuration and the validated address only; text/plain bodies | AC-006-02; `MailNotifierTest` |
| SR-06 | `HttpsOnlyFilter` before authentication; production refuses `ORGANIZER_HTTPS_ONLY=false` | AC-008-04, AC-009-10; runtime 403; `StartupChecksTest` |
| SR-07 | export columns fixed by the contract; notification body and attachment hold only registration data | AC-008-01, AC-007-02; `PoiWorkbookWriterTest` |
| SR-08 | frontend nginx sets `Host` and `X-Forwarded-For` itself; production proxy settings in the backend README | `nginx.conf` inspection; release notes manual test |
| SR-09 | organizer page renders values as text only | AC-009-13; `OrganizerPage.test.tsx` |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (`out/logs/06_trace-ac.txt`):

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | Us001ExternalRegistrationAcceptanceTest.java, e2e-01-external-registration.spec.ts | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-02 | Us001ExternalRegistrationAcceptanceTest.java | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-03 | Us001ExternalRegistrationAcceptanceTest.java | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-04 | Us001ExternalRegistrationAcceptanceTest.java | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-05 | Us001ExternalRegistrationAcceptanceTest.java | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-06 | Us001ExternalRegistrationAcceptanceTest.java | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-07 | Us001CaptchaVerificationAcceptanceTest.java (3), Us001ExternalRegistrationAcceptanceTest.java (2) | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-001-08 | us001-external-form.test.tsx, e2e-01-external-registration.spec.ts | 02cdabf, 54fd0c5, a15ce4a, cd38e75, eb4cec1, ed38694 (+7) |
| AC-002-01 | Us002StudentRegistrationAcceptanceTest.java, e2e-02-student-registration.spec.ts | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-002-02 | Us002StudentRegistrationAcceptanceTest.java | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-002-03 | Us002StudentRegistrationAcceptanceTest.java | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-002-04 | Us002StudentRegistrationAcceptanceTest.java | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-002-05 | Us002StudentRegistrationAcceptanceTest.java | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-002-06 | Us002CaptchaVerificationAcceptanceTest.java (2), Us002StudentRegistrationAcceptanceTest.java | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-002-07 | us002-student-form.test.tsx (2), e2e-02-student-registration.spec.ts | 2c50fbd, 54fd0c5, a15ce4a, eb4cec1, f0c79df, 9479887 (+1) |
| AC-003-01 | Us003ConferenceOptionsAcceptanceTest.java | cdd2fc0, 1842009, 53201a7, 84ee2d0, f4848f0, abf2d13 |
| AC-003-02 | Us003ConferenceOptionsAcceptanceTest.java | cdd2fc0, 1842009, 53201a7, 84ee2d0, f4848f0, abf2d13 |
| AC-003-03 | Us003ConferenceOptionsAcceptanceTest.java | cdd2fc0, 1842009, 53201a7, 84ee2d0, f4848f0, abf2d13 |
| AC-004-01 | us004-confirmation.test.tsx, e2e-01-external-registration.spec.ts, e2e-02-student-registration.spec.ts | 269f543, f0c79df, 9479887, 9f288ac, 12bd9cd, 5db5155 |
| AC-004-02 | us004-confirmation.test.tsx, e2e-03-field-errors.spec.ts | 269f543, f0c79df, 9479887, 9f288ac, 12bd9cd, 5db5155 |
| AC-004-03 | us004-confirmation.test.tsx, e2e-03-field-errors.spec.ts | 269f543, f0c79df, 9479887, 9f288ac, 12bd9cd, 5db5155 |
| AC-004-04 | us004-confirmation.test.tsx (2) | 269f543, f0c79df, 9479887, 9f288ac, 12bd9cd, 5db5155 |
| AC-005-01 | Us005StorageAcceptanceTest.java, e2e-01-external-registration.spec.ts | 461eafb, 457fe7a, 20e912f |
| AC-005-02 | Us005StorageAcceptanceTest.java, e2e-01-external-registration.spec.ts | 461eafb, 457fe7a, 20e912f |
| AC-005-03 | Us005JsonCopyFailureAcceptanceTest.java | 461eafb, 457fe7a, 20e912f |
| AC-005-04 | Us005StorageAcceptanceTest.java, e2e-04-slovenian-characters.spec.ts | 461eafb, 457fe7a, 20e912f |
| AC-006-01 | Us006ParticipantEmailAcceptanceTest.java, e2e-01-external-registration.spec.ts | e934f27, 6109d17, 7295abe |
| AC-006-02 | Us006ParticipantEmailAcceptanceTest.java | e934f27, 6109d17, 7295abe |
| AC-006-03 | Us006MailServerDownAcceptanceTest.java | e934f27, 6109d17, 7295abe |
| AC-007-01 | Us007OrganizerNotificationAcceptanceTest.java (2), e2e-01-external-registration.spec.ts | d1a3e55, fb34f01, 6109d17, 7295abe |
| AC-007-02 | Us007OrganizerNotificationAcceptanceTest.java, e2e-01-external-registration.spec.ts | d1a3e55, fb34f01, 6109d17, ea12aac, 7295abe |
| AC-008-01 | Us008ExportAcceptanceTest.java, e2e-05-export.spec.ts | 0371ae0, de69bfc, 144ae13, facb5d2, 02ace9e, cb97ce6 (+1) |
| AC-008-02 | Us008ExportAcceptanceTest.java, e2e-05-export.spec.ts | 0371ae0, de69bfc, 144ae13, facb5d2, 02ace9e, cb97ce6 (+1) |
| AC-008-03 | Us008ExportAcceptanceTest.java, e2e-04-slovenian-characters.spec.ts | 0371ae0, de69bfc, 144ae13, facb5d2, 02ace9e, cb97ce6 (+1) |
| AC-008-04 | Us008ExportAcceptanceTest.java | 0371ae0, de69bfc, 144ae13, facb5d2, 02ace9e, cb97ce6 (+1) |
| AC-009-01 | Us009SearchAcceptanceTest.java, e2e-06-organizer-search.spec.ts | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-02 | Us009SearchAcceptanceTest.java, e2e-06-organizer-search.spec.ts | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-03 | Us009SearchAcceptanceTest.java, e2e-06-organizer-search.spec.ts | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-04 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-05 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-06 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-07 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-08 | Us009SearchAcceptanceTest.java (2) | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-09 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-10 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-11 | Us009SearchAcceptanceTest.java | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-12 | us009-organizer-page.test.tsx (3), e2e-06-organizer-search.spec.ts | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |
| AC-009-13 | us009-organizer-page.test.tsx | 0294dda, 7acb276, de69bfc, facb5d2, 02ace9e, ed38694 (+5) |

48 acceptance criteria; without a test: none; without a commit: none.

## Findings

Severity by `general/quality/severity-scale.md` (Semgrep MEDIUM/WARNING → Medium, CVSS for dependency scanners, npm audit levels as reported).

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Medium | Semgrep `npm-missing-minimum-release-age` (`frontend/.npmrc`) | no `min-release-age` for newly published packages | Accepted: the setting needs npm 11.10; the pinned npm is 11.6.2 (`tech-stack.md`); exact versions, the committed lock file and `npm ci` already fix every resolved package |
| F-02 | Low | Semgrep `missing-internal` (`frontend/nginx.conf` `/api/` location) | `proxy_pass` location without `internal` | Accepted as false positive: the location is the public API proxy and its upstream is the constant `http://backend:8080`, so there is no client-controlled target (scanner match on constant input); `internal` would break the application |
| F-03 | Low | Semgrep parse errors | `frontend/src/organizer/OrganizerPage.tsx` (inline `type` imports) and the vendored `backend/mvnw` were only partly parsed | Accepted: tool limitation; `OrganizerPage.tsx` is covered by ESLint, `tsc`, the AC-009-13 test and review (no `dangerouslySetInnerHTML`, no URL or HTML sinks) |
| F-04 | Low | Gitleaks `generic-api-key` (`backend/src/test/.../acceptance/support/Settings.java:20`) | organizer password constant of the acceptance tests | Accepted as false positive: test-only credential of throwaway Testcontainers contexts; not a `.env` value (leak check clean) |
| F-05 | Low | Gitleaks `generic-api-key` (`out/logs/03_backend-acceptance-run.log`, 8 lines) | "Using generated security password" printed by the bootstrap skeleton in the phase 3 run | Accepted: random one-time value of a test JVM that no longer exists; the built backend defines its own user and prints no password |
| F-06 | Low | Gitleaks `generic-api-key` (`out/logs/05_stryker-probe.log:217`) | Vitest internal token in a Stryker debug log | Accepted: tool-internal value with no meaning outside that run |
| F-07 | Medium | `npm audit` | Moderate GHSA-qxg6-89mg-rqv8, GHSA-xp26-3454-mjgp in `@vitest/mocker` via `vitest`, `@vitest/coverage-v8` 4.1.11 | Accepted (D-06): test tooling only; `npm audit --omit=dev` reports 0 |
| F-08 | Medium | `npm audit` | Moderate advisories on `qs` via `@stryker-mutator/core` | Accepted (PA-03, D-07) |
| F-09 | Low | Dependency-Check | CVE-2025-7962 on `angus-activation` 2.0.3 | Suppressed for that artifact (PA-01, D-09); `angus-mail` 2.0.5 ships |
| F-10 | Low | Dependency-Check | CVE-2025-15104 on `hibernate-validator` | Suppressed for that artifact (PA-02, D-10); CPE name collision |

Totals: Critical 0, High 0, Medium 3, Low 7. Open after triage: Critical 0, High 0, Medium 0.

Online Dependency-Check with the NVD key (`out/logs/06_dependency-check.log`): 0 unsuppressed vulnerabilities, 2 suppressed (F-09, F-10). `npm audit --omit=dev`: 0. Leak checks of `general/skills/verify-release` step 8: no tracked or untracked file and no commit message contains a `.env` value. The runtime demo log was written with the organizer addresses masked before it was committed.

## Fix loops

No Critical or High finding, so no verify → fix → re-verify loop was needed in phase 6. Defects found by frozen tests during the build (phase 4) were fixed there: the JSON attachment differed from the stored copy on the Linux container (E2E-01; `ea12aac`).

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| none | | | |
