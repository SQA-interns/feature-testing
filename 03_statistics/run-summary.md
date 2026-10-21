# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
| Item | Value |
|---|---|
| Run id | tanej-single-agent-sdd-v001-run-1-opus-5-5-t1.7-ft-20261020T1823Z-8493 |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.7-ft |
| Run type, feature, base run | normal, none, – |
| Start commit | a728ac1 |
| End commit | a10b9fb |
| Start, end | 2026-10-20T18:23:44Z, 2026-10-21T06:46:26Z |
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 preflight-and-bootstrap | 38 min | 21 min | 17 min |
| 1 requirements | 1 min | 0 min | 1 min |
| 2 design | 5 min | 0 min | 5 min |
| 3 test-design | 34 min | 0 min | 34 min |
| 4 build | 44 min | 0 min | 44 min |
| 5 unit-tests | 63 min | 14 min | 49 min |
| 6 verify | 9 h 08 min | 9 h 01 min | 7 min |
| 7 release | 10 min | 0 min | 10 min |
| **Total** | **12 h 23 min** | **9 h 36 min** | **2 h 47 min** |

Waits over one hour: 1 (see the report).
<!-- /auto:time -->

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 116: backend acceptance 102, frontend acceptance 9, end-to-end 5 (27 files in the manifest); all 116 failed for behavioural reasons on the skeleton (backend context without the spec settings, page without the form, backend not serving); no frozen-test change |
| First complete run (phase 5, before any fix) | 116 passed, 0 failed |
| Final run (phase 6) | 311 passed, 0 failed (backend 251, frontend 55, end-to-end 5) |
| Coverage, all tests | backend line 99.11 % / branch 97.90 % (JaCoCo); frontend line 100 % / branch 94.03 % (Vitest v8) |
| Mutation score | backend raw 96.70 %, adjusted 99.10 % (PIT 1.30.0; 322 killed, 8 equivalent, 333 total; scope `si.konferenca.registration.**` without the entry point); frontend raw = adjusted 90.85 % (Stryker 10.0.0; 407 killed, 0 equivalent, 448 viable, 4 non-viable; scope `src/**` without tests, harness, `d.ts`, `main.tsx`) |
| Quality gate | met: backend line met, mutation met after 1 survivor round (full run 88.25 %); frontend line and mutation met at the first run (0 rounds); tests at first mutation run 237 / 55, at the end 251 / 55; no shortfall |
| Findings as found | Critical 0, High 1, Medium 5, Low 3 |
| Findings after triage | open Critical 0, High 0; F-01 (High) lowered to Low under pre-approval PA-04 (D-17), not by the human; 5 Medium accepted with reasons (F-07 pending review, D-16) |
| Fix loops | 0 |
| Code (cloc) | backend 1 920 production / 3 445 test lines; frontend 650 / 1 698 (cloc image 2.10 reporting 1.98, D-07) |
| Duplication | backend 0 (PMD CPD, 100 tokens); frontend 0.00 % (jscpd 5.4.0) |

## Test measures

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 102 / 0 | — | — | — | JUnit 6, Spring Boot test, Testcontainers |
| backend | integration | 0 / 0 | — | — | — | — |
| backend | architecture | 5 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 144 / 0 | 94.64 % | 96.50 % | — | JUnit 6, JaCoCo 0.8.12 |
| backend | non-unit | 107 / 0 | 99.11 % | 97.90 % | — | JaCoCo 0.8.12 |
| backend | all levels | 251 / 0 | 99.11 % | 97.90 % | raw 96.70 %, adjusted 99.10 %, 322 + 8 equivalent of 333, `si.konferenca.registration.**`, all 251 JUnit tests | PIT 1.30.0 |
| frontend | acceptance | 9 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 46 / 0 | 100 % | 93.28 % | — | Vitest 4.1.11 v8 |
| frontend | non-unit | 9 / 0 | 75.00 % | 64.92 % | — | Vitest 4.1.11 v8 |
| frontend | all levels | 55 / 0 | 100 % | 94.03 % | 90.85 %, 407 of 448, `src/**` production, all 55 Vitest tests | Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 |

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| — | none | — | — | — | no Critical or High finding needed a fix |

