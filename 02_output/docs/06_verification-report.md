# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-20 at commit `3a020a3` (no code or test changed afterwards); raw output in `out/logs/phase6-*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Final run: backend 251/251 (`./mvnw verify`: unit 144, architecture 5, acceptance 102), frontend 55/55 (`vitest run`: unit 46, acceptance 9), end-to-end 5/5 (`phase6-e2e.log`, stack rebuilt from the final code). Both manifests recomputed (LF-normalised): `03_acceptance-manifest.sha256` 27/27 and `00_input-manifest.sha256` 41/41 match. |
| DoD-02 | met | Backend `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check`: 0 violations, 0 bugs (`phase5-check.log`; SpotBugs exclusion D-14). Frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`): clean (`phase6-frontend-check.log`). Semgrep findings F-01, F-03 … F-05 triaged below. |
| DoD-03 | met | Quality gate per `docs/03_test-strategy.md`: backend line 99.11 %, mutation adjusted 99.10 % (raw 96.70 %, PIT, 1 survivor round); frontend line 100 %, mutation 90.85 % (Stryker, no round). No code or test changed after phase 5, so nothing was re-measured. |
| DoD-04 | met | `ArchitectureTest` (ArchUnit 1.3.2) checks rules A1 … A5 of the specification, including slice cycles for `si.konferenca.registration.(*)` and `adapter.(*)`: 5/5 pass. |
| DoD-05 | met | No open Critical or High: Dependency-Check 0 open (2 pre-approved suppressions), `npm audit --omit=dev` 0, the one Semgrep ERROR is PA-04 (F-01), the one Gitleaks hit is a test constant (F-02). |
| DoD-06 | met | Runtime demonstration below (`phase6-runtime-demo.log`). |
| DoD-07 | met | Traceability table below: 35/35 criteria have a test and a commit. |
| DoD-08 | met (phase 7) | Root, backend and frontend READMEs followed from a fresh `git clone` of `31025cc`: backend build, check and 251 tests, frontend `npm ci`, build, check and 55 tests, `docker compose up --build` healthy, e2e 5/5 (`out/logs/phase7-clone.log`). A clone below a deep Windows folder needs `core.longpaths` (README note added). |
| DoD-09 | met (phase 7) | `docs/release-notes.md`, "Must be tested manually by a human": reCAPTCHA with production keys, SMTP to a real mailbox, TLS and reverse proxy, production start guard. |
| DoD-10 | met | Every decision D-01 … D-17 has a resolution or is pending review (D-08 … D-11, D-16); inputs unchanged (input manifest matches). |
| DoD-11 | met | Phase 3 commits add only tests, test harnesses and the compose/Docker files, no production source beyond the skeleton; the freeze commit `083eb5f` adds only the manifest, and every listed file was committed before it. Build commits name US/AC ids for all 8 stories. Every manifest hash matches. |
| DoD-P01 | met | One external and one student registration through the frontend proxy: both 201. |
| DoD-P02 | met | Both rows in PostgreSQL (psql) and both JSON copies on the volume (`ls`, `cat`); the student copy holds `Študent` and `Računalništvo` unchanged. |
| DoD-P03 | met | Mailpit API: a confirmation to each participant and an organizer notification with one JSON attachment per registration. |
| DoD-P04 | met | Export with organizer credentials: 200, a valid workbook (OOXML parts; 20 rows = header + 19 database rows); without credentials 401 with 0 bytes; wrong password 401. |
| DoD-P05 | met | `CaptchaVerificationAcceptanceTest`: production path (test mode off) against a mocked verify endpoint, accepted and rejected tokens, secret posted (6/6). |

## Runtime demonstration

`docker compose --env-file ../.env up -d --build` in `02_output/` (local environment, plain HTTP on 127.0.0.1), then the steps logged in `out/logs/phase6-runtime-demo.log`.

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend | local (container, uid 10001) | `/actuator/health`, `/liveness`, `/readiness` (NFR-04); `/actuator/env` | 200 UP ×3; 401 (not exposed) |
| frontend + backend | local | external and student registration via `http://127.0.0.1:8081/api/registrations`; rejected captcha token | 201, 201; 400 |
| backend + PostgreSQL + volume | local | rows and JSON copies of both registrations | present, UTF-8 text unchanged |
| backend + Mailpit | local | participant and organizer mails | 2 confirmations, 2 notifications with a JSON attachment |
| backend | local | export with, without and with wrong credentials (US-008) | 200 workbook; 401; 401 |
| all | local | `docker compose down`, then `up` (NFR-02) | both rows and both JSON copies still present; all containers healthy |
| frontend, backend | local | security headers; client `Host` via the proxy; 20 KB body; 62 requests in one minute | headers present; 200 with the fixed upstream host; 413; 60 × 200 then 2 × 429 |
| frontend (browser) | local | E2E-01 … E2E-05 (Playwright, Chromium) | 5/5 |

