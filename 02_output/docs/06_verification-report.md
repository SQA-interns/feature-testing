# Verification report

> Written in: phase 6 · Source: skill `general/skills/verify-release` · Agent: writes

**This is a self-check by the agent that built the system, not an independent review.**

Run `tjan/single-agent/sdd/v002/run-2`, verified on 2026-10-27 against the code at the end of phase 5 (no production code changed in phases 5 and 6). Raw output is in `out/logs/06_*`.

## Summary

| Item | Result |
|---|---|
| Frozen and input manifests | 26/26 and 51/51 hashes match (`06_manifest-check.log`) |
| Full test suite | backend 301/301, frontend 74/74, end-to-end 5/5 |
| Checks (ES-05) | format, lint, type check, PMD, SpotBugs: no errors |
| Quality gate | met for both components in phase 5, not re-measured (no code or test changed since) |
| Runtime demonstration | DoD-P01..P04, NFR-02, NFR-04, SR-02 shown on the local stack |
| Scanners | Dependency-Check (online): 0 unsuppressed; npm audit: 5 Moderate, dev only; Semgrep: 1 ERROR, 1 WARNING; Gitleaks: 20 matches, none a real secret |
| `.env` leak check | no file and no commit message contains a `.env` value |
| Traceability | 35/35 AC have a test and a commit |
| Open Critical/High | none (F-01 lowered to Low by the human, D-18) |

## 1. Hashes (step 2)

`docs/00_input-manifest.sha256`: 51 files, 0 mismatches. `docs/03_acceptance-manifest.sha256`: 26 files, 0 mismatches (re-frozen under D-16 in `44fe78a`, documented in `docs/03_test-strategy.md`). No file under `01_input/` or in README sections 1 and 2 changed.

## 2. Tests and checks (step 3)

| Suite | Command | Result | Log |
|---|---|---|---|
| Backend: acceptance 89, unit 201, integration 5, architecture 6 | `./mvnw -B clean package` | 301 passed, 0 failed | `06_backend-final-run.log` |
| Backend checks | `./mvnw -B spotless:check pmd:check spotbugs:check` | clean, SpotBugs 0 bugs | `06_backend-check.log` |
| Frontend: acceptance 7, unit/component 67; checks; build | `npm test && npm run check && npm run build` | 74 passed; Prettier, ESLint, tsc clean; build ok | `06_frontend-final-run.log` |
| End-to-end E2E-01..05 | `npx playwright test` against the rebuilt stack | 5 passed | `06_e2e-final-run.log`, `06_stack-up.log` |

Coverage and mutation (phase 5, `docs/03_test-strategy.md`): backend line 99.44 %, PIT 92.71 % raw = adjusted; frontend line 100 %, Stryker 90.30 % raw = adjusted; 0 survivor rounds. Not re-measured: no production code or test changed after the phase 5 runs.

## 3. Runtime demonstration (step 4)

Local stack `docker compose up` (project `registration-sdd-run2`), driven over `http://127.0.0.1:8080` and the Mailpit API (`06_runtime-demo.log`, organizer addresses masked).

