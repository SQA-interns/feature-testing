# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Date: 2026-10-22. Code verified: commit `d76ca46`. Backend and frontend code and tests are unchanged since phase 5; the only phase 6 change is the F-01 fix in `docs/02_contracts/validate.mjs`, which is not component code, so coverage and mutation scores were not re-measured.

## Integrity

| Check | Method | Result |
|---|---|---|
| Input manifest | `sha256sum -c` and an LF-normalised recomputation of all 41 entries of `docs/00_input-manifest.sha256` | all match |
| Acceptance manifest | the same for the 28 entries of `docs/03_acceptance-manifest.sha256` | all match; no frozen test changed |
| Secret leak, files | `git ls-files -co --exclude-standard -z \| xargs -0 grep -lF -f <(.env values of 6+ characters)` | no file listed |
| Secret leak, commit messages | `git log --all --format=%B \| grep -cF -f …` | `0` |

## Test suite and checks (ES-05)

| Component | Command | Result | Log |
|---|---|---|---|
| backend | `./mvnw verify` | 288 tests, 0 failures (177 unit, 6 architecture, 105 acceptance) | `logs/06_backend-verify.log` |
| backend | `./mvnw spotless:check compile pmd:check pmd:cpd-check spotbugs:check` | clean | `logs/06_backend-check.log` |
| frontend | `npm run check` (Prettier, ESLint, tsc) | clean | `logs/06_frontend-check.log` |
| frontend | `npm test` | 109 tests, 0 failures (91 unit, 18 acceptance) | `logs/06_frontend-test.log` |
| frontend | `npm run build` | built | `logs/06_frontend-build.log` |
| both | `npm run e2e` against the recreated stack | 6 of 6 passed | `logs/06_e2e.log` |

Quality gate (phase 5, `docs/03_test-strategy.md`): backend line 98.09 %, adjusted mutation 93.15 % (0 rounds); frontend line 100 %, adjusted mutation 93.83 % (1 round). Thresholds: line ≥ 95 %, mutation ≥ 90 %.

## Runtime demonstration

Local stack: `docker compose --env-file ../.env down` then `up -d --build` in `02_output/`; all four containers healthy. Script `logs/06_runtime-demo.sh`, output `logs/06_runtime-demo.log` (credentials read from `.env` and never printed; email addresses masked).

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend | local, image `registration-backend:local`, user `app` | health, liveness, readiness | all `UP` (NFR-04, ES-09) |
| backend | local | one external and one student registration (Slovenian characters) through `POST /api/registrations` | both 201 with ids (DoD-P01) |
| postgres, backend volume | local | rows of both ids, 5 option rows, 2 consent rows; `/data/json-copies/<id>.json` for both | present (DoD-P02) |
| mailpit | local | participant confirmations; organizer notifications with 1 attachment each | present (DoD-P03) |
| backend | local | export with credentials / without / wrong password | 200 xlsx (zip signature `PK`, 4.7 kB) / 401 / 401 (DoD-P04) |
| backend | local | search by last name, by type STUDENT, without credentials, invalid page | 1 match / only students / 401 / 400 with a `page` error (DoD-P06) |
| frontend | local, image `registration-frontend:local`, user `nginx` | security headers; `/api` proxy with a forged `Host` header | CSP, X-Frame-Options, nosniff, Referrer-Policy present; the proxy answers 200 with its fixed upstream Host (SR-08) |
| backend | local | 20 kB body; 70 requests in one minute to one endpoint | 413; 13 answers 429 with `Retry-After` (SR-03) |
| all | local | `docker compose down` and `up` of every container | 9 rows before and after, JSON copy still present, search still finds the registration (NFR-02) |
| frontend and backend | local, Chromium | the six e2e journeys | 6 passed (DoD-06; DoD-P06 search page) |

