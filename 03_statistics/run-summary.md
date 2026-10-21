# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
| Item | Value |
|---|---|
| Run id | tanej-single-agent-sdd-v001-run-2-opus-5-5-t1.7-ft-20261021T1103Z-01db |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.7-ft |
| Run type, feature, base run | normal, none, – |
| Start commit | 3572c92 |
| End commit | 8397c8b |
| Start, end | 2026-10-21T11:03:53Z, 2026-10-21T16:40:44Z |
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 preflight-and-bootstrap | 23 min | 4 min | 19 min |
| 1 requirements | 1 min | 0 min | 1 min |
| 2 design | 7 min | 0 min | 7 min |
| 3 test-design | 32 min | 0 min | 32 min |
| 4 build | 44 min | 0 min | 44 min |
| 5 unit-tests | 91 min | 37 min | 55 min |
| 6 verify | 2 h 13 min | 103 min | 29 min |
| 7 release | 7 min | 0 min | 7 min |
| **Total** | **5 h 37 min** | **2 h 24 min** | **3 h 13 min** |

Waits over one hour: 1 (see the report).
<!-- /auto:time -->

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 114: backend acceptance 96, frontend acceptance 13, end-to-end 5 (38 files in `03_acceptance-manifest.sha256`); 112 failed for behavioural reasons on the skeleton, 2 passed only on bootstrap behaviour (listed in `03_test-strategy.md`); no frozen-test change |
| First complete run (phase 5, before any fix) | 114 passed, 0 failed |
| Final run (phase 6) | 321 passed, 0 failed (unit 193, integration 7, architecture 7, acceptance 109, e2e 5) |
| Coverage, all tests | backend 99.5 % line / 98.7 % branch (JaCoCo); frontend 100 % / 100 % (V8; e2e not instrumented) |
| Mutation score | backend raw 95.8 %, adjusted 95.8 % (PIT 1.30.0; 297 killed, 0 equivalent, 310 total; `si.konferenca.registration.**` without the entry point; all 230 backend tests); frontend raw 94.9 %, adjusted 98.5 % (Stryker 10.0.0; 431 killed, 16 equivalent, 454 total; `src/**/*.{ts,tsx}` without `main.tsx`, `*.d.ts`, tests; all 86 Vitest tests, e2e excluded by D-15) |
| Quality gate | met for both components: line (>= 95 %) and adjusted mutation (>= 90 %); survivor rounds: backend 0, frontend 1; tests at the first mutation run: backend 230, frontend 60; at the end: backend 230, frontend 86; no shortfall |
| Findings as found | Critical 0, High 1, Medium 2, Low 5 |
| Findings after triage | open Critical 0, High 0; none lowered by the human (tool severities kept; F-04 Medium accepted in D-16, F-05 per PA-03) |
| Fix loops | 2: F-01 (about 7 min), F-02 (under 1 min) |
| Code (cloc) | backend production 1915 / test 3466; frontend production 645 / test 1727 |
| Duplication | backend 0 % (PMD CPD, 100 tokens); frontend 0 % (jscpd 5.4.0) |

## Test measures

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 96 / 0 | — | — | — | JUnit 6, Spring Boot test, Testcontainers (PostgreSQL, Mailpit) |
| backend | integration | 7 / 0 | — | — | — | JUnit 6, MockMvc |
| backend | architecture | 7 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 120 / 0 | 74.7 % | 92.4 % | — | JUnit 6, JaCoCo 0.8.12 |
| backend | non-unit | 110 / 0 | 90.9 % | 75.6 % | — | JaCoCo 0.8.12 |
| backend | all levels | 230 / 0 | 99.5 % | 98.7 % | 95.8 % raw = adjusted, 297 of 310, `si.konferenca.registration.**` without `RegistrationApplication`, all 230 tests | JaCoCo 0.8.12, PIT 1.30.0 |
| frontend | acceptance | 13 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 73 / 0 | 100 % | 100 % | — | Vitest 4.1.11, V8 |
| frontend | non-unit | 13 / 0 | 80.3 % | 69.3 % | — | Vitest 4.1.11, V8 |
| frontend | all levels | 86 / 0 | 100 % | 100 % | 94.9 % raw, 98.5 % adjusted, 431 of 454 (16 equivalent), `src/**/*.{ts,tsx}` without `main.tsx`, `*.d.ts`, tests; all 86 Vitest tests | V8, Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 against `docker compose up` |

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-01 (High, Semgrep `spawn-shell-true`) | security-config | no | 7 | the e2e runner starts the Playwright CLI with `process.execPath` instead of a shell |
| 2 | F-02 (Low, Gitleaks) | process | no | < 1 | redacted Spring Boot's generated dev passwords from committed test logs; a template rule could prevent it: filter `Using generated security password` from saved logs |

