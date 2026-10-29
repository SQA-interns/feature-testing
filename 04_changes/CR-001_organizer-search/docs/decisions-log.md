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

## D-23: Rate limit of the organizer search
- Timestamp: 2026-10-29T13:11:30Z
- Phase: 1
- Type: non-blocking
- Trigger: G-08. SR-03 (MODIFIED) rate limits the organizer search per client but gives no figure; the page calls the API after login, on Search or Enter and on every filter change (BR-13), and failed logins must stay limited (SB-06).
- Options: 1. (default) Its own endpoint group with 30 requests per client and minute, set by `RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE`; successful and failed calls count together, as for the export; 2. Share the export's group and limit (10 per minute), which a few filter changes exhaust; 3. 60 per minute, as the public form configuration.
- Human response: none
- Resolution: option 1, pending review

## D-24: Registration id on the organizer page
- Timestamp: 2026-10-29T13:11:30Z
- Phase: 1
- Type: non-blocking
- Trigger: G-09. BR-10 says each row shows "the data its export row holds" and lists type, fields, options, consents with time and registration time; the export row also holds the registration id. AC-009-07 says "and no other data".
- Options: 1. (default, more conservative) The API returns the id (registration data, also in the export) as the key of each row; the page does not show it; 2. Show the id on the page as well.
- Human response: none
- Resolution: option 1, pending review

## D-25: Search text used when a filter changes
- Timestamp: 2026-10-29T13:11:30Z
- Phase: 1
- Type: non-blocking
- Trigger: G-10. AC-009-26 updates the list when a filter changes without pressing Search; it does not say whether text typed into the search bar but not yet submitted applies.
- Options: 1. (default) A filter change searches with the text currently in the search bar, so the list always matches what the page shows; 2. A filter change reuses the text of the last Search or Enter.
- Human response: none
- Resolution: option 1, pending review

## D-26: No Basic challenge on the organizer search's 401
- Timestamp: 2026-10-29T13:11:30Z
- Phase: 1
- Type: non-blocking
- Trigger: G-11. AC-009-03 requires 401 for missing or wrong credentials and AC-009-04 an error message on the page. The export's 401 sends `WWW-Authenticate: Basic`, on which browsers open their own login dialog even for a page's `fetch` call, so the page could not show its message.
- Options: 1. (default) The organizer search answers 401 with the same `Error` body and no `WWW-Authenticate` header; the export keeps its challenge unchanged (AC-008-02 must not change); 2. Keep the challenge and accept the browser dialog in front of the page's own form.
- Human response: none
- Resolution: option 1, pending review

## D-27: Human approves D-23..D-26
- Timestamp: 2026-10-29T14:04:12Z
- Phase: 5
- Type: non-blocking
- Trigger: D-23 (organizer search rate limit 30 per minute), D-24 (id not shown on the page), D-25 (a filter change uses the current search text) and D-26 (no Basic challenge on the search's 401) were pending review.
- Options: 1. Keep the four resolutions as built; 2. Change one or more of them.
- Human response: "i agree with all of the decisions you presented" (2026-10-29T14:04:12Z)
- Resolution: option 1; D-23..D-26 approved as built
