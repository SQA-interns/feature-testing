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
| Frozen tests (phase 3) | 101: backend acceptance 89, frontend acceptance 7, end-to-end 5; all failed for behavioural reasons on the bootstrap skeleton. Changed once under D-16 (backend harness and 9 acceptance classes, no assertion changed), re-frozen in `44fe78a` |
| First complete run (phase 5, before any fix) | 101 passed, 0 failed |
| Final run (phase 6) | 380 passed, 0 failed (backend 301, frontend 74, end-to-end 5) |
| Coverage, all tests | backend 99.44 % line / 96.39 % branch (JaCoCo); frontend 100 % line / 97.01 % branch (Vitest v8); end-to-end not instrumented (D-17) |
| Mutation score | backend raw 92.71 %, adjusted 92.71 %, PIT 1.30.0, 318 killed, 0 equivalent, 343 total, scope `si.konferenca.registration.*` minus the entry point; frontend raw 90.30 %, adjusted 90.30 %, Stryker 10.0.0, 428 killed, 0 equivalent, 474 total, scope `src/**/*.{ts,tsx}` minus `main.tsx`, `*.d.ts` and tests |
| Quality gate | met for both components (line >= 95 %, mutation >= 90 %); 0 survivor rounds; tests at the first mutation run and at the end: backend 301 / 301, frontend 74 / 74; no shortfall |
| Findings as found | Critical 0, High 1, Medium 2, Low 3 |
| Findings after triage | open Critical or High: 0; F-01 (Semgrep High on HTTP Basic) lowered to Low by the human, D-18 |
| Fix loops | 0 |
| Code (cloc) | backend 2084 production / 3598 test lines; frontend 659 production / 1641 test lines |
| Duplication | backend 0 % (PMD CPD); frontend 0 % (jscpd) |

## Test measures

<!-- One row per component and level, from `docs/03_test-strategy.md` (measurement-scope.md defines every figure). Tests: passed / failed from the runner. Coverage: `unit` row = unit tests only, `non-unit` row = integration, acceptance, architecture and end-to-end tests that run under the coverage tool, `all levels` row = one merged report. Mutation score on the `all levels` row, with scope, tool, killed of total and the tests run. Write "—" where a cell does not apply. Goal: high coverage and mutation score. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 89 / 0 | — | — | — | JUnit 6, Testcontainers |
| backend | integration | 5 / 0 | — | — | — | JUnit 6, MockMvc slice |
| backend | architecture | 6 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 201 / 0 | 93.47 % (with the integration and architecture tests) | 95.31 % | — | JUnit 6, JaCoCo 0.8.12 |
| backend | non-unit | 89 / 0 (acceptance) | 90.54 % | 71.48 % | — | JaCoCo 0.8.12 |
| backend | all levels | 301 / 0 | 99.44 % | 96.39 % | 92.71 % raw = adjusted, 318 of 343, `si.konferenca.registration.*` minus the entry point, full JUnit suite | JaCoCo 0.8.12, PIT 1.30.0 |
| frontend | acceptance | 7 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 67 / 0 | 100 % | 97.01 % | — | Vitest 4.1.11 v8 |
| frontend | non-unit | 7 / 0 (acceptance) | 76.28 % | 62.68 % | — | Vitest 4.1.11 v8 |
| frontend | all levels | 74 / 0 | 100 % | 97.01 % | 90.30 % raw = adjusted, 428 of 474, `src/**/*.{ts,tsx}` minus `main.tsx`, `*.d.ts`, tests, full Vitest suite | Vitest v8, Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 |

## Fix loops

<!-- one row per loop: finding, category, needs a human (yes/no), minutes, change. Goal: few, short, no human needed; note any loop that a template rule could have prevented. -->

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| — | none: no Critical or High finding was fixed; F-01 was lowered by the human (D-18) | — | — | — | — |

## Commits

<!-- auto:commits -->
<!-- /auto:commits -->

## Decisions and human interventions

<!-- counts of blocking and non-blocking decisions by category; one row per human intervention with cause and minutes. Goal: few blocking decisions; list every decision that a pre-approved answer in the inputs could have avoided. -->

18 decisions: 6 blocking (environment 3, dependency 1, frozen test 1, severity 1) and 12 non-blocking (requirement 4, security finding 3, environment 2, dependency 1, measurement 1, other 1). D-11..D-14 are pending review; all others are resolved.

| Intervention | Cause | Minutes |
|---|---|---|
| D-01..D-04 (phase 0): `.env` missing, installed Docker and JDK versions differ from the stack, Vite High advisory | decision | 12.3 |
| stop after phase 1 | session | 19.6 |
| go-ahead for phase 2 | session | 0.6 |
| D-16 (phase 4): frozen harness ignored per-class configuration | decision | 4.1 |
| stop after phase 4 | session | 182.6 |
| D-18 (phase 6): Semgrep High on HTTP Basic | decision | 9.7 |

Avoidable with a pre-approved answer in the inputs: D-02 and D-03 (installed Docker and JDK patch versions differing from `tech-stack.md`) and D-18 (a generic SAST rule against HTTP Basic when the inputs leave the mechanism to phase 2).

## Divergences

<!-- every departure from the rules, phases or stack, from `divergences[]`; "none" if none. Goal: none. -->

- Phase 3: commit `0929294` (backend acceptance harness) has 633 changed lines, over the 400-line limit.
- Phase 3: the measurement exclusions were written after the first acceptance tests; they equal the input defaults.
- Phase 5: commit `e5b5b50` (domain unit tests) has 569 changed lines, over the 400-line limit.

## What held the run back

<!-- at most three items, each one sentence with its evidence: the largest causes of agent time and of cost (phase, tool class, waits, repeated fixes). -->

- Waiting for the human: 229 minutes in total, of which 183 were the stop between phase 4 and phase 5 (`humanInterventions`).
- The frozen-harness defect D-16: Spring Framework 7 registers a base class's `@DynamicPropertySource` last, so two acceptance classes silently ran with the wrong configuration until phase 4, which needed a blocking decision and a re-freeze.
- Long tool runs in phases 5 and 6: the full PIT run (343 mutants over the Testcontainers-based suite, about 25 minutes), Stryker, and the online dependency scan.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
