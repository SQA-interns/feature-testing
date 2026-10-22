# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
| Item | Value |
|---|---|
| Run id | tanej-single-agent-sdd-v001-run-2-search-opus-5-5-t1.7-ft-20261022T1806Z-c947 |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.7-ft |
| Run type, feature, base run | greenfield, search, – |
| Start commit | 6917289 |
| End commit | 2043b38 |
| Start, end | 2026-10-22T18:06:26Z, 2026-10-22T21:35:10Z |
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 preflight-and-bootstrap | 37 min | 24 min | 13 min |
| 1 requirements | 0 min | 0 min | 0 min |
| 2 design | 6 min | 0 min | 6 min |
| 3 test-design | 34 min | 0 min | 34 min |
| 4 build | 40 min | 0 min | 40 min |
| 5 unit-tests | 68 min | 0 min | 68 min |
| 6 verify | 15 min | 0 min | 15 min |
| 7 release | 9 min | 0 min | 9 min |
| **Total** | **3 h 29 min** | **24 min** | **3 h 05 min** |

Waits over one hour: none.
<!-- /auto:time -->

## Outcome

<!-- One row per measure, values from `run-log.json`; keep the rows and their order. Goal: final run all green, no open Critical or High; always say what a coverage or mutation figure covers. -->

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 129: backend acceptance 105, frontend acceptance 18, end-to-end 6; all failed on the skeleton for behavioural reasons; no frozen test changed |
| First complete run (phase 5, before any fix) | 129 passed, 0 failed |
| Final run (phase 6) | 403 passed, 0 failed (unit 268, architecture 6, acceptance 123, e2e 6) |
| Coverage, all tests | backend 98.09 % line / 99.24 % branch (JaCoCo, 288 JUnit tests); frontend 100 % line / 98.52 % branch (Vitest v8, 93 tests at measurement) |
| Mutation score | backend raw 93.15 %, adjusted 93.15 %, PIT 1.30.0, 340 killed, 0 equivalent, 365 total, scope `si.konferenca.registration.*` without the entry point; frontend raw 93.83 %, adjusted 93.83 %, Stryker 10.0.0, 502 killed, 0 equivalent, 535 total, scope `src/**` without `main.tsx` and test files |
| Quality gate | met for both: backend line and mutation met with 0 survivor rounds (288 tests at the first mutation run, 288 at the end); frontend line and mutation met after 1 survivor round (93 tests at the first mutation run, 109 at the end); no shortfall |
| Findings as found | Critical 0, High 0, Medium 4, Low 4 |
| Findings after triage | open Critical or High: 0; none lowered by the human (F-01 fixed, F-02..F-08 accepted with reasons, PA-01..PA-03 applied) |
| Fix loops | 1: F-01, about 2 minutes |
| Code (cloc) | backend 2061 / 3805 lines; frontend 782 / 2195 lines (production / test) |
| Duplication | backend 0 % (PMD CPD 3.26.0, 100 tokens); frontend 0 % (jscpd 5.4.0, production code) |

## Test measures

<!-- One row per component and level, from `docs/03_test-strategy.md` (measurement-scope.md defines every figure). Tests: passed / failed from the runner. Coverage: `unit` row = unit tests only, `non-unit` row = integration, acceptance, architecture and end-to-end tests that run under the coverage tool, `all levels` row = one merged report. Mutation score on the `all levels` row, with scope, tool, killed of total and the tests run. Write "—" where a cell does not apply. Goal: high coverage and mutation score. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 105 / 0 | — | — | — | JUnit 6, Testcontainers 2.0.5 |
| backend | integration | 0 / 0 (adapter tests against a temp directory, a mock HTTP server or mock mail sender are counted as unit) | — | — | — | — |
| backend | architecture | 6 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 177 / 0 | 75.72 % (unit and architecture tests) | 95.83 % | — | JUnit 6, Mockito, JaCoCo 0.8.12 |
| backend | non-unit | 105 / 0 | 86.96 % | 72.35 % | — | JaCoCo 0.8.12 |
| backend | all levels | 288 / 0 | 98.09 % | 99.24 % | 93.15 % adjusted (93.15 % raw), 340 of 365, `si.konferenca.registration.*` without the entry point, all 288 JUnit tests | PIT 1.30.0 |
| frontend | acceptance | 18 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 91 / 0 | 99.54 % (75 unit tests at measurement) | 96.32 % | — | Vitest 4.1.11, coverage-v8 |
| frontend | non-unit | 18 / 0 | 78.08 % | 70.58 % | — | coverage-v8 |
| frontend | all levels | 109 / 0 | 100 % (93 tests at measurement) | 98.52 % | 93.83 % adjusted (93.83 % raw), 502 of 535, `src/**` without `main.tsx`, tests, declarations, all Vitest tests (unit and acceptance; full run plus round 1) | Stryker 10.0.0 |
| frontend + backend | end-to-end | 6 / 0 | — | — | — | Playwright 1.63.0, Chromium, local Docker stack |