## Security evidence

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator`, `RegistrationController.parse` | AC-001-02 … 06 and AC-002-02 … 05 tests; `RegistrationValidatorTest` |
| SB-02 | `SecurityConfig` (deny by default), `OrganizerAccess` | AC-008-02, AC-008-04; `SecuritySettingsTest` |
| SB-03 | BCrypt-hashed organizer password; secrets without defaults in `application.properties` | inspection; leak checks |
| SB-04 | external nginx with TLS (production) | manual test (release notes) |
| SB-05 | JPA parameters, React escaping, string-only workbook cells, plain-text mail | AC-006-02; `workbookHasTheHeaderAndStringCells`; Semgrep |
| SB-06 | `RateLimitFilter` ahead of authentication | runtime 429; `WebAdapterTest`, `FilterLimitsTest` |
| SB-07 | `ApiExceptionHandler`, `server.error.*`, logs with ids only | AC-004-04, AC-005-03; backend container log has no participant data (`phase6-backend-container.log`) |
| SB-08 | Dependency-Check (online, NVD key), `npm audit` | `phase6-depcheck.log`, `phase6-npm-audit.txt` (F-06, F-07, F-08) |
| SB-09 | Semgrep, Gitleaks, SpotBugs, PMD | `phase6-semgrep.json`, `phase6-gitleaks.json`, `phase5-check.log` |
| SB-10 | Spring Security headers, nginx `add_header` | runtime `curl -I` |
| SB-11 | non-root users in both images | runtime `id` (10001, nginx) |
| SB-12, SB-13 | only the fields of `business-rules.md`; purposes in `security-requirements.md`; retention in the release notes | inspection |
| SB-14 | consents never preselected | AC-001-08, AC-002-07 |
| SR-01 | `RecaptchaHttpVerifier`; verification before storage | AC-001-07, AC-002-06, DoD-P05 |
| SR-02 | `ProductionGuard`; test mode only by `CAPTCHA_TEST_MODE` | `SecuritySettingsTest` |
| SR-03 | `RateLimitFilter`, `BodySizeLimitFilter`, nginx `client_max_body_size` | runtime 429 and 413; filter tests |
| SR-04 | option ids checked against the active configured set | AC-001-05, AC-002-04 |
| SR-05 | `MailRegistrationNotifier` (plain text, no input in headers) | AC-006-02; `MailRegistrationNotifierTest` |
| SR-06 | `OrganizerAccess` (HTTPS or loopback); production refuses `ORGANIZER_HTTPS_ONLY=false` | AC-008-04; `SecuritySettingsTest` |
| SR-07 | export and organizer mail carry registration fields only | AC-007-01, AC-008-01; inspection |
| SR-08 | nginx `proxy_set_header Host backend:8080` | runtime request with `Host: evil.example`; inspection; the production proxy is a manual test |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (`out/logs/phase6-trace-ac.md`):

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

35 acceptance criteria; without a test: none; without a commit: none.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | High → Low | Semgrep `use-of-basic-authentication` (ERROR), organizer security scheme in `docs/02_contracts/openapi.yaml` | HTTP Basic on the export | accepted under PA-04 (D-17): required by the architecture, HTTPS or localhost only (SR-06), BCrypt hash, rate limited |
| F-02 | Low | Gitleaks `generic-api-key`, `AcceptanceTestSupport.java:42` | test-only organizer password constant in a frozen acceptance test | false positive, accepted: not a `.env` value (leak check clean), used only against Testcontainers |
| F-03 | Medium | Semgrep `detect-non-literal-regexp` (WARNING) ×5 in `src/acceptance/harness.tsx`, `e2e/support.ts` | RegExp built from a string | accepted: test code only; every input is a constant label passed through `escapeRegExp` |
| F-04 | Medium | Semgrep `missing-internal` (WARNING), `frontend/nginx.conf` `/api/` | proxy location without `internal` | accepted: `/api/` must be public; the upstream is a fixed host, so the client cannot choose a target (no SSRF) |
| F-05 | Medium | Semgrep `npm-missing-minimum-release-age`, `frontend/.npmrc` | no `min-release-age` | accepted: the setting needs npm 11.10, the pinned npm is 11.6.2; every version is exact and locked (ES-04) |
| F-06 | Medium | `npm audit`: `qs` ×2 via `@stryker-mutator/core` | dev-tooling advisories | accepted under PA-03 (D-06); `npm audit --omit=dev` 0 |
| F-07 | Medium | `npm audit`: GHSA-698v-4r3x-3vcv, `vite` ≤ 6.4.3 (launch-editor on Windows) | affects the dev server only, not the built bundle | accepted, pending review (D-16): the fix, `vite` 6.4.4, is a version change that needs the human |
| F-08 | Low | Dependency-Check CVE-2025-7962 (angus-activation), CVE-2025-15104 (hibernate-validator) | false positives | suppressed per artifact under PA-01, PA-02 (D-05) |
| F-09 | Low | Semgrep parse error in `backend/mvnw` | vendored wrapper script not analysed | accepted: unchanged Maven Wrapper 3.3.2 script (checksum verified in phase 0) |

As found: Critical 0, High 1 (F-01), Medium 5 (F-03 … F-07), Low 3. Open after triage: Critical 0, High 0; Medium 5 accepted with reasons.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| — | none | no Critical or High finding needed a fix; no code changed in phase 6 | — |