## Commits

<!-- auto:commits -->
| Measure | Value |
|---|---|
| Commits | 66 (chore 3, build 7, docs 12, test 25, feat 17, fix 1, refactor 1) |
| Changed lines per commit: median / 90th percentile / largest | 161.5 / 323 / 612 |
| Files per commit (median) | 2 |
| Over the size guide / mixing concerns / in a burst | 4.5 % / 9.1 % / 45.5 % |
| Subject follows the format and the 72-character limit | 80.3 % |
| Active minutes between commits (median / longest gap) | 1.2 / 30.6 |
| Cost per commit (median) / per 100 changed lines | $0.21 / $0.29 |
<!-- /auto:commits -->

## Decisions and human interventions

Decisions: 17 (blocking 2, non-blocking 15). By category: environment 4 (blocking 2: D-02 JDK vendor, D-03 Compose minor version), dependency 4, security finding 3, requirement 4, other 2. Six non-blocking decisions applied pre-approved answers (PA-01 … PA-05). D-02 and D-03 could have been avoided by a pre-approved answer for a different JDK vendor and a Compose minor version, or by pinning the versions installed on the machine.

| Intervention | Cause | Start | End | Minutes |
|---|---|---|---|---|
| D-02, D-03 answered by the human | decision | 2026-10-20T18:40:24Z | 2026-10-20T19:01:08Z | 20.7 |
| session usage limit reached, run resumed by the human (recorded by hand) | session | 2026-10-20T21:34:00Z | 2026-10-21T06:34:51Z | 541 |

## Divergences

1. Phase 0: host JDK Oracle 21.0.11+9 instead of Temurin 21.0.10+7 (approved, D-02).
2. Phase 0: Docker Compose 5.6.0 instead of 5.5.1 (approved, D-03).
3. Phase 2: commit `81b5b37` (specification with all contracts) has about 600 changed lines, over the 400-line limit.
4. Phase 3: all tests were written and checked together before the first test commit; the 14 harness and story commits followed in a burst.
5. Phase 4: commit `06893ee` was made while SpotBugs reported EI_EXPOSE_REP (the check helper ignored the exit code); fixed in the next commit.
6. Phase 5: commit `1051016` (outbound adapter unit tests) has 576 changed lines, over the 400-line limit.
7. Phase 6: a first runtime-demo attempt printed one `ORGANIZER_EMAILS` address into the session's tool output (no file, log or commit); the log was deleted and the demo rerun with recipients redacted.

## What held the run back

1. Mutation testing: the backend PIT full run (24 minutes, the full suite with Spring and Testcontainers acceptance tests in every minion) plus a survivor round, and the frontend Stryker run (16.5 minutes) were the longest single steps of the agent's work.
2. Phase 3 harness design: the frozen tests could only be checked against the real implementation in phase 4, so their helpers (Mailpit API, xlsx reader, timestamps) were self-checked separately against real tools before the freeze.
3. Environment details on Windows: shell quoting of heredocs, the Windows command-line encoding of non-ASCII request bodies, MSYS path conversion and the 260-character path limit cost several reruns of scripts.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
| Item | Value |
|---|---|
| Cost (calculated from tokens and the prices in usage.md) | $33.72 ($29.32 without the cache rebuilds caused by waits) |
| Model calls / tool calls | 236 / 315 |
| Model time / tool time / agent time | 68 min / 101 min / 2 h 47 min |
| Session wall time | 12 h 23 min |
| Share of the cost at over 150k context | 91 % |
| Context per call (average / peak) | 345k / 627k |
| Approval prompts that needed a human | 0 (of 315 permission decisions; 0 rejected) |
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