## Security baseline and project requirements

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator`, `SearchCriteria` on the server | AC-001-02..07, AC-002-02..06, AC-009-08 tests; domain unit tests |
| SB-02 | `SecurityConfig`: HTTP Basic, role ORGANIZER on search and export, everything else not listed denied | AC-008-02, AC-009-09; runtime 401 |
| SB-03 | BCrypt hash of the organizer password, made at start; secrets only from configuration | inspection of `SecurityConfig`; secret-leak check clean |
| SB-04 | TLS at the external nginx; organizer HTTPS-only (`HttpsOnlyFilter`) | AC-008-04, AC-009-10; real TLS is a manual test |
| SB-05 | React text rendering, JPA parameters with escaped LIKE, plain-text email, Excel text cells | AC-009-13, AC-006-02, `LikeEscapeTest`, `PoiWorkbookWriterTest` |
| SB-06 | `RateLimitFilter` | `FiltersTest`; runtime 429 |
| SB-07 | `ApiExceptionHandler` general messages; logs carry registration ids only | `ApiExceptionHandlerTest`, AC-004-04; logs inspected (below) |
| SB-08 | Dependency-Check online, `npm audit` | no Critical or High (findings below) |
| SB-09 | Semgrep, Gitleaks | findings below |
| SB-10 | Spring Security headers; nginx CSP and headers | runtime headers |
| SB-11 | non-root users `app` and `nginx` | runtime `id -un` |
| SB-12, SB-13 | only the BR-01 fields are stored; purpose and retention in `security-requirements.md` and the release notes (OQ-06) | inspection of the schema and request types |
| SB-14 | consents unchecked; mandatory consent required; time stored | AC-001-06, AC-001-08, AC-002-05, AC-002-07, AC-005-01 |
| SR-01 | `CaptchaVerifier` on every registration | AC-001-07, AC-002-06; DoD-P05 (`RecaptchaVerifierTest`) |
| SR-02 | test mode only by setting; start refused without keys or with test mode in production | `ConfigTest` |
| SR-03 | rate limit and body size filters on every `/api` endpoint | `FiltersTest`; runtime 413 and 429 |
| SR-04 | options checked against the active configured set | AC-001-05, AC-002-04 |
| SR-05 | plain-text email, no input in headers, strict address rule | AC-006-02, `SmtpNotifierTest`, `EmailAddressTest` |
| SR-06 | `HttpsOnlyFilter` with trusted-proxy handling (`TRUSTED_PROXIES`) | AC-008-04, AC-009-10, `FiltersTest` |
| SR-07 | export columns and notification text hold registration data only | AC-007-02, AC-008-01, `MailTextsTest` |
| SR-08 | `proxy_set_header Host backend:8080` in `frontend/nginx.conf`; production proxy sample in the backend README | inspection; runtime request with a forged Host |
| SR-09 | React renders text only; no `dangerouslySetInnerHTML` | AC-009-13 |

Logs and error responses: the test, runtime and scanner logs in `logs/` were checked. Error bodies contain only the general messages above. The only password-like strings are the random Spring Boot development passwords of the phase 3 skeleton (F-04). Email failures are logged with the registration id only; no participant data appears in backend log lines.

## Scanners

| Tool | Mode | Result | Raw output |
|---|---|---|---|
| OWASP Dependency-Check 12.1.0 | online with the NVD key from `.env`; OSS Index off (D-05) | no vulnerability after the PA-01/PA-02 suppressions | `logs/06_dependency-check.log` |
| npm audit (npm 11.6.2) | full and `--omit=dev` | 5 Moderate, all dev tooling; `--omit=dev`: 0 | `logs/06_npm-audit.json` |
| Semgrep 1.177.0 | rule sets p/default, java, typescript, react, dockerfile, nginx | 3 results (F-01..F-03); no basic-authentication finding, so PA-04 was not needed | `logs/06_semgrep.json`, `logs/06_semgrep-after-f01.json` |
| Gitleaks v8.30.1 | git history, redacted | 18 matches, all false positives (F-04, F-07) | `logs/06_gitleaks.json` |

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | full suites pass (above); both manifests match |
| DoD-02 | met | Spotless, PMD, CPD, SpotBugs, Prettier, ESLint, tsc clean; Semgrep findings triaged |
| DoD-03 | met | `docs/03_test-strategy.md` (scope, raw and adjusted scores, rounds); `run-log.json` `codeMetrics.gate.met = true` |
| DoD-04 | met | `ArchitectureTest` (6 rules incl. AR-03 cycles); AR-01 by inspection (`api.ts` only calls `/api`); AR-05 by `RegistrationServiceTest` and AC-005-03; AR-06 Flyway; AR-07 no secret in the frontend; AR-09 acceptance tests |
| DoD-05 | met | no open Critical or High finding |
| DoD-06 | met | runtime demonstration and e2e above |
| DoD-07 | met | traceability below: every AC has a test and a commit |
| DoD-08 | phase 7 | READMEs are written in phase 7 and followed from a clean clone there |
| DoD-09 | phase 7 | release notes with the manual tests are written in phase 7 |
| DoD-10 | met | D-01..D-15 all resolved (D-08, D-10, D-11 pending review); manifests unchanged |
| DoD-11 | met | phase 3 commits add tests and the stack files only (D-13); freeze commit `1ddcadd` holds only the manifest, every listed file was committed before it; every hash matches; build commits name US-001..US-009 |
| DoD-P01 | met | runtime: external and student registration 201 |
| DoD-P02 | met | runtime: database rows and JSON copies |
| DoD-P03 | met | runtime: Mailpit messages with the JSON attachment |
| DoD-P04 | met | runtime: 200 workbook, 401 without access |
| DoD-P05 | met | `RecaptchaVerifierTest` against a mocked siteverify endpoint, accepted and rejected token |
| DoD-P06 | met | runtime search by last name and type, 401 without access; E2E-06 on the search page |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs`:

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | Us001ExternalRegistrationAcceptanceTest.java (4), e2e-01-external-registers.spec.ts (2) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-02 | Us001ExternalRegistrationAcceptanceTest.java (3) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-03 | Us001ExternalRegistrationAcceptanceTest.java (2) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-04 | Us001ExternalRegistrationAcceptanceTest.java (3) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-05 | Us001ExternalRegistrationAcceptanceTest.java (3) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-06 | Us001ExternalRegistrationAcceptanceTest.java (3) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-07 | Us001ExternalRegistrationAcceptanceTest.java (3) | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-001-08 | Us001ExternalRegistrationAcceptanceTest.java, us001-external-form.test.tsx (3), e2e-01-external-registers.spec.ts | 9866aa1, d1adf9c, b9e9b2c, dfcbd5b, b2c797e, d6b8dff (+2) |
| AC-002-01 | Us002StudentRegistrationAcceptanceTest.java (4), e2e-02-student-registers.spec.ts (2) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-002-02 | Us002StudentRegistrationAcceptanceTest.java (3) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-002-03 | Us002StudentRegistrationAcceptanceTest.java (3) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-002-04 | Us002StudentRegistrationAcceptanceTest.java (3) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-002-05 | Us002StudentRegistrationAcceptanceTest.java (3) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-002-06 | Us002StudentRegistrationAcceptanceTest.java (3) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-002-07 | Us002StudentRegistrationAcceptanceTest.java, us002-student-form.test.tsx (4), e2e-02-student-registers.spec.ts (2) | 6ea5788, d1adf9c, dfcbd5b, d6b8dff |
| AC-003-01 | Us003ConferenceOptionsAcceptanceTest.java (4) | b680b9e, 78cfb96, 44d7c19, af8c526 |
| AC-003-02 | Us003ConferenceOptionsAcceptanceTest.java (2) | b680b9e, 78cfb96, 44d7c19, af8c526 |
| AC-003-03 | Us003ConferenceOptionsAcceptanceTest.java (3) | b680b9e, 78cfb96, 44d7c19, af8c526 |
| AC-004-01 | us004-confirmation.test.tsx (2), e2e-01-external-registers.spec.ts (2), e2e-02-student-registers.spec.ts (2) | d109f16, b2c797e, d6b8dff, 654f085, 3b8b80d |
| AC-004-02 | us004-confirmation.test.tsx, e2e-03-field-errors.spec.ts (2) | d109f16, b2c797e, d6b8dff, 654f085, 3b8b80d |
| AC-004-03 | us004-confirmation.test.tsx (2), e2e-03-field-errors.spec.ts (2) | d109f16, b2c797e, d6b8dff, 654f085, 3b8b80d |
| AC-004-04 | us004-confirmation.test.tsx (2) | d109f16, b2c797e, d6b8dff, 654f085, 3b8b80d |
| AC-005-01 | Us005StorageAcceptanceTest.java (3), e2e-01-external-registers.spec.ts (2) | a159a1d, 6dbb663, fe8fc6d, a7a57e5, f6812a2 |
| AC-005-02 | Us005StorageAcceptanceTest.java, e2e-01-external-registers.spec.ts | a159a1d, 6dbb663, fe8fc6d, a7a57e5, f6812a2 |
| AC-005-03 | Us005JsonCopyFailureAcceptanceTest.java (3), Us005StorageAcceptanceTest.java | a159a1d, 6dbb663, fe8fc6d, a7a57e5, f6812a2 |
| AC-005-04 | Us005StorageAcceptanceTest.java (2), e2e-04-slovenian-characters.spec.ts (2) | a159a1d, 6dbb663, fe8fc6d, a7a57e5, f6812a2 |
| AC-006-01 | Us006ParticipantEmailAcceptanceTest.java (3), e2e-01-external-registers.spec.ts | 2268a0f, bb8f246, d3ba2e4, 8be6444 |
| AC-006-02 | Us006ParticipantEmailAcceptanceTest.java | 2268a0f, bb8f246, d3ba2e4, 8be6444 |
| AC-006-03 | Us006MailUnavailableAcceptanceTest.java (2), Us006ParticipantEmailAcceptanceTest.java | 2268a0f, bb8f246, d3ba2e4, 8be6444 |
| AC-007-01 | Us007OrganizerNotificationAcceptanceTest.java (3), e2e-01-external-registers.spec.ts | f214eb7, bb8f246, d3ba2e4 |
| AC-007-02 | Us007OrganizerNotificationAcceptanceTest.java, e2e-01-external-registers.spec.ts (2) | f214eb7, bb8f246, d3ba2e4 |
| AC-008-01 | Us008ExportAcceptanceTest.java (3), e2e-05-organizer-exports.spec.ts (2) | 46de03c, a39fb3a, c32febe, 025e931, dc55b1a, 3fe8238 |
| AC-008-02 | Us008ExportAcceptanceTest.java (2), e2e-05-organizer-exports.spec.ts (2) | 46de03c, a39fb3a, c32febe, 025e931, dc55b1a, 3fe8238 |
| AC-008-03 | Us008ExportAcceptanceTest.java (2), e2e-04-slovenian-characters.spec.ts (2) | 46de03c, a39fb3a, c32febe, 025e931, dc55b1a, 3fe8238 |
| AC-008-04 | OrganizerHttpsOnlyAcceptanceTest.java (3), Us008ExportAcceptanceTest.java | 46de03c, a39fb3a, c32febe, 025e931, dc55b1a, 3fe8238 |
| AC-009-01 | Us009SearchAcceptanceTest.java (4), e2e-06-organizer-searches.spec.ts (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-02 | Us009SearchAcceptanceTest.java (3), e2e-06-organizer-searches.spec.ts (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-03 | Us009SearchAcceptanceTest.java (2), e2e-06-organizer-searches.spec.ts (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-04 | Us009SearchAcceptanceTest.java (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-05 | Us009SearchAcceptanceTest.java (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-06 | Us009SearchAcceptanceTest.java (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-07 | Us009SearchAcceptanceTest.java (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-08 | Us009SearchAcceptanceTest.java (4) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-09 | Us009SearchAcceptanceTest.java (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-10 | OrganizerHttpsOnlyAcceptanceTest.java (3), Us009SearchAcceptanceTest.java | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-11 | Us009SearchAcceptanceTest.java | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-12 | us009-organizer-page.test.tsx (3), e2e-06-organizer-searches.spec.ts (2) | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |
| AC-009-13 | us009-organizer-page.test.tsx | 85ba900, 1b0554e, 3e7ab2b, 0e13812, 0359d7b, 18a08e4 (+2) |

48 acceptance criteria; without a test: none; without a commit: none.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Medium | Semgrep (WARNING) | prototype-pollution-loop in `docs/02_contracts/validate.mjs` (walks `$ref` paths) | fixed in `d76ca46`: own properties only; re-scan clean |
| F-02 | Medium | Semgrep | `frontend/.npmrc` sets no `min-release-age` | accepted: the setting exists only from npm 11.10, the pinned npm 11.6.2 ignores it; every version is exact and installed from the committed lock file |
| F-03 | Low | Semgrep (WARNING, reclassified as false positive) | nginx `location /api/` without `internal` | accepted: it is the public API proxy and its upstream is fixed (`backend:8080`), so no client-controlled target exists |
| F-04 | Low | Gitleaks | 17 "generic-api-key" matches in phase 3 logs | accepted, false positive: Spring Boot's random development passwords printed by the bootstrap skeleton's throwaway test contexts; the application now defines its own user and logs none |
| F-05 | Medium | npm audit | `@vitest/mocker` Moderate advisories through vitest 4.1.11 | accepted, dev tooling only (D-08) |
| F-06 | Medium | npm audit | `qs` Moderate advisories through `@stryker-mutator/core` | accepted (PA-03, D-07) |
| F-07 | Low | Gitleaks | fixture password in `frontend/tests/acceptance/support/mockApi.ts` | accepted, false positive: a test-only value for a mocked backend |
| F-08 | Low | Dependency-Check | CVE-2025-7962 (angus-activation), CVE-2025-15104 (hibernate-validator) | pre-approved false positives (PA-01, PA-02, D-06), suppressed per artifact |

Counts as found: Critical 0, High 0, Medium 4, Low 4. Open after triage: none (F-01 fixed; the others accepted with the reasons above).

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | `validate.mjs` resolves `$ref` through own properties only | `node docs/02_contracts/validate.mjs` still valid; Semgrep re-run without the finding (`logs/06_semgrep-after-f01.json`) |

## Decisions

D-01..D-15 all have a resolution; D-08 (vitest advisories), D-10 (403 for plain-HTTP organizer requests) and D-11 (raw search text length) are listed as pending review.

## Manual tests for a human (repeated in the release notes)

A real reCAPTCHA submission with production keys (accepted and rejected); delivery through the real SMTP server to a real mailbox; the production nginx with Let's Encrypt (HTTPS, redirects, `/api` routing, fixed upstream `Host`, `TRUSTED_PROXIES`).