## Commits

<!-- auto:commits -->
| Measure | Value |
|---|---|
| Commits | 98 (chore 9, build 13, docs 20, test 30, feat 20, refactor 3, fix 3) |
| Changed lines per commit: median / 90th percentile / largest | 78 / 286 / 634 |
| Files per commit (median) | 1 |
| Over the size guide / mixing concerns / in a burst | 1 % / 4.1 % / 44.9 % |
| Subject follows the format and the 72-character limit | 99 % |
| Active minutes between commits (median / longest gap) | 1.2 / 25.3 |
| Cost per commit (median) / per 100 changed lines | $0.3 / $0.41 |
<!-- /auto:commits -->

## Decisions and human interventions

Decisions: 16, of which 3 blocking (D-07 dependency, D-08 environment, D-09 dependency: the human's answer to both) and 13 non-blocking (environment 2, dependency 3, security-finding 4, requirement 4, other 1). Pending review: D-11, D-12, D-13.

| Intervention | Cause | Minutes | Note |
|---|---|---|---|
| 1 (phase 0) | decision | 3.9 | D-07 vite 6.4.3 High advisory and D-08 Docker Compose 5.6.0 |
| 2 (phase 6) | session | 103 | session usage limit; recorded by hand at the operator's request |

Avoidable with a pre-approved answer in the inputs: D-07 (pin vite 6.4.4 in `tech-stack.md`) and D-08 (approve the installed Compose minor version, or extend PA-05 to Compose).

## Divergences

- Phase 0: commit `d5f8c2c` has a 73-character subject (limit 72).
- Phase 0: vite 6.4.4 replaces 6.4.3 (D-07, D-09, approved by the human).
- Phase 0: Docker Compose 5.6.0 used instead of 5.5.1 (D-08, D-09, approved by the human).
- Phase 2: the specification and its contracts are two commits instead of one, to keep the 400-line limit of `working-rules.md` (precedence over `phases.md`).
- Phase 3: commit `534b7f2` (backend acceptance harness) has 634 changed lines (limit 400).
- Phase 6: the session-limit wait was added to `humanInterventions` by hand, because `stat.mjs` cannot record a start in the past.

## What held the run back

- The phase 6 session limit stopped the run for 103 minutes (recorded wait, excluded from agent time).
- Mutation testing is the longest tool time: the full Stryker run took 23 minutes and its survivor round about as long, the full PIT run with Testcontainers 22 minutes (`out/logs/05_*`).
- Two Spring Boot 4 property renames (`spring.servlet.encoding`, `spring.web.error`) passed unnoticed until a phase 5 integration test caught the missing charset; the fix was small but cost a debugging step.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
| Item | Value |
|---|---|
| Cost (calculated from tokens and the prices in usage.md) | $48.83 ($43.74 without the cache rebuilds caused by waits) |
| Model calls / tool calls | 366 / 445 |
| Model time / tool time / agent time | 82 min / 105 min / 3 h 13 min |
| Session wall time | 5 h 37 min |
| Share of the cost at over 150k context | 93.5 % |
| Context per call (average / peak) | 393k / 718k |
| Approval prompts that needed a human | 1 (of 445 permission decisions; 0 rejected) |
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