| Item | What was run | Observed |
|---|---|---|
| NFR-04, ES-09 | `ps`; `/actuator/health`, `/liveness`, `/readiness` inside the backend | all four services `healthy`; each endpoint `UP`; readiness includes the database |
| SB-11 | `id` in the containers | backend uid 10001 `app`, frontend uid 101 `nginx` |
| SB-10 | headers of `/` and `/api/form-config` | CSP, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` on both; `no-store` on the API |
| DoD-P01 | one external and one student `POST /api/registrations` | both 201 with ids and options |
| DoD-P02 | `psql` and the JSON copy directory | both rows with their options; both `<id>.json` present |
| DoD-P03 | Mailpit API | 2 × "Registration confirmed: Conference 2026" to the participants, 2 × "New registration: Conference 2026" to the organizer, each with 1 attachment |
| DoD-P04 | export with organizer credentials, without, with a wrong password | 200 `.xlsx` attachment, valid zip, both ids and `Šuštar` in the sheet; 401 `Basic realm="registration-export"` with `{"message":"Authentication required."}` twice |
| NFR-02 | `docker compose down` (no `-v`), `up --wait` | 14 rows and 14 JSON copies before and after; both demo rows and copies present |
| SR-02 | backend started with `APP_ENVIRONMENT=production` and test mode on; with empty keys; with `staging` (`06_sr02-startup-refusal.log`) | all three refuse to start (exit 1, `IllegalStateException` with the S4.5 message) |

A first SR-02 attempt passed the overrides as one malformed argument (zsh does not split an unquoted variable), so the container ran as `local` and started; the control with `staging` exposed this and the corrected run above replaced it. It was a defect of the demonstration command, not of the system.

## 4. Security evidence (step 5)

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `domain.RegistrationValidator`, `EmailAddress`, `ConferenceConfigLoader` (backend decides; frontend validation is convenience) | acceptance AC-001-02..07, AC-002-02..06; `RegistrationValidatorTest` |
| SB-02 | `web.SecurityConfiguration`: export needs `ORGANIZER`, everything not listed is denied | AC-008-01/02; `SecurityWebTest`; runtime 401 |
| SB-03 | BCrypt 12 hash of `ORGANIZER_PASSWORD` at startup; secrets only from variables (`application.yml`, no defaults) | inspection; `ApplicationConfigurationTest` (no secret in `toString`) |
| SB-04 | production TLS at the external nginx (spec S8); SR-06 filter | inspection; AC-008-04 |
| SB-05 | JPA with bound parameters, `RegistrationCopy` via Jackson, plain-text mail with one parsed recipient, string-only export cells | AC-006-02, AC-005-*; `PoiRegistrationWorkbookTest` (`=1+1` stays text); Semgrep |
| SB-06, SR-03 | `web.RateLimitFilter` (per client and group, 429 + `Retry-After`), `RequestSizeFilter` (413), nginx `client_max_body_size` | `WebFiltersTest`; acceptance size and rate tests |
| SB-07, ES-07 | `ApiExceptionHandler` fixed messages; failures logged by class name or registration id only | `ApiMappingTest`; backend container log: 0 personal-data hits (`06_backend-container.log`) |
| SB-08 | Dependency-Check, npm audit | section 5 |
| SB-09 | SpotBugs, PMD, Semgrep, Gitleaks | section 5 |
| SB-10 | Spring Security headers (API), nginx headers (static) | `SecurityWebTest`; runtime headers |
| SB-11 | non-root images (uid 10001, nginx) | runtime `id` |
| SB-12, SB-13 | only the fields of BR-01; purposes and retention in spec S9; retention note goes into the release notes (OQ-06) | inspection |
| SB-14 | consents never preselected, given with timestamp | AC-001-06, AC-002-05; frontend acceptance; JSON copy `givenAt` |
| SR-01 | `adapter.captcha.SiteverifyCaptchaVerifier` on the backend | `RecaptchaVerificationAcceptanceTest` against a mocked siteverify (DoD-P05); `CaptchaVerifierTest` |
| SR-02 | `config.ApplicationConfiguration.checkEnvironment` via the `formSettings` bean | `ApplicationConfigurationTest`; runtime refusal (section 3) |
| SR-04 | `RegistrationValidator.options` against the catalog | AC-001-05, AC-002-04 |
| SR-05 | `MailTexts.line`, `InternetAddress(…, strict)`, fixed subjects | AC-006-02; `MailAdapterTest` |
| SR-06 | `web.HttpsOnlyFilter` (403 before credentials, loopback exempt) | AC-008-04; `WebFiltersTest` |
| SR-07 | export columns of S4.8, organizer text of `emails.md` | AC-007-01, AC-008-01; `PoiRegistrationWorkbookTest` |
| SR-08 | nginx `proxy_set_header Host backend`, `X-Forwarded-Host ""` | inspection of `frontend/nginx.conf` |

## 5. Scanners (step 6)

| Tool | Scope | Result | Raw output |
|---|---|---|---|
| OWASP Dependency-Check 12.1.0, online with NVD key | backend runtime and test dependencies | 0 unsuppressed; suppressed: CVE-2025-7962 `angus-activation` (PA-01, D-09), CVE-2025-15104 `hibernate-validator` (PA-02, D-10) | `06_dependency-check.log`, `06_dependency-check.json` |
| npm audit 11.6.2 | frontend | all: 5 Moderate (`qs`, `typed-rest-client` via Stryker: PA-03, D-05; `vitest`, `@vitest/mocker`, `@vitest/coverage-v8`: D-06); `--omit=dev`: 0 | `06_npm-audit.json`, `06_npm-audit.txt`, `06_npm-audit-prod.txt` |
| Semgrep 1.177.0, `p/default` | `02_output` (134 files) | 1 ERROR (F-01), 1 WARNING (F-02); 1 partial-parse error on `backend/mvnw` (generated wrapper script) | `06_semgrep.json` |
| Gitleaks v8.30.1 | git history and working tree | 20 `generic-api-key` matches in committed phase 3 logs (F-03); working tree adds `.env` (ignored by git, never committed) | `06_gitleaks-history.log`, `06_gitleaks-dir.log` |
| SpotBugs 4.10.4.1, PMD 3.26.0 | backend | 0 | `06_backend-check.log` |

## 6. Logs, error responses and secrets (step 8)

- Error bodies seen during testing and the demonstration hold fixed messages only (401, 403, 413, 429, 400 field errors, 500 general message).
- Backend container log: 0 occurrences of the demo names or addresses; the only per-registration log lines name the registration id (OQ-03).
- `.env` leak check of the skill: the file command listed no file name; the commit-message command printed `0`. The runtime log was checked after masking the organizer address that Mailpit returned.

## 7. Traceability (step 9)

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (`06_trace-ac.log`):

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | ExternalRegistrationAcceptanceTest.java, e2e-01-external-registers.spec.ts | 9788bf0, e933b1c, 83e29f5, b945401, f67e8ac, 9d0fda1 (+2) |
| AC-001-02 | ExternalRegistrationAcceptanceTest.java | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-001-03 | ExternalRegistrationAcceptanceTest.java | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-001-04 | ExternalRegistrationAcceptanceTest.java | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-001-05 | ExternalRegistrationAcceptanceTest.java | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-001-06 | ExternalRegistrationAcceptanceTest.java | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-001-07 | ExternalRegistrationAcceptanceTest.java, RecaptchaVerificationAcceptanceTest.java (4) | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-001-08 | e2e-01-external-registers.spec.ts, us001-external-form.test.tsx | 9788bf0, 83e29f5, b945401, f67e8ac, 9d0fda1, 307e53b (+1) |
| AC-002-01 | StudentRegistrationAcceptanceTest.java, e2e-02-student-registers.spec.ts | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-002-02 | StudentRegistrationAcceptanceTest.java | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-002-03 | StudentRegistrationAcceptanceTest.java | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-002-04 | StudentRegistrationAcceptanceTest.java | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-002-05 | StudentRegistrationAcceptanceTest.java | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-002-06 | RecaptchaVerificationAcceptanceTest.java (2), StudentRegistrationAcceptanceTest.java | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-002-07 | e2e-02-student-registers.spec.ts, us002-student-form.test.tsx | 0faeb3e, 83e29f5, b945401, 5a08e80 |
| AC-003-01 | ConferenceOptionsAcceptanceTest.java | ee2e1bb, 6842e47, 307e53b |
| AC-003-02 | ConferenceOptionsAcceptanceTest.java | ee2e1bb, 6842e47, 307e53b |
| AC-003-03 | ConferenceOptionsAcceptanceTest.java | ee2e1bb, 6842e47, 307e53b, 5f77396 |
| AC-004-01 | e2e-01-external-registers.spec.ts, e2e-02-student-registers.spec.ts, us004-confirmation.test.tsx | e724a39, 021e7c2, f763fd6, 5a08e80 |
| AC-004-02 | e2e-03-field-errors.spec.ts, us004-confirmation.test.tsx | e724a39, 021e7c2, f763fd6, 5a08e80 |
| AC-004-03 | e2e-03-field-errors.spec.ts, us004-confirmation.test.tsx | e724a39, 021e7c2, f763fd6, 5a08e80 |
| AC-004-04 | us004-confirmation.test.tsx | e724a39, 021e7c2, f763fd6, 5a08e80 |
| AC-005-01 | RegistrationStorageAcceptanceTest.java, e2e-01-external-registers.spec.ts | 0d849b4, f67e8ac, f0d9e0e, bb60236 |
| AC-005-02 | RegistrationStorageAcceptanceTest.java, e2e-01-external-registers.spec.ts | 0d849b4, f67e8ac, f0d9e0e, bb60236 |
| AC-005-03 | RegistrationStorageAcceptanceTest.java | 0d849b4, f67e8ac, f0d9e0e, bb60236 |
| AC-005-04 | RegistrationStorageAcceptanceTest.java, e2e-04-slovenian-characters.spec.ts | 0d849b4, f67e8ac, f0d9e0e, bb60236 |
| AC-006-01 | ParticipantEmailAcceptanceTest.java, e2e-01-external-registers.spec.ts | 8bb1eed, a6f284d, 6b6926e |
| AC-006-02 | ParticipantEmailAcceptanceTest.java | 8bb1eed, a6f284d, 6b6926e |
| AC-006-03 | MailServerDownAcceptanceTest.java | 8bb1eed, a6f284d, 6b6926e |
| AC-007-01 | OrganizerNotificationAcceptanceTest.java, e2e-01-external-registers.spec.ts | 065f190, a6f284d, 6b6926e |
| AC-007-02 | OrganizerNotificationAcceptanceTest.java, e2e-01-external-registers.spec.ts | 065f190, a6f284d, 6b6926e |
| AC-008-01 | RegistrationExportAcceptanceTest.java, e2e-05-organizer-export.spec.ts | 6ec6548, 6aae807, c2f8df1, 6b6926e |
| AC-008-02 | RegistrationExportAcceptanceTest.java, e2e-05-organizer-export.spec.ts | 6ec6548, 6aae807, c2f8df1, ce5c072, 6b6926e |
| AC-008-03 | RegistrationExportAcceptanceTest.java, e2e-04-slovenian-characters.spec.ts | 6ec6548, 6aae807, c2f8df1, 6b6926e |
| AC-008-04 | RegistrationExportAcceptanceTest.java | 6ec6548, e933b1c, 6aae807, c2f8df1, 6b6926e |

35 acceptance criteria; without a test: none; without a commit: none.

## 8. Findings (step 7, 11)

Severity by `general/quality/severity-scale.md`; tools are used as reported.

| ID | Severity | Source | Finding | Status |
|---|---|---|---|---|
| F-01 | Low (reported High by Semgrep ERROR; lowered under D-18) | Semgrep `yaml.openapi.security.use-of-basic-authentication`, `docs/02_contracts/openapi.yaml:108`; implemented in `web.SecurityConfiguration` | The organizer export uses HTTP Basic authentication; the rule calls Basic weak and suggests OAuth2, OIDC or mTLS. | accepted: the human chose option 1 of D-18 ("do recomended"); the evidence is in D-18; the release notes name HTTP Basic, TLS and a strong password |
| F-02 | Medium (Semgrep WARNING) | Semgrep `generic.nginx.security.missing-internal`, `frontend/nginx.conf:38` | `location /api/` has `proxy_pass` without `internal`. | accepted: false positive. The upstream is the constant `http://backend:8080` (set in the config, no request data in the target), so the location cannot be steered to another host; `internal` would make `/api/` unreachable for the browser and break every flow. |
| F-03 | Low | Gitleaks `generic-api-key`, 19 lines in 8 committed phase 3 logs (`03_backend-acceptance-run.log`, `03_us00*-first.log`) | Lines `Using generated security password: <uuid>` printed by Spring Boot for the bootstrap skeleton's default user. | accepted: each value was a random password of a user that existed only inside one test JVM in phase 3; the current code has no such user, no `.env` value is involved, and history is not rewritten. |
| F-04 | Medium | npm audit | 5 Moderate advisories in dev-only tooling. | accepted per D-05 (PA-03) and D-06; production audit is clean; listed in the release notes. |
| F-05 | Low | PIT and Stryker survivors in security, validation and persistence code | Classified one by one below; none shows a defect. | accepted: the gate is met and the stop rule forbids further tests for score; the behaviour each survivor touches is covered as stated. |
| F-06 | Low | `general/working-rules.md` (commit size) | Commits `0929294` (633 lines) and `e5b5b50` (569 lines) exceed 400 lines. | accepted: recorded as divergences in `run-log.json`; history is not rewritten. |

