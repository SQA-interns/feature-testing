# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
<!-- /auto:time -->

## Outcome

<!-- One row per measure, values from `run-log.json`; keep the rows and their order. Goal: final run all green, no open Critical or High; always say what a coverage or mutation figure covers. -->

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 150 frozen: 116 of the base run unchanged (backend acceptance 102, frontend acceptance 9, e2e 5; `03_acceptance-manifest.sha256`) + 34 new for US-009 (backend acceptance 30, frontend acceptance 3, e2e 1; `03_acceptance-manifest-us-009.sha256`). Of the new ones, 31 failed for behavioural reasons before the build; 3 (AC-009-09, 401 without credentials) passed on the base security configuration. No change to a frozen test. |
| First complete run (phase 5, before any fix) | 345 passed, 0 failed (backend 281, frontend 58, e2e 6) |
| Final run (phase 6) | 381 passed, 0 failed (backend 281, frontend 94, e2e 6) |
| Coverage, all tests | backend 99.20 % line / 98.11 % branch (JaCoCo 0.8.12); frontend 100 % line / 94.81 % branch (Vitest v8) |
| Mutation score | backend raw 96.99 %, adjusted 99.18 % (PIT 1.30.0; 354 killed, 8 equivalent, 365 total; scope `si.konferenca.registration.**` without the entry point); frontend raw = adjusted 91.19 % (Stryker 10.0.0; 600 killed, 0 equivalent, 658 valid; scope `src/**/*.{ts,tsx}` without tests, `*.d.ts`, `main.tsx`; phase 6 re-measure after F-10) |
| Quality gate | met for both components: backend line and mutation met on the first measurement (0 rounds, 281 tests at the first mutation run); frontend line met after 15 API client tests, mutation met after 1 survivor round (73 tests at the first mutation run, 94 at the end); no shortfall |
| Findings as found | Critical 0, High 2, Medium 5, Low 6 |
| Findings after triage | open Critical 0, High 0; F-01 lowered under PA-04 (D-28), F-07 (Vite) lowered to Medium by the human (D-22); 6 Medium accepted; F-10 fixed |
| Fix loops | 1: F-10 (duplicated 400 handling in `frontend/src/api.ts`), 8.6 minutes |
| Code (cloc) | backend 2 116 production / 3 771 test lines; frontend 1 011 / 2 167 (cloc image 2.10 reporting 1.98, D-07) |
| Duplication | backend 0 (PMD CPD); frontend 0.00 % after F-10 (jscpd 5.4.0) |

## Test measures

<!-- One row per component and level, from `docs/03_test-strategy.md` (measurement-scope.md defines every figure). Tests: passed / failed from the runner. Coverage: `unit` row = unit tests only, `non-unit` row = integration, acceptance, architecture and end-to-end tests that run under the coverage tool, `all levels` row = one merged report. Mutation score on the `all levels` row, with scope, tool, killed of total and the tests run. Write "—" where a cell does not apply. Goal: high coverage and mutation score. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 132 / 0 | — | — | — | JUnit 6, Testcontainers |
| backend | integration | — | — | — | — | (none separate from acceptance) |
| backend | architecture | 5 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 144 / 0 | 78.74 % | 86.16 % | — | JaCoCo 0.8.12 |
| backend | non-unit | 137 / 0 | 87.66 % | 70.44 % | — | JaCoCo 0.8.12 |
| backend | all levels | 281 / 0 | 99.20 % | 98.11 % | 99.18 % adjusted (96.99 % raw), 354 killed of 365, scope `si.konferenca.registration.**`, all 281 tests | PIT 1.30.0 |
| frontend | acceptance | 12 / 0 | — | — | — | Vitest 4.1.11 |
| frontend | unit | 82 / 0 | 99.59 % | 94.30 % | — | Vitest v8 |
| frontend | non-unit | 12 / 0 | 78.36 % | 66.32 % | — | Vitest v8 |
| frontend | all levels | 94 / 0 | 100 % | 94.81 % | 91.19 %, 600 killed of 658, scope `src/**/*.{ts,tsx}` without tests, all 94 tests | Stryker 10.0.0 |
| frontend + backend | end-to-end | 6 / 0 | — | — | — | Playwright 1.63.0 |

## Fix loops

<!-- one row per loop: finding, category, needs a human (yes/no), minutes, change. Goal: few, short, no human needed; note any loop that a template rule could have prevented. -->

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-10 (Low, jscpd clone) | defect | no | 8.6 | `rejection()` helper shared by both API functions in `frontend/src/api.ts`; re-verified with jscpd, check, 94 tests, e2e and one Stryker run |

## Commits

<!-- auto:commits -->
<!-- /auto:commits -->

## Decisions and human interventions

<!-- counts of blocking and non-blocking decisions by category; one row per human intervention with cause and minutes. Goal: few blocking decisions; list every decision that a pre-approved answer in the inputs could have avoided. -->

Decisions of this run: D-18 … D-29 (12). Blocking 4: environment 3 (D-19 JDK vendor, D-20 Docker Engine minor, D-21 Compose minor), security-finding 1 (D-22 Vite rated High by npm). Non-blocking 8: other 1 (D-18 change-run rules), environment 3 (D-23 wrapper mode, D-24 stale volumes, D-25 NVD paging), requirement 2 (D-26, D-27 search parameter details), security-finding 1 (D-28, PA-04), dependency 1 (D-29).

| Intervention | Cause | Minutes |
|---|---|---|
| phase 0 questions D-19 … D-22 (and D-24 information), answered in one message | decision | 40.6 |

Avoidable with pre-approved answers: D-19 … D-21 (host JDK vendor and Docker/Compose minor versions differ from the pins on this machine; PA-05 covers only patch differences of the same vendor) and D-22 (dev-only Vite advisory rated High; PA-03 covers only Stryker's transitive packages).

## Divergences

<!-- every departure from the rules, phases or stack, from `divergences[]`; "none" if none. Goal: none. -->

- Phase 3: the three US-009 test commits were written first and committed afterwards, about a minute apart, instead of committing each part before writing the next (`working-rules.md`, Commits).
- Phase 4: two base-run log files (`logs/phase4-check.log`, `logs/phase4-frontend-check.log`) were overwritten during phase 4 and restored from git before any commit; this run's logs use the `us009` prefix.

## What held the run back

<!-- at most three items, each one sentence with its evidence: the largest causes of agent time and of cost (phase, tool class, waits, repeated fixes). -->

- The full backend mutation run took 48 minutes (phase 5), twice the base run's 24, because mutants that hang until the timeout are expensive on this machine; it found no new survivor.
- The phase 0 NVD download stalled twice (more than 40 minutes in total) until smaller pages were requested (D-25), and the blocking version questions waited 41 minutes for the human.
- The organizer page needed a survivor round: its acceptance tests check structure only, so the first Stryker run scored 41 % on that file (81.44 % overall).

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