## Fix loops

<!-- one row per loop: finding, category, needs a human (yes/no), minutes, change. Goal: few, short, no human needed; note any loop that a template rule could have prevented. -->

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-01 Semgrep prototype-pollution-loop (Medium) in the contract validator | defect | no | 2 | `$ref` resolved through own properties only |

## Commits

<!-- auto:commits -->
| Measure | Value |
|---|---|
| Commits | 77 (chore 3, build 7, docs 12, test 28, feat 24, fix 2, refactor 1) |
| Changed lines per commit: median / 90th percentile / largest | 153 / 351 / 732 |
| Files per commit (median) | 2 |
| Over the size guide / mixing concerns / in a burst | 6.5 % / 5.2 % / 55.8 % |
| Subject follows the format and the 72-character limit | 94.8 % |
| Active minutes between commits (median / longest gap) | 0.6 / 33.7 |
| Cost per commit (median) / per 100 changed lines | $0.14 / $0.24 |
<!-- /auto:commits -->

## Decisions and human interventions

<!-- counts of blocking and non-blocking decisions by category; one row per human intervention with cause and minutes. Goal: few blocking decisions; list every decision that a pre-approved answer in the inputs could have avoided. -->

| Category | Blocking | Non-blocking |
|---|---|---|
| environment | 1 (D-01) | 2 (D-03, D-04) |
| dependency | 1 (D-02) | 3 (D-05, D-09, D-12) |
| security-finding | 0 | 3 (D-06, D-07, D-08) |
| requirement | 0 | 2 (D-10, D-11) |
| other | 0 | 3 (D-13, D-14, D-15) |

| Intervention | Cause | Minutes |
|---|---|---|
| answer to D-01 and D-02 (asked together) | decision | 24.2 |

Avoidable with pre-approved answers in the inputs: D-01 (Docker Compose 5.6 minor version and a non-Temurin JDK, both outside PA-05) and D-02 (vite 6.4.3 High advisory, fixed in 6.4.4).

## Divergences

<!-- every departure from the rules, phases or stack, from `divergences[]`; "none" if none. Goal: none. -->

| Phase | Rule | Divergence |
|---|---|---|
| 0 | commit size | backend build commit `33deaa9` (635 lines) and frontend build commit `a0fa3fa` (7247 lines), mostly the vendored Maven wrapper scripts and `package-lock.json` |
| 3 | commit size, commit before the next change | harness commits `a476e15` (563 lines) and `3ca5efb` (502 lines); the phase 3 tests were written together and committed story by story in a row |
| 5 | commit size | unit-test commit `86fd471` with 408 lines |

Approved stack changes, not divergences: vite 6.4.4 (D-02), host Compose and JDK (D-01, D-03); dev-only additions `@types/node` 24.13.6 (D-09) and the Temurin JDK build image (D-12).

## What held the run back

<!-- at most three items, each one sentence with its evidence: the largest causes of agent time and of cost (phase, tool class, waits, repeated fixes). -->

1. Mutation testing was the longest tool time: the full PIT run took 26 minutes (20:25–20:51 UTC, Testcontainers acceptance tests in the minions), then Stryker took 9 minutes for the full run and 7 more for survivor round 1 (`logs/05_*`).
2. The Testcontainers acceptance suite (about 4 minutes per full backend run) was rerun often in phases 3 to 6 for coverage splits, gate checks and the clean-clone check.
3. The blocking question on host versions and the vite advisory paused phase 0 for 24 minutes; both answers were standard approvals that could have been pre-approved.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
| Item | Value |
|---|---|
| Cost (calculated from tokens and the prices in usage.md) | $32.48 ($32.48 without the cache rebuilds caused by waits) |
| Model calls / tool calls | 243 / 349 |
| Model time / tool time / agent time | 65 min / 92 min / 3 h 05 min |
| Session wall time | 3 h 29 min |
| Share of the cost at over 150k context | 90.6 % |
| Context per call (average / peak) | 383k / 670k |
| Approval prompts that needed a human | 1 (of 349 permission decisions; 0 rejected) |
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