### Surviving mutants in security, validation and persistence code (F-05)

PIT keeps one Spring application context per test class within a minion, so a mutant in a `@Bean` method that runs only while the context is built is often not activated for the cached context ("context cache" below).

| Class / file | Line, mutation | Classification |
|---|---|---|
| `web.SecurityConfiguration` | L62 two negated conditionals; L64–68 removed CORS calls | context cache: `SecurityWebTest` asserts the CORS allow and refuse outcomes, and only its first mutant was activated (1 kill). Low. |
| `config.ApplicationConfiguration` | L25 removed call to `checkEnvironment` | test gap on the wiring (unit tests call the check directly); wiring shown at runtime (SR-02 refusals, section 3). Low. |
| `web.RateLimitFilter` | L51 removed `sweep`; L88, L90 sweep conditions | memory housekeeping of old windows; limits and `Retry-After` are unchanged and tested. Low. |
| `web.RateLimitFilter` | L75 `"registration"` → `""` | equivalent: the default group still uses the registration limit, and the key prefix differs from the other groups either way. |
| `web.RequestSizeFilter` stream | L68 `b >= 0` → `b > 0` | test gap: a NUL byte read one at a time would not be counted; JSON parsing reads in blocks (L77 counts those) and the declared length is checked first. Low. |
| `web.RequestSizeFilter` stream | L77 `n > 0` → `n >= 0` | equivalent: counting 0 bytes changes nothing. |
| `web.RequestSizeFilter` stream | `isFinished`, `isReady` → `true`; `setReadListener` (no coverage) | delegation used only by asynchronous reads, which the API does not use. Low. |
| `web.JsonErrors` | L16 removed `setCharacterEncoding` | equivalent for the shipped messages, which are ASCII constants. |
| `domain.EmailAddress` | L19 `at <= 0` → `at < 0` | equivalent: an empty local part fails the local-part pattern. |
| `adapter.jsoncopy.FileJsonCopyStore` | L47 removed `force` | durability on power loss; not observable in a test. Low. |
| `adapter.persistence.RegistrationEntity` | `isNew` → `true` / `false`; `getId` → `null` (no coverage) | equivalent in this application: entities are only inserted, never updated; with `false` Spring Data merges instead of persisting, which inserts the same row. |
| `validation.ts` (frontend) | L34 six mutants of the `@` guard | five equivalent (the patterns exclude `@` and an empty local part); removing the whole guard would accept a value without `@` that looks like a domain (`a.b.si`) on the client only; the backend rejects it (AC-001-04). Low. |
| `Captcha.tsx` (frontend) | L81, L100, L104 | equivalent in the component's lifecycle (container always mounted, stable props, `grecaptcha` present whenever a widget id exists); the token is verified on the backend (SR-01). |

