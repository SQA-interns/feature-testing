# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-21 at commit `79fd249` (code) and the documentation commits after it. Raw output: `out/logs/06_*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Backend `./mvnw clean verify`: 230 tests, 0 failures (`06_backend-verify.log`); frontend `npm test`: 86 passed (`06_frontend-test.log`); e2e `npm run test:e2e` against a fresh `docker compose up`: 5 passed (`06_frontend-e2e.log`, again after F-01: `06_frontend-e2e-after-fix.log`). `03_acceptance-manifest.sha256`: 38 files recomputed (LF-normalised), 0 mismatches; none of them changed since the freeze commit `3f9a505`. |
| DoD-02 | met | Backend Spotless, PMD 7.7.0 (0 violations), CPD (0 clones), SpotBugs 4.10.4.1 (0 bugs) in `06_backend-verify.log`; frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`) exit 0 (`06_frontend-check.log`), `npm run build` exit 0; Semgrep 1.177.0 after F-01: 1 Medium, accepted (F-04). |
| DoD-03 | met | `docs/03_test-strategy.md` "Test measures" and "Quality gate": backend line 99.5 % (all levels, JaCoCo), PIT raw 95.8 % = adjusted 95.8 % over `si.konferenca.registration.**`, 0 survivor rounds; frontend line 100 % (V8), Stryker raw 94.9 %, adjusted 98.5 % (16 equivalents listed with reasons), 1 round. No production code or test in either mutation scope changed after phase 5 (only `frontend/scripts/run-e2e.mjs`, outside the scope), so nothing was re-measured. |
| DoD-04 | met | `ArchitectureTest`: 7 ArchUnit rules (AR-02 layering exactly as declared in `02_specification.md` section 2; AR-03 no slice cycles) pass. AR-01 by inspection: the frontend calls only relative `/api/...` URLs (`src/api/client.ts`). AR-04 `ChangedOptionsConfigAcceptanceTest`, `InvalidOptionsConfigAcceptanceTest`. AR-05 `JsonCopyFailureAcceptanceTest`, `RegisterParticipantTest`. AR-06 Flyway `V1__create_registration_tables.sql` with `ddl-auto: validate`. AR-07 the only key is `captchaSiteKey` from `/api/form-config` (`CaptchaVerificationAcceptanceTest`). AR-09 every acceptance and e2e test uses the contract's paths, fields and status codes. |
| DoD-05 | met | Findings below: no Critical or High open (F-01 High fixed in loop 1). Dependency-Check online with `NVD_API_KEY`: 67 dependencies, BUILD SUCCESS at `failBuildOnCVSS=7`, only the PA-01 and PA-02 suppressions (`06_backend-dependency-check.log`); `npm audit --omit=dev`: 0; full `npm audit`: 2 Moderate via `@stryker-mutator/core` (PA-03); Gitleaks over history and working tree: F-02 (fixed) and F-03 (Low). |
| DoD-06 | met | Runtime demonstration below (`06_runtime-demo.log`, `06_runtime-demo-2.log`). |
| DoD-07 | met | Traceability below: every AC has at least one test and at least one commit. |
| DoD-08 | phase 7 | READMEs are written and followed from a clean clone in phase 7. |
| DoD-09 | phase 7 | Release notes with the manual-test list in phase 7. |
| DoD-10 | met | `docs/decisions-log.md`: D-01..D-16 each have a resolution; D-11, D-12, D-13 are "pending review" with the applied option. `00_input-manifest.sha256`: 41 files, 0 mismatches. |
| DoD-11 | met | Phase 3 commits add only tests, test resources, documents, logs and one build change (`@types/node`, D-14), no production code; the freeze commit `3f9a505` adds only the manifest, and every file it lists was committed before it; phase 4 has commits naming US-001 to US-008; the manifest hashes match. |
| DoD-P01 | met | `POST /api/registrations` against the running stack: external 201 (`4e553dda-…`) and student 201 (`3e6b4cd3-…`); also E2E-01 and E2E-02 through the browser. |
| DoD-P02 | met | `psql`: both rows with type, names and institution (Slovenian characters unchanged); `/data/json-copies/<id>.json` exist on the named volume, content shown in the log. |
| DoD-P03 | met | Mailpit API: per registration one "Registration confirmed: Konferenca 2026" to the participant (no attachment) and one "New registration: Konferenca 2026" per organizer address with one JSON attachment. |
| DoD-P04 | met | `GET /api/export` with organizer credentials: 200, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`; the workbook read back has the 12 contract columns and one row per registration; without credentials 401, wrong password 401. |
| DoD-P05 | met | `CaptchaVerificationAcceptanceTest` (test mode off, a JDK `HttpServer` mocks the verification endpoint): accepted token 201, rejected token 400 with nothing stored, test token rejected without test mode; `GoogleCaptchaVerifierTest` (error status, malformed body, unreachable endpoint). |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local (`docker compose --env-file ../.env up -d --build`, fresh volumes) | container health; e2e suite E2E-01..05 | postgres, mailpit, backend, frontend healthy; 5/5 e2e passed |
| backend | local | `/actuator/health`, `/health/readiness`, `/health/liveness` (NFR-04) | `UP` each; the compose health checks use readiness (backend) and `/healthz` (frontend) |
| backend | local | external and student registration with Slovenian text (UTF-8 bodies) | 201 with id each; row in PostgreSQL and JSON copy unchanged (`Žagar`, `Črnuče`, `Čuk`, `Računalništvo`) |
| backend | local | invalid submission; student selecting `ev-industry-tour` | 400 with one entry per field; 400 for `options` |
| backend | local | emails in Mailpit | confirmation to the participant, notification with JSON attachment per organizer |
| backend | local | export with, without and with wrong credentials | 200 workbook / 401 / 401 |
| backend | local, `ORGANIZER_HTTPS_ONLY=true` | export over plain HTTP from the host (not loopback inside the container) | 403 `Organizer access requires HTTPS.` (SR-06); the loopback case is covered by `OrganizerHttpsOnlyAcceptanceTest` (BusyBox `wget` in the image cannot send Basic credentials) |
| backend | local | 20 kB body; 31 failed logins within a minute | 413; 429 (SR-03, SB-06) |
| frontend | local (nginx) | `/`, `/api/form-config` through nginx, spoofed `Host: evil.example` | 200; 200, because the upstream `Host` is fixed in `nginx.conf` (SR-08); security headers present |
| all | local | `docker compose down`, then `up` (NFR-02) | before: 5 rows, 5 copies; after: 5 rows, 5 copies |
| all | local | process users (SB-11) | backend `app`, frontend `nginx` |

