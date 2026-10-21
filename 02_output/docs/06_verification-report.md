# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Change run `tjan_single-agent_sdd_v001_run-1-search` (US-009 organizer search on the base run `tanej_single-agent_sdd_v001_run-1`, D-18). The whole system, base and US-009, was verified on 2026-10-22 at commit `5f1cc90` (the last code change, fix F-10); raw output in `out/logs/phase6-us009-*`. The base run's report is in git history.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Final run: backend 281/281 (`./mvnw verify`: unit 144, architecture 5, acceptance 132 = 102 base + 30 US-009; `phase5-us009-backend-coverage-all.log`, backend code and tests unchanged since), frontend 94/94 (`vitest run`: unit 82, acceptance 12 = 9 base + 3 US-009; `phase6-us009-frontend-tests.log`), end-to-end 6/6 (`phase6-us009-e2e.log`, stack rebuilt from the final code). Manifests recomputed (LF-normalised): `03_acceptance-manifest.sha256` 27/27 (unchanged since the base freeze `083eb5f`), `03_acceptance-manifest-us-009.sha256` 4/4, `00_input-manifest.sha256` 41/41. |
| DoD-02 | met | Backend `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check`: clean (`phase6-us009-backend-check.log`; SpotBugs exclusion D-14). Frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`): clean (`phase6-us009-frontend-check.log`). Semgrep findings triaged below. |
| DoD-03 | met | Quality gate (`docs/03_test-strategy.md`): backend line 99.20 %, mutation adjusted 99.18 % (raw 96.99 %, PIT, 0 rounds; unchanged since phase 5, not re-measured); frontend re-measured once after F-10: line 100 % (245/245), branch 94.81 %, mutation 91.19 % raw = adjusted (600 killed of 658 valid, Stryker full run `phase6-us009-stryker-full.log`). |
| DoD-04 | met | `ArchitectureTest` (ArchUnit 1.3.2) rules A1 … A5 pass (5/5) with the new classes in `domain`, `application`, `adapter.persistence`, `adapter.web`, `config`; no slice cycles. |
| DoD-05 | met | No open Critical or High: Dependency-Check 0 open (2 pre-approved suppressions), `npm audit --omit=dev` 0, the High on `vite` lowered to Medium by the human (D-22), the Semgrep ERROR is PA-04 (F-01, D-28), Gitleaks hits are false positives (F-02, F-11, F-12). |
| DoD-06 | met | Runtime demonstration below (`phase6-us009-runtime-demo.log`). |
| DoD-07 | met | Traceability below: 48/48 criteria have a test and a commit. |
| DoD-08 | phase 7 | Clean-clone check of the READMEs in phase 7. |
| DoD-09 | phase 7 | `docs/release-notes.md` updated in phase 7. |
| DoD-10 | met | Every decision D-01 … D-29 has a resolution or is pending review (D-08 … D-11, D-16, D-26, D-27, D-29); inputs unchanged during the run (input manifest 41/41). |
| DoD-11 | met | Phase 3 of this run (`691bc5a`, `f5b3dba`, `70ebd5c`) adds only test files and no production code; the freeze commit `943abd7` adds only `03_acceptance-manifest-us-009.sha256`, and each of its 4 files was committed before it; the base freeze `083eb5f` and its manifest are untouched. Build commits name US-009 / AC ids. Every manifest hash matches. |
| DoD-P01 | met | One external and one student registration through the frontend proxy: both 201; a rejected captcha token 400. |
| DoD-P02 | met | Both rows in PostgreSQL (psql) and both JSON copies on the volume; the student copy holds `Računalništvo` unchanged. |
| DoD-P03 | met | Mailpit: a confirmation to each participant and an organizer notification with one attachment per registration (4 messages). |
| DoD-P04 | met | Export with credentials: 200, a valid OOXML workbook with 15 rows (header + 14 database rows); without credentials 401; wrong password 401. |
| DoD-P05 | met | `CaptchaVerificationAcceptanceTest` (production path against a mocked verify endpoint, accepted and rejected token) passes in the final run. |
| DoD-P06 | met | On the running stack: E2E-06 in Chromium on `/organizer` (wrong credentials → error, no data; sign-in lists both new registrations; search by the student's last name → only the student; type "External" → only external rows); API: search by last name `Čebelar…` → total 1, by type STUDENT and EXTERNAL → the matching registration only; without credentials 401 with no registration data; wrong password 401. |

## Runtime demonstration

`docker compose --env-file ../.env up -d --build` in `02_output/` with `COMPOSE_PROJECT_NAME=konferenca-us009` (D-24), local environment, plain HTTP on 127.0.0.1; steps in `out/logs/phase6-us009-runtime-demo.log`.

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend | local (container, uid 10001) | `/actuator/health`, `/liveness`, `/readiness` (NFR-04); `/actuator/env` | 200 UP ×3; 401 |
| frontend + backend | local | external and student registration via `http://127.0.0.1:8081/api/registrations`; rejected captcha | 201, 201; 400 |
| backend + PostgreSQL + volume | local | rows and JSON copies of both registrations | present, UTF-8 unchanged |
| backend + Mailpit | local | participant and organizer mails | 2 confirmations, 2 notifications with an attachment |
| backend | local | export with, without and with wrong credentials (US-008) | 200 workbook; 401; 401 |
| backend | local | search by last name, by type, through the proxy, without and with wrong credentials, invalid parameters (US-009) | total 1 each; 200; 401 (no data); 401; 400 with `type` and `page` errors |
| backend | local | wrong credentials with `X-Requested-With: XMLHttpRequest` | 401 without `WWW-Authenticate` (no browser login dialog) |
| frontend | local | `/organizer` | 200, the app shell (nginx `try_files`) |
| frontend, backend | local | security headers; client `Host`; 20 KB body; 62 searches in one minute, then one registration POST | headers present; 200; 413; 60 × 200 then 2 × 429, the POST is not limited (400 for the empty body), so the search does not use up the registration budget |
| all | local | `docker compose down`, then `up` (NFR-02) | both rows and copies present, search finds both; all containers healthy |
| backend log | local | names and emails of this demo in the container log | 0 lines (ES-07) |
| frontend (browser) | local | E2E-01 … E2E-06 (Playwright, Chromium) | 6/6 |