Survivors outside these areas (mail transfer-encoding header, UI literals and React state initialisers in `RegistrationForm.tsx` and `App.tsx`) are counted in the scores and listed in the mutation reports (`out/logs/05_mutation/`).

## 9. Definition of done (step 10)

| ID | Evidence | Result |
|---|---|---|
| DoD-01 | section 2; section 1 | met |
| DoD-02 | `06_backend-check.log`, `06_frontend-final-run.log` | met |
| DoD-03 | `docs/03_test-strategy.md` "Test measures": scope, raw and adjusted scores, 0 rounds | met |
| DoD-04 | `ArchitectureTest` (5 rules of S2, cycle check) in the backend run | met |
| DoD-05 | section 8 | met: no open Critical or High (F-01 lowered under D-18) |
| DoD-06 | section 3 | met |
| DoD-07 | section 7 | met |
| DoD-08 | READMEs are written in phase 7 | checked at the phase 7 gate (clone log) |
| DoD-09 | release notes are written in phase 7 | checked at the phase 7 gate |
| DoD-10 | decisions log: D-01..D-18 resolved, D-11..D-14 marked pending review; manifests match | met |
| DoD-11 | phase 3 commits change no production source; the freeze commit `e933b1c` adds only the manifest and every listed file was committed before it; every user story has build commits (US-001 6 … US-008 2); manifest matches | met; commit sizes F-06 |
| DoD-P01..P04 | section 3 | met |
| DoD-P05 | `RecaptchaVerificationAcceptanceTest` (accepted, rejected, HTTP 500, test token with test mode off) | met |

## 10. Decisions (step 12)

D-01..D-10 and D-15..D-18 are resolved (D-18 by the human, option 1); D-11..D-14 are resolved by the agent and marked pending review.

## 11. Verify → fix → re-verify loops

None: the only High (F-01) was lowered by the human under D-18 rather than fixed; no code changed in phase 6, so nothing was re-verified or re-measured.
