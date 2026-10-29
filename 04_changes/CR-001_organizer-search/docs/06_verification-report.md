# Verification report: CR-001

> Written in: phase 6 of the change run · Source: skill `general/skills/verify-release` with the additions of `general/change-phases.md` · Agent: writes

**This is a self-check by the agent that built the change, not an independent review.**

CR-001 Organizer registration search on baseline `4d664d2`, verified on 2026-10-29. No production code or test changed after phase 5, so coverage and mutation are not re-measured. Raw output is in `out/logs/cr-001_06_*`.

## Summary

| Item | Result |
|---|---|
| Hashes | CR input manifest (53 files), `input.sha256`, CR acceptance manifest (8 files) and baseline acceptance manifest (26 files) all match; no baseline frozen test file changed since base (supersession rule holds trivially: no MODIFIED or REMOVED id). Baseline input manifest differs only in `run-config.md` (ignored) and three statistics tools (D-22) |
| CR lint `--signed` | 1 error: the `docs/` folder that `change-phases.md` requires (D-28, F-08); with `docs/` aside 0 errors, 1 warning (30 criteria, approved) |
| Tests and checks | backend 335/335 with Spotless, PMD, SpotBugs clean; frontend 121/121 with Prettier, ESLint, tsc and build clean; end-to-end 6/6 in the runtime demo stack (phase 4 run and demo below) |
| Quality gate | met: backend line 99.53 %, mutation 95.13 %; frontend line 99.63 %, mutation 91.26 % (phase 5, `docs/03_test-strategy.md`) |
| Runtime | DoD-P01..P04, NFR-02, NFR-04 and the CR flows shown on the rebuilt local stack |
| Scanners | Dependency-Check online 0; npm audit 5 Moderate dev-only, production 0; Semgrep 2 (both baseline F-01, F-02); Gitleaks: no match in any commit of this run |
| Secrets | **Critical F-07 open**: the `ORGANIZER_EMAILS` value is in two committed statistics files from before the CR (D-29, waiting for the human). Commit messages clean; no file of this run holds a `.env` value |
| Traceability | 30/30 criteria with tests and commits |
| Decisions | D-19..D-28 resolved or pending review; D-29 open (blocking) |

## 1. Hashes (step 2, supersession rule)

`out/logs/cr-001_06_manifest-check.log`. Every line of `docs/00_input-manifest.sha256`, `cr/input.sha256` and `docs/03_acceptance-manifest.sha256` matches (LF-normalised). `git diff --stat 4d664d2 -- <baseline frozen files>` is empty. The baseline `00_input-manifest.sha256` differs only as recorded in D-22 plus `run-config.md`.

## 2. Tests and checks (step 3)

- Backend `./mvnw -B clean spotless:check pmd:check verify spotbugs:check`: 335 tests, 0 failures (acceptance 123, unit 201, integration 5, architecture 6, incl. the ArchUnit rules of S2), SpotBugs 0, PMD 0 (`cr-001_06_backend-final-run.log`).
- Frontend `npm run check && npm test && npm run build`: 121 tests, 0 failures (`cr-001_06_frontend-final-run.log`).
- End-to-end: 6/6 on the final code (`cr-001_04_e2e-run-1.log`; no production code changed since).

## 3. Runtime demonstration (step 4)

Script run from the scratchpad against `docker compose` (`COMPOSE_PROJECT_NAME=registration-sdd-run2`); log `out/logs/cr-001_06_runtime-demo.log` (mail recipients written as `<organizer address>` / `<participant address>`).