## Security evidence

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator`, `RegistrationController.parse`; `SearchCriteriaParser` (US-009) | AC-001-02 … 06, AC-002-02 … 05, AC-009-08 tests; runtime 400 |
| SB-02 | `SecurityConfig` (deny by default), `OrganizerAccess` on export and search | AC-008-02, AC-008-04, AC-009-09, AC-009-10; runtime 401 |
| SB-03 | BCrypt-hashed organizer password; secrets without defaults | inspection; leak checks |
| SB-04 | external nginx with TLS (production) | manual test (release notes) |
| SB-05 | JPA parameters (the search binds the pattern, type and option as parameters and escapes `%`, `_`, `\`), React escaping, string-only workbook cells, plain-text mail | AC-006-02, AC-009-13; Semgrep |
| SB-06 | `RateLimitFilter` ahead of authentication, per client, method and path | runtime 429 on the search; filter tests |
| SB-07 | `ApiExceptionHandler`, `server.error.*`, logs with ids only | AC-004-04, AC-005-03; demo log check (0 lines) |
| SB-08 | Dependency-Check (online, NVD key), `npm audit` | `phase6-us009-depcheck.log`, `phase6-us009-npm-audit.txt` (F-06, F-07, F-08, F-13) |
| SB-09 | Semgrep, Gitleaks, SpotBugs, PMD | `phase6-us009-semgrep.json`, `phase6-us009-gitleaks.json`, `phase6-us009-backend-check.log` |
| SB-10 | Spring Security headers, nginx `add_header` | runtime headers on `/api/registrations` and `/organizer` |
| SB-11 | non-root users in both images | runtime `id` (10001, nginx) |
| SB-12, SB-13 | only the fields of `business-rules.md`; the search returns a subset of them | inspection |
| SB-14 | consents never preselected | AC-001-08, AC-002-07 |
| SR-01 | `RecaptchaHttpVerifier`; verification before storage | AC-001-07, AC-002-06, DoD-P05 |
| SR-02 | `ProductionGuard` | `SecuritySettingsTest` |
| SR-03 | `RateLimitFilter` (export and search included), `BodySizeLimitFilter`, nginx `client_max_body_size` | runtime 429 and 413 |
| SR-04 | option ids checked against the active configured set | AC-001-05, AC-002-04 |
| SR-05 | `MailRegistrationNotifier` | AC-006-02 |
| SR-06 | `OrganizerAccess` (HTTPS or loopback) on export and search | AC-008-04, AC-009-10 |
| SR-07 | export, organizer mail and search carry registration fields only (the search omits study programme, student ID and consents) | AC-007-01, AC-008-01, AC-009-01; `SearchController` inspection |
| SR-08 | nginx `proxy_set_header Host backend:8080` | runtime request with `Host: evil.example` |
| SR-09 | organizer page renders every value as React text, no `dangerouslySetInnerHTML` | AC-009-13 (markup shown literally, no `img`/`script`/`b` element, no handler run); inspection |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (`out/logs/phase6-us009-trace-ac.md`):

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | ExternalRegistrationAcceptanceTest.java (4), external-registration.e2e.spec.ts (2) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-02 | ExternalRegistrationAcceptanceTest.java (2) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-03 | ExternalRegistrationAcceptanceTest.java (2) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-04 | ExternalRegistrationAcceptanceTest.java (2) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-05 | ExternalRegistrationAcceptanceTest.java (2) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-06 | ExternalRegistrationAcceptanceTest.java (3) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-07 | CaptchaVerificationAcceptanceTest.java (4), ExternalRegistrationAcceptanceTest.java (3) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-001-08 | external-registration.e2e.spec.ts, registration-form.acceptance.test.tsx (2) | 17c5442, d3b5f37, 083eb5f, 00917aa, 1a1d19c, 535e89a (+5) |
| AC-002-01 | StudentRegistrationAcceptanceTest.java (4), student-registration.e2e.spec.ts (2) | 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-002-02 | StudentRegistrationAcceptanceTest.java (2) | 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-002-03 | StudentRegistrationAcceptanceTest.java (2) | 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-002-04 | StudentRegistrationAcceptanceTest.java (2) | 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-002-05 | StudentRegistrationAcceptanceTest.java (2) | 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-002-06 | CaptchaVerificationAcceptanceTest.java (3), StudentRegistrationAcceptanceTest.java (4) | 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-002-07 | student-registration.e2e.spec.ts (2), registration-form.acceptance.test.tsx (3) | d3b5f37, 0d2b9d5, 00917aa, a5005b6, 207b2aa, bd53805 |
| AC-003-01 | CaptchaVerificationAcceptanceTest.java, FormConfigAcceptanceTest.java (3) | 8ee505a, 0b27908, 88ca1be, 041dc47, 207b2aa, d45a84c |
| AC-003-02 | ChangedConfigAcceptanceTest.java (3) | 8ee505a, 0b27908, 88ca1be, 041dc47, 207b2aa, d45a84c |
| AC-003-03 | InvalidConfigAcceptanceTest.java (2) | 8ee505a, 0b27908, 88ca1be, 041dc47, 207b2aa, d45a84c |
| AC-004-01 | external-registration.e2e.spec.ts (2), student-registration.e2e.spec.ts (2), confirmation.acceptance.test.tsx (3) | 5012cc7, 35a01a6, b440e36, 73ecbaf, bd53805 |
| AC-004-02 | field-errors.e2e.spec.ts (2), confirmation.acceptance.test.tsx (3) | 5012cc7, 35a01a6, b440e36, 73ecbaf, bd53805 |
| AC-004-03 | field-errors.e2e.spec.ts (2), confirmation.acceptance.test.tsx (2) | 5012cc7, 35a01a6, b440e36, 73ecbaf, bd53805 |
| AC-004-04 | confirmation.acceptance.test.tsx (4) | 5012cc7, 35a01a6, b440e36, 73ecbaf, bd53805 |
| AC-005-01 | StorageAcceptanceTest.java (4), external-registration.e2e.spec.ts (2) | b5ed93a, 1a1d19c, de0568a, ed1a757, 1051016, 865434e |
| AC-005-02 | StorageAcceptanceTest.java (2), external-registration.e2e.spec.ts | b5ed93a, 1a1d19c, de0568a, ed1a757, 1051016, 865434e |
| AC-005-03 | JsonCopyFailureAcceptanceTest.java (2) | b5ed93a, 1a1d19c, de0568a, ed1a757, 1051016, 865434e |
| AC-005-04 | StorageAcceptanceTest.java (2), slovenian-characters.e2e.spec.ts (2) | b5ed93a, 1a1d19c, de0568a, ed1a757, 1051016, 865434e |
| AC-006-01 | ParticipantEmailAcceptanceTest.java (3), external-registration.e2e.spec.ts (2) | 69e0682, 20f8857, 1051016 |
| AC-006-02 | ParticipantEmailAcceptanceTest.java (2) | 69e0682, 20f8857, 1051016 |
| AC-006-03 | MailServerDownAcceptanceTest.java (3) | 69e0682, 20f8857, 1051016 |
| AC-007-01 | OrganizerNotificationAcceptanceTest.java (3), external-registration.e2e.spec.ts | 4c3bb8d, 20f8857, 1051016, 865434e |
| AC-007-02 | OrganizerNotificationAcceptanceTest.java (2), external-registration.e2e.spec.ts (2) | 4c3bb8d, 20f8857, 1051016, 865434e |
| AC-008-01 | ExportAcceptanceTest.java (4), organizer-export.e2e.spec.ts (2) | 5780c4c, 083eb5f, 04c3d60, eccaf4e |
| AC-008-02 | ExportAcceptanceTest.java (2), organizer-export.e2e.spec.ts (2) | 5780c4c, 083eb5f, 04c3d60, eccaf4e |
| AC-008-03 | ExportAcceptanceTest.java (2), slovenian-characters.e2e.spec.ts (2) | 5780c4c, 083eb5f, 04c3d60, eccaf4e |
| AC-008-04 | ExportAcceptanceTest.java (3) | 5780c4c, 083eb5f, 04c3d60, eccaf4e |
| AC-009-01 | SearchAcceptanceTest.java (4), organizer-search.e2e.spec.ts (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-02 | SearchAcceptanceTest.java (2), organizer-search.e2e.spec.ts (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-03 | SearchAcceptanceTest.java (2), organizer-search.e2e.spec.ts (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-04 | SearchAcceptanceTest.java (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-05 | SearchAcceptanceTest.java (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-06 | SearchAcceptanceTest.java (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-07 | SearchAcceptanceTest.java (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-08 | SearchAcceptanceTest.java (4) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-09 | SearchAcceptanceTest.java (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-10 | SearchAcceptanceTest.java (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-11 | SearchAcceptanceTest.java (3) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-12 | organizer-search.e2e.spec.ts (2), organizer-search.acceptance.test.tsx (3) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |
| AC-009-13 | organizer-search.acceptance.test.tsx (2) | 321a75d, 27bec6c, dd36d01, 691bc5a, f5b3dba, 70ebd5c (+13) |

48 acceptance criteria; without a test: none; without a commit: none.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | High → Low | Semgrep `use-of-basic-authentication` (ERROR), `organizerBasic` in `docs/02_contracts/openapi.yaml` | HTTP Basic on export and search | accepted under PA-04 (D-17, D-28) |
| F-02 | Low | Gitleaks `generic-api-key`, `AcceptanceTestSupport.java:42` | test-only organizer password constant (frozen base test) | false positive, accepted: not a `.env` value, Testcontainers only |
| F-03 | Medium | Semgrep `detect-non-literal-regexp` (WARNING) ×5 in `src/acceptance/harness.tsx`, `e2e/support.ts` | RegExp from a string in base test code | accepted: test code; constant labels through `escapeRegExp` |
| F-04 | Medium | Semgrep `missing-internal` (WARNING), `frontend/nginx.conf` `/api/` | proxy location without `internal` | accepted: `/api/` must be public; fixed upstream host (no SSRF) |
| F-05 | Medium | Semgrep `npm-missing-minimum-release-age`, `frontend/.npmrc` | no `min-release-age` | accepted: needs npm ≥ 11.10, the pin is 11.6.2; versions exact and locked (ES-04) |
| F-06 | Medium | `npm audit`: `qs` via `@stryker-mutator/core` | dev-tooling advisories | accepted under PA-03 (D-06) |
| F-07 | High → Medium | `npm audit`: `vite` ≤ 6.4.3 (launch-editor and dev-server advisories) | dev server only, not in the bundle | lowered by the human (D-22); upgrade listed in the release notes |
| F-08 | Low | Dependency-Check CVE-2025-7962, CVE-2025-15104 | false positives | suppressed per artifact under PA-01, PA-02 (D-05) |
| F-09 | Low | Semgrep parse error in `backend/mvnw` | vendored wrapper script not analysed | accepted: unchanged Maven Wrapper 3.3.2 |
| F-10 | Low | jscpd 5.4.0: 1 clone (12 lines, 1.06 %) in `frontend/src/api.ts` | the 400 handling of `submitRegistration` repeated in the new `searchRegistrations` | fixed in `5f1cc90` (shared `rejection` helper); re-run: 0 clones, check clean, 94/94, Stryker 91.19 % |
| F-11 | Low | Gitleaks `generic-api-key` ×246 in `03_statistics/data/…jsonl` (base commit `84f9bde`, deleted in `06619d7`) | the match is the API beta-flag list `oauth-2025-04-20`, … in transcript metadata | false positive, accepted: no `.env` value in that blob (`grep -cF` 0); the file is the operator's transcript backup, outside the agent's files |
| F-12 | Low | Gitleaks `generic-api-key`, `frontend/src/acceptance/organizer-harness.tsx:10` | test-only organizer password of the mocked backend | false positive, accepted: not a `.env` value, never sent to a real backend |
| F-13 | Medium | `npm audit`: `@vitest/mocker` ≤ 4.1.11 (GHSA-qxg6-89mg-rqv8, GHSA-xp26-3454-mjgp) via the pinned `vitest` 4.1.11 | test tooling only | accepted, pending review (D-29); `npm audit --omit=dev` 0 |

As found: Critical 0, High 2 (F-01, F-07), Medium 5 (F-03 … F-06, F-13), Low 6 (F-02, F-08 … F-12). Open after triage: Critical 0, High 0 (F-01 lowered under PA-04, F-07 lowered by the human in D-22); Medium 6 accepted with reasons (F-03 … F-07, F-13); F-10 fixed.

Leak check (step 8): no `.env` value in any tracked or untracked file, in any commit message, or in the base transcript blob (file names and counts only, values never displayed).

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-10 (Low, duplication) | `5f1cc90`: `rejection()` shared by both API functions | jscpd 0 clones; `npm run check` clean; vitest 94/94; e2e 6/6 on the rebuilt stack; Stryker full run 91.19 % |