A first attempt passed the demonstration bodies as command-line arguments; Git Bash on Windows handed them over in a non-UTF-8 code page and the backend correctly answered 400 "Request body is not valid JSON." The flows were repeated with UTF-8 files (`06_runtime-demo.log`); the browser path with Slovenian text is E2E-04.

## Security baseline and project requirements

| Item | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator`, `RegisterParticipant` | `RegistrationValidatorTest`; acceptance AC-001-02..07, AC-002-02..06 |
| SB-02 | `SecurityConfig` (role `ORGANIZER`, everything else denied) | `ExportAcceptanceTest` (AC-008-02), `WebSecurityIntegrationTest.unknownEndpointsAreDenied` |
| SB-03 | BCrypt in `SecurityConfig.organizerAccount`; secrets only from `.env` | inspection; `ConfigUnitTest` (secrets absent from `toString`); leak check below |
| SB-04 | external TLS proxy (`environments.md`); HTTPS-only organizer access | runtime 403; manual test in the release notes |
| SB-05 | JPA with bound parameters; plain-text mails; string cells in the export | inspection, `SmtpRegistrationNotifierTest`, AC-006-02, Semgrep |
| SB-06 | `RateLimitFilter` | `RateLimitFilterTest`, `WebSecurityIntegrationTest`, runtime 429 |
| SB-07 | `ApiExceptionHandler`, `spring.web.error.*`, logging of ids only | `ApiExceptionHandlerTest`, AC-004-04, runtime logs (no personal data; no stack traces in responses) |
| SB-08 | Dependency-Check, npm audit | this phase: no Critical or High |
| SB-09 | Semgrep, SpotBugs, PMD, Gitleaks | this phase |
| SB-10 | backend CSP, frame, nosniff and referrer headers; nginx headers | `WebSecurityIntegrationTest`, runtime headers |
| SB-11 | `USER app` and `USER nginx` in the images | runtime `id -un` |
| SB-12, SB-13 | only the fields of `security-requirements.md`; retention OQ-06 | inspection of `database.json`; release notes |
| SB-14 | consent checkboxes unchecked, stored with `given_at` | AC-001-08, AC-002-07, AC-005-01 |
| SR-01 | backend verification before anything is stored | `CaptchaVerificationAcceptanceTest`, AC-001-07, AC-002-06 |
| SR-02 | `StartupGuard` | `ConfigUnitTest` (test mode only in local/test, keys required without it) |
| SR-03 | `RateLimitFilter`, `RequestSizeFilter`, nginx `client_max_body_size` | unit, integration and runtime checks above |
| SR-04 | catalog check in `RegistrationValidator` | AC-001-05, AC-002-04 |
| SR-05 | `SmtpRegistrationNotifier` (text/plain, fixed subjects) | AC-006-02, `SmtpRegistrationNotifierTest` |
| SR-06 | `OrganizerHttpsOnlyFilter`, `StartupGuard` | AC-008-04, `ConfigUnitTest`, runtime 403 |
| SR-07 | export and organizer mail carry only registration fields | AC-007-02, AC-008-01, `SmtpRegistrationNotifierTest` |
| SR-08 | `proxy_set_header Host backend:8080` in `frontend/nginx.conf` | inspection, runtime request with a spoofed `Host` |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (`out/logs/06_trace-ac.md`):

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | external-registration.e2e.ts | a884103, 79c9f9d, fa35dbc, 8004c35, 3f9a505, 3d51ebb (+7) |
| AC-001-02 | **none** | a884103, 79c9f9d, fa35dbc, 8004c35, 3d51ebb, b3f9e1d (+6) |
| AC-001-03 | **none** | a884103, 79c9f9d, fa35dbc, 8004c35, 3d51ebb, b3f9e1d (+6) |
| AC-001-04 | **none** | a884103, 79c9f9d, fa35dbc, 8004c35, 3d51ebb, b3f9e1d (+6) |
| AC-001-05 | **none** | a884103, 79c9f9d, fa35dbc, 8004c35, 3d51ebb, b3f9e1d (+6) |
| AC-001-06 | **none** | a884103, 79c9f9d, fa35dbc, 8004c35, 3d51ebb, b3f9e1d (+6) |
| AC-001-07 | **none** | a884103, 79c9f9d, 1f68b17, fa35dbc, 8004c35, 3d51ebb (+7) |
| AC-001-08 | external-registration.e2e.ts, external-form.acceptance.test.tsx (3) | a884103, 79c9f9d, fa35dbc, 8004c35, 3d51ebb, b3f9e1d (+6) |
| AC-002-01 | student-registration.e2e.ts | 5704723, 730a212, 894cc31, b9b4974 |
| AC-002-02 | **none** | 5704723, 730a212, 894cc31, b9b4974 |
| AC-002-03 | **none** | 5704723, 730a212, 894cc31, b9b4974 |
| AC-002-04 | **none** | 5704723, 730a212, 894cc31, b9b4974 |
| AC-002-05 | **none** | 5704723, 730a212, 894cc31, b9b4974 |
| AC-002-06 | **none** | 5704723, 1f68b17, 730a212, 894cc31, b9b4974 |
| AC-002-07 | student-registration.e2e.ts, student-form.acceptance.test.tsx (4) | 5704723, 730a212, 894cc31, b9b4974 |
| AC-003-01 | **none** | aa5eaaa, b8610d6, b64d844, cbe2cad |
| AC-003-02 | **none** | aa5eaaa, b8610d6, b64d844, cbe2cad |
| AC-003-03 | **none** | aa5eaaa, b8610d6, b64d844, cbe2cad |
| AC-004-01 | external-registration.e2e.ts, student-registration.e2e.ts, confirmation.acceptance.test.tsx (2) | d2fbc19, 43ffe5d |
| AC-004-02 | field-errors.e2e.ts, confirmation.acceptance.test.tsx | d2fbc19, 43ffe5d |
| AC-004-03 | field-errors.e2e.ts, confirmation.acceptance.test.tsx | d2fbc19, 43ffe5d |
| AC-004-04 | confirmation.acceptance.test.tsx (2) | d2fbc19, 43ffe5d |
| AC-005-01 | external-registration.e2e.ts | 156a9cd, 2ad9700, b3f9e1d, 81b4659, 2523c20 |
| AC-005-02 | external-registration.e2e.ts | 156a9cd, 2ad9700, b3f9e1d, 81b4659, 2523c20 |
| AC-005-03 | **none** | 156a9cd, 2ad9700, b3f9e1d, 81b4659, 2523c20 |
| AC-005-04 | slovenian-characters.e2e.ts | 156a9cd, 2ad9700, b3f9e1d, 81b4659, 2523c20 |
| AC-006-01 | external-registration.e2e.ts | e9e3f8c, 721627b |
| AC-006-02 | **none** | e9e3f8c, 721627b |
| AC-006-03 | **none** | e9e3f8c, 721627b |
| AC-007-01 | external-registration.e2e.ts | 9e155fd, 721627b |
| AC-007-02 | external-registration.e2e.ts | 9e155fd, 721627b |
| AC-008-01 | organizer-export.e2e.ts | a884103, 6731120, 21fb0f6, a5bca86, 1f4c3c7, f338b93 |
| AC-008-02 | organizer-export.e2e.ts | a884103, 6731120, 21fb0f6, a5bca86, 1f4c3c7, f338b93 |
| AC-008-03 | slovenian-characters.e2e.ts | a884103, 6731120, 21fb0f6, a5bca86, 1f4c3c7, f338b93 |
| AC-008-04 | **none** | a884103, 6731120, 21fb0f6, 3f9a505, a5bca86, 1f4c3c7 (+1) |

The script matches only the form `AC-nnn-nn`. The backend acceptance tests name the AC in their method names as `ac_nnn_nn_…`, because Java names cannot contain `-`. So the 18 rows marked **none** do have these frozen tests (F-08, `out/logs/06_trace-ac-backend.md`; also the AC → test table in `docs/03_test-strategy.md`):

| AC | Backend test classes with `ac_nnn_nn_` methods |
|---|---|
| AC-001-02 | ExternalRegistrationAcceptanceTest |
| AC-001-03 | ExternalRegistrationAcceptanceTest |
| AC-001-04 | ExternalRegistrationAcceptanceTest |
| AC-001-05 | ExternalRegistrationAcceptanceTest |
| AC-001-06 | ExternalRegistrationAcceptanceTest |
| AC-001-07 | CaptchaVerificationAcceptanceTest, ExternalRegistrationAcceptanceTest |
| AC-002-02 | StudentRegistrationAcceptanceTest |
| AC-002-03 | StudentRegistrationAcceptanceTest |
| AC-002-04 | StudentRegistrationAcceptanceTest |
| AC-002-05 | StudentRegistrationAcceptanceTest |
| AC-002-06 | CaptchaVerificationAcceptanceTest, StudentRegistrationAcceptanceTest |
| AC-003-01 | FormConfigAcceptanceTest |
| AC-003-02 | ChangedOptionsConfigAcceptanceTest |
| AC-003-03 | InvalidOptionsConfigAcceptanceTest |
| AC-005-03 | JsonCopyFailureAcceptanceTest |
| AC-006-02 | ParticipantEmailAcceptanceTest |
| AC-006-03 | MailUnavailableAcceptanceTest |
| AC-008-04 | OrganizerHttpsOnlyAcceptanceTest |

Every AC has at least one commit.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | High | Semgrep `spawn-shell-true` (ERROR) | `frontend/scripts/run-e2e.mjs` started `npx` with `shell: true` on Windows and forwarded its arguments | fixed in `91034b2` (loop 1): the Playwright CLI is started with `process.execPath`, without a shell; e2e 5/5, Semgrep no longer reports it |
| F-02 | Low | Gitleaks `generic-api-key` | committed phase 3/4 logs held Spring Boot's "Using generated security password" (random per test JVM, never valid anywhere else) | fixed in `79fd249` (loop 2): redacted in the working tree; the copy in the git history is accepted as Low, the password belonged to processes that no longer exist |
| F-03 | Low | Gitleaks `generic-api-key` | `TestStack.java:23`: the fixed organizer password of the frozen acceptance harness, used only inside Testcontainers | accepted: false positive, a test fixture rather than a deployment secret; the file is frozen |
| F-04 | Medium | Semgrep `npm-missing-minimum-release-age` | `frontend/.npmrc` sets no minimum release age | accepted (D-16): the setting needs npm 11.10, the pinned npm is 11.6.2; exact pins, committed lock file and `npm ci` |
| F-05 | Medium | `npm audit` | Moderate advisories on `qs` via `@stryker-mutator/core` (dev only) | accepted per PA-03 (D-06); `npm audit --omit=dev` reports 0 |
| F-06 | Low | Dependency-Check | CVE-2025-7962 on `angus-activation` 2.0.3 | PA-01 (D-04): false positive, suppressed for that artifact |
| F-07 | Low | Dependency-Check | CVE-2025-15104 on `hibernate-validator` 9.1.3.Final | PA-02 (D-05): false positive, suppressed for that artifact |
| F-08 | Low | `trace-ac.mjs` | the backend test names carry the AC ids as `ac_nnn_nn`, which the script does not match | accepted: the mapping is above and in `docs/03_test-strategy.md`; the frozen tests stay unchanged |

Semgrep also reported a partial parse of the vendored `backend/mvnw` script (information, not a finding). The Semgrep rule of PA-04 (basic authentication) did not fire.

Counts as found: Critical 0, High 1, Medium 2, Low 5. Open after triage: Critical 0, High 0, Medium 0.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 (14:39–14:46 UTC) | F-01 | `run-e2e.mjs` runs `node …/@playwright/test/cli.js` | `06_frontend-e2e-after-fix.log`: 5 passed; `06_semgrep-after-fix.json`: 1 result (F-04) |
| 2 (14:46 UTC) | F-02 | redacted the generated-password lines in 10 logs | `06_gitleaks-dir-after-fix.json`: only F-03 remains |

## Leak and log checks

- `.env` values in tracked or untracked, not ignored files (skill step 8): no file listed. In commit messages: `0`.
- Backend container log of the demonstration (`06_backend-container.log`): no names, emails or other submitted values. The error responses seen in the demonstration contain no class names, stack traces or SQL.
- Playwright traces (they may hold the organizer `Authorization` header) are deleted after every run and `test-results/` is git-ignored.

## Decisions

Every record D-01..D-16 has a resolution; D-11, D-12 and D-13 are pending review with the conservative option applied (`docs/decisions-log.md`).
