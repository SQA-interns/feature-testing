# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
| Item | Value |
|---|---|
| Run id | tanej-single-agent-sdd-v001-run-1-search-opus-5-5-t1.7-ft-20261022T0811Z-f98d |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.7-ft |
| Run type, feature, base run | greenfield, search, – |
| Start commit | 0d62004 |
| End commit | bdef67e |
| Start, end | 2026-10-22T08:11:19Z, 2026-10-22T16:45:37Z |
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 preflight-and-bootstrap | 51 min | 29 min | 22 min |
| 1 requirements | 2 min | 0 min | 2 min |
| 2 design | 6 min | 0 min | 6 min |
| 3 test-design | 24 min | 0 min | 24 min |
| 4 build | 2 h 27 min | 102 min | 45 min |
| 5 unit-tests | 4 h 19 min | 3 h 30 min | 49 min |
| 6 verify | 17 min | 0 min | 17 min |
| 7 release | 9 min | 0 min | 9 min |
| **Total** | **8 h 34 min** | **5 h 41 min** | **2 h 53 min** |

Waits over one hour: 1 (see the report).
<!-- /auto:time -->

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 116: backend 98 acceptance; frontend 12 acceptance; 6 end-to-end. All failed for behavioural reasons before the build (bootstrap security answered 401, no schema, no UI); no frozen test was changed |
| First complete run (phase 5, before any fix) | 116 passed, 0 failed |
| Final run (phase 6) | 295 passed, 0 failed (backend 227: 123 unit, 6 architecture, 98 acceptance; frontend 62: 50 unit, 12 acceptance; 6 end-to-end) |
| Coverage, all tests | backend 98.32 % line / 95.91 % branch (JaCoCo); frontend 100 % line / 95.20 % branch (V8) |
| Mutation score | backend 91.20 % raw = adjusted, PIT 1.30.0, 373 killed, 0 equivalent, 409 total, scope `si.konferenca.registration.**` without the entry point, 227 tests; frontend 93.81 % raw = adjusted, Stryker 10.0.0, 515 killed, 0 equivalent, 549 total, scope `src/**/*.{ts,tsx}` without `main.tsx`, 62 tests (e2e not part of mutation runs, D-14) |
| Quality gate | met for both components (line ≥ 95 %, mutation ≥ 90 %); backend 0 survivor rounds (227 tests at first and last mutation run); frontend 1 round (37 tests at the first run, 62 at the end; 80.69 % → 93.81 %); no shortfall |
| Findings as found | Critical 0, High 0, Medium 3, Low 7 (phase 6); in phase 0 one High npm advisory (vite) removed by the approved patch upgrade (D-05) |
| Findings after triage | open Critical or High: 0; no finding lowered by the human; Medium findings accepted with reasons (F-01, F-07 D-06, F-08 PA-03) |
| Fix loops | 0 in phase 6 (no Critical or High) |
| Code (cloc) | backend 2290 production / 3574 test lines; frontend 723 / 1470 |
| Duplication | backend 0 % (PMD CPD); frontend 0 % (jscpd 5.4.0) |

## Test measures

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 98 / 0 | — | — | — | JUnit 6, Testcontainers (PostgreSQL, Mailpit) |
| backend | integration | — | — | — | — | (no separate integration level) |
| backend | architecture | 6 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 123 / 0 | 76.54 % | 89.62 % | — | JUnit 6, Mockito, JaCoCo (unit run includes the 6 architecture tests) |
| backend | non-unit | 104 / 0 | 89.63 % | 68.55 % | — | JaCoCo |
| backend | all levels | 227 / 0 | 98.32 % | 95.91 % | 91.20 %, 373 of 409, `si.konferenca.registration.**`, 227 tests | PIT 1.30.0 |
| frontend | acceptance | 12 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 50 / 0 | 99.06 % | 94.52 % | — | Vitest, V8 |
| frontend | non-unit | 12 / 0 | 80.00 % | 65.06 % | — | Vitest, V8 |
| frontend | all levels | 62 / 0 | 100 % | 95.20 % | 93.81 %, 515 of 549, `src/**/*.{ts,tsx}`, 62 tests | Stryker 10.0.0 |
| frontend + backend | end-to-end | 6 / 0 | — | — | — | Playwright 1.63.0 (Chromium) |

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| — | none in phase 6 | | | | |

During the build, E2E-01 found one defect (JSON attachment not byte-identical to the copy on the Linux container: OS line separator in pretty-printed JSON, text transfer encoding), fixed in `ea12aac` within phase 4.

## Commits

<!-- auto:commits -->
| Measure | Value |
|---|---|
| Commits | 88 (chore 9, build 12, docs 22, test 25, feat 17, fix 2, refactor 1) |
| Changed lines per commit: median / 90th percentile / largest | 120 / 332 / 849 |
| Files per commit (median) | 2 |
| Over the size guide / mixing concerns / in a burst | 3.4 % / 5.7 % / 45.5 % |
| Subject follows the format and the 72-character limit | 100 % |
| Active minutes between commits (median / longest gap) | 1.1 / 16.4 |
| Cost per commit (median) / per 100 changed lines | $0.28 / $0.32 |
<!-- /auto:commits -->

## Decisions and human interventions

16 decision records: 3 blocking (environment 2: host JDK D-01, Compose D-03; dependency 1: vite High advisory D-05), 13 non-blocking (environment 2, dependency 3, security finding 2, requirement 1, other 5). D-11 (403 for HTTPS-only refusal) is pending review; all others are resolved.

| Intervention | Cause | Minutes |
|---|---|---|
| phase 0 questions D-01, D-03, D-05, D-06 | decision | 4.3 |

Answerable in advance: D-01 and D-03 (host JDK vendor and Compose minor version) would be covered if PA-05 named the vendor and Compose; D-05 if the pinned vite were 6.4.4.

## Divergences

- Phase 0: vite 6.4.4 instead of 6.4.3 (D-05), host JDK Oracle 21.0.11 instead of Temurin 21.0.10+7 (D-01), Compose 5.6.0 instead of 5.5.1 (D-03), all approved by the human.
- Phase 3: the two test-harness commits `6dfdae7` (849 lines) and `07a91f8` (468 lines) exceed the 400-line limit.
- Phase 4: commit `de69bfc` did not compile; fixed by the next commit `144ae13`.
- Phase 5: one extra full frontend mutation run (D-15, corrected by D-16); the first full run remains the measurement.

## What held the run back

- Phase 5 mutation testing was the longest work: the full PIT run (26 min) and Stryker runs (about 25 min each, one of them unnecessary because of a misread survivor, D-15/D-16) dominate tool time.
- The host went to sleep during long silent tool runs (Windows power log: 12:11–12:36, 12:55–13:41, 14:33–16:56 local time), stretching wall time of phases 4 and 5 without agent work.
- Platform pins that did not match the host (JDK vendor, Compose minor) and a High advisory on the pinned vite needed a human answer in phase 0.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
| Item | Value |
|---|---|
| Cost (calculated from tokens and the prices in usage.md) | $41.53 ($37.63 without the cache rebuilds caused by waits) |
| Model calls / tool calls | 284 / 453 |
| Model time / tool time / agent time | 76 min / 99 min / 2 h 53 min |
| Session wall time | 8 h 34 min |
| Share of the cost at over 150k context | 92.1 % |
| Context per call (average / peak) | 395k / 732k |
| Approval prompts that needed a human | 1 (of 453 permission decisions; 1 rejected) |
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