| Requirement | Observed |
|---|---|
| NFR-04 | all four services `healthy`; readiness `{"status":"UP"}` |
| DoD-P01 | external and student registration: 201, 201 |
| DoD-P02 | 2 rows (`EXTERNAL Ana Koš`, `STUDENT Luka Kranjc`), each with its JSON copy |
| DoD-P03 | per registration one participant confirmation and one organizer notification with 1 attachment |
| DoD-P04 | export with access 200, xlsx with header + 2 rows; without access 401 |
| BR-09, AC-009-01 | `GET /organizer` 200, the SPA shell |
| AC-009-03, D-26 | no credentials 401 without `WWW-Authenticate`; wrong credentials 401 |
| AC-009-06, AC-009-24 | count 2, newest first `Kranjc, Koš`; meal count 2; workshop counts list the inactive `Workshop: Legacy systems` with 0 |
| AC-009-10, AC-009-11, NFR-01 | `kos` → `Koš`; `koš` → `Koš`; text unchanged |
| AC-009-18, AC-009-25 | `type=STUDENT` → `Kranjc`, meal count 1 |
| AC-009-16, AC-009-22 | 101 characters → 400; `type=VISITOR`, `workshop=meal-lunch-vegetarian` → 400 `type/UNKNOWN_TYPE`, `workshop/WRONG_CATEGORY` |
| NFR-02 | after `docker compose down` and `up`: 2 rows, JSON copies present (20 files: the volume also holds copies of earlier e2e runs; the demo emptied only the table), search count 2 |
| SR-03, D-23 | 32 calls in one minute: 29 × 401, 3 × 429 (limit 30 per client, failed logins included) |

## 4. Security evidence (step 5)

The baseline table (`02_output/docs/06_verification-report.md` section 4) still holds for every SB and SR; its evidence is reused where the code did not change. Items the CR touches:

| ID | Implemented in | Checked by |
|---|---|---|
| SB-02, SR-09, BR-08 | `web.SecurityConfiguration`: `GET /api/organizer/registrations` needs `ORGANIZER`, same account as the export | AC-009-03 (3 variants), runtime 401 |
| SR-06 | `web.HttpsOnlyFilter.isOrganizerPath` covers the search | AC-009-05 |
| SR-03, SB-06 | `web.RateLimitFilter` group `organizer-search`, `RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE` (30) | `OrganizerSearchRateLimitAcceptanceTest`, runtime 429 |
| SR-07, BR-10 | `web.OrganizerSearchResponse`: only the export row's data; `NON_NULL` leaves out the other type's fields | AC-009-07 (exact key sets) |
| SR-09 (credentials) | `OrganizerPage` keeps them in React state; `api.basicAuthorization` builds the header per call | AC-009-30 (storage and cookie empty, reload shows the login form) |
| SR-09 (literal text), SB-05 | React text only, no `dangerouslySetInnerHTML` (`grep` over `frontend/src`: none) | AC-009-29; Semgrep |
| SB-07 | `ApiExceptionHandler.searchRejected`: fixed message, field codes only | AC-009-16, AC-009-22 (no registration data in the body) |

## 5. Scanners (step 6)

| Tool | Scope | Result | Raw output |
|---|---|---|---|
| OWASP Dependency-Check 12.1.0, online (NVD) | backend, 67 dependencies | 0 after the baseline suppressions (D-09, D-10) | `cr-001_06_dependency-check.json` |
| npm audit (npm 11.6.2) | frontend | 5 Moderate in dev tooling (D-05, D-06); `--omit=dev`: 0 | `cr-001_06_npm-audit.json`, `cr-001_06_npm-audit-prod.txt` |
| Semgrep 1.177.0 `p/default` | `02_output` (git-tracked) | ERROR `use-of-basic-authentication` at `openapi.yaml:192` (the same `organizerBasic` scheme, now also used by the search, F-01); WARNING `missing-internal` `nginx.conf:38` (F-02); 4 parse errors on JSON logs and `mvnw`, no rule effect | `cr-001_06_semgrep.json` |
| Gitleaks v8.30.1 | git history (110 commits), `02_output` directory | history 409 matches, none in a commit of this run: 389 `oauth-2025-04-20` header strings and 5 quoted `Using generated security password` lines in the transcript backup of `a82fcea`, 20 in the baseline phase 3 logs (F-03); directory 20 = the same logs | `cr-001_06_gitleaks-history.json`, `cr-001_06_gitleaks-dir.log` |

## 6. Logs, error responses and secrets (step 8)

