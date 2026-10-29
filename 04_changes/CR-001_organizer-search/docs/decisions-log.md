# Decisions log: CR-001

> Written in: every phase of the change run · Source: `general/working-rules.md` "Decision record" · Agent: writes

Ids continue after D-18 of `02_output/docs/decisions-log.md`.

## D-19: SR-03 modified by CR-001
- Timestamp: 2026-10-29T13:09:54Z
- Phase: 0
- Type: non-blocking
- Trigger: `cr/change.md` "Baseline items changed": SR-03 MODIFIED, the rate-limited endpoints gain the organizer search.
- Options: 1. (default) Apply the CR's SR-03 wording for this run.
- Human response: approved by the signed CR (2026-10-28T21:47:37Z)
- Resolution: approved by the signed CR

## D-20: SR-07 modified by CR-001
- Timestamp: 2026-10-29T13:09:54Z
- Phase: 0
- Type: non-blocking
- Trigger: `cr/change.md` "Baseline items changed": SR-07 MODIFIED, the organizer search is a new place where registration data is exposed.
- Options: 1. (default) Apply the CR's SR-07 wording for this run.
- Human response: approved by the signed CR (2026-10-28T21:47:37Z)
- Resolution: approved by the signed CR

## D-21: NFR-01 modified by CR-001
- Timestamp: 2026-10-29T13:09:54Z
- Phase: 0
- Type: non-blocking
- Trigger: `cr/change.md` "Baseline items changed": NFR-01 MODIFIED, the organizer search page is a new place where text must appear unchanged.
- Options: 1. (default) Apply the CR's NFR-01 wording for this run.
- Human response: approved by the signed CR (2026-10-28T21:47:37Z)
- Resolution: approved by the signed CR

## D-22: Baseline input manifest differs in three statistics tools
- Timestamp: 2026-10-29T13:09:54Z
- Phase: 0
- Type: blocking
- Trigger: Baseline check (`general/skills/change-interview` step 2, repeated by `change-phases.md` phase 0): `02_output/docs/00_input-manifest.sha256` no longer matches `03_statistics/tools/build-report.mjs`, `metrics-catalog.json` and `stat.mjs` (besides `run-config.md`, which the check ignores). All three were changed only by the human's commit `a82fcea` ("update run-config -human", 2026-10-28T21:16), before the CR baseline `4d664d2`; no production code, test or project input differs.
- Options: 1. (proposed) Accept the three files as changed by the human and record the current hashes in this run's `docs/00_input-manifest.sha256`; 2. Stop the change run until the human restores the baseline versions.
- Human response: the requester accepted these changes in the change interview, baseline check row 0 of `interview-log.md` ("changed by the requester in a82fcea and accepted by them", 2026-10-28T20:24Z)
- Resolution: option 1