- Backend container log of the demo (40 lines, `cr-001_06_backend-container.log`): 0 hits for the demo names, emails or study programme, 0 hits for any `.env` value.
- Error bodies of the search (400, 401, 403, 429) hold a fixed message and field codes only (acceptance tests assert no registration data).
- `.env` file check: lists `03_statistics/data/3e28dc22-d783-46a9-86e7-7e4ffaf4b7da.jsonl` and `03_statistics/results/transcript.md`, both added in `a82fcea` by the human; the value is `ORGANIZER_EMAILS` (2 lines each). **Critical F-07, D-29.** Commit messages: 0.

## 7. Traceability (step 9)

`node 01_input/00_general/skills/verify-release/trace-ac.mjs 04_changes/CR-001_organizer-search/acceptance-criteria.md` (`out/logs/cr-001_06_trace-ac.log`):

"30 acceptance criteria; without a test: none; without a commit: none." Per criterion: AC-009-01 E2E-06 and `us009-organizer-search.test.tsx`; AC-009-02, 04, 28..30 `us009-organizer-search.test.tsx`; AC-009-03, 05, 18..20, 22, 25 `OrganizerSearchAcceptanceTest.java`; AC-009-06, 07 both; AC-009-08..16 `OrganizerSearchTextAcceptanceTest.java`; AC-009-17, 21, 26, 27 `us009-organizer-filters.test.tsx`; AC-009-23, 24 backend and frontend. Each has the US-009 commits (fa8705e … f497161).

## 8. Findings (step 7, 11)

Ids continue after F-06 of the baseline report. F-01..F-04 of the baseline still apply as accepted there; F-01 now also covers the search, which SR-09 requires to use the export's credentials.

| ID | Severity | Source | Finding | Status |
|---|---|---|---|---|
| F-07 | Critical (step 8 rule) | `.env` file check | The `ORGANIZER_EMAILS` value is in `03_statistics/data/3e28dc22-…jsonl` and `03_statistics/results/transcript.md`, committed in `a82fcea` before the CR baseline. | **open**: D-29, waiting for the human |
| F-08 | Low | `lint-change.mjs --signed` | The lint rejects `cr/docs/`, which `change-phases.md` requires. | accepted under D-28 (pending review); template tooling defect |
| F-09 | Low | Stryker survivors in changed frontend code (20 of 281 changed-line mutants) | `OrganizerPage.tsx`: initial-state literals that the first login overwrites, `preventDefault` removals (jsdom does not navigate), `++`→`--` on the request counter (still unique), `failed`/`wrongCredentials` render guards whose states are always reset together; `api.ts`: the `Accept` header literal and a `status === 400` guard whose `true` mutant still falls through to `failed`. | accepted: gate met, stop rule; the behaviours are covered by AC-009-04, 17, 26, 27 and `OrganizerPage.test.tsx`. Backend changed-line mutants: 92/92 killed |

## 9. Definition of done (step 10)

| Item | Evidence |
|---|---|
| DoD-01 | 335 + 121 + 6 pass; manifests match (sections 1, 2) |
| DoD-02 | checks clean (section 2) |
| DoD-03 | gate met for both changed components (phase 5) |
| DoD-04 | `ArchitectureTest` 6/6: no new package dependency outside S2, no cycles |
| DoD-05 | **not met while F-07 is open** (D-29) |
| DoD-06 | runtime demonstration (section 3) |
| DoD-07 | traceability (section 7) |
| DoD-08 | phase 7 (READMEs from a clean checkout) |
| DoD-09 | phase 7 (`docs/release-notes.md`) |
| DoD-10 | decisions: section 10; inputs unchanged (section 1) |
| DoD-11 | phase 3 commits add no production code; the freeze commit `075d029` adds only the manifest, whose files were committed before; no commit exceeds 15 files or 400 lines (logs excluded) |
| DoD-P01..P04 | section 3 |
| DoD-P05 | unchanged code and test (`RecaptchaVerificationAcceptanceTest` passed in the final run); baseline evidence reused |

## 10. Decisions (step 12)

D-19..D-22, D-27 resolved; D-23..D-26 approved by the human in D-27; D-28 pending review; **D-29 open (blocking)**.

## 11. Verify → fix → re-verify loops

None yet. F-07 cannot be fixed by the agent (D-29).
