# Requirements check: CR-001

> Written in: phase 1 of the change run · Source: `cr/change.md`, `cr/acceptance-criteria.md`, `project/01_requirements/*`, `project/02_design/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria | Count | E2E scenarios | Rules referenced |
|---|---|---|---|---|
| US-009 | AC-009-01..30 (all ADDED) | 30 | E2E-06 (ADDED) | BR-08..13, SR-06, SR-07, SR-09, OQ-07, NFR-01, NFR-03 |

Every criterion sits under US-009 and cites at least one rule that exists in the CR or the baseline. E2E-06 covers only ids of US-009 plus NFR-01. No criterion or scenario is MODIFIED or REMOVED, so no baseline test changes. Without a criterion of their own, carried to the specification: SR-03 (MODIFIED: the organizer search joins the rate-limited endpoints) and SR-07 (MODIFIED: on the API side, AC-009-07 covers the page).

## Open questions

OQ-07..OQ-09 are answered in `cr/change.md`; no decision is recorded for them.

## Baseline items the CR touches

| Item | Touched by | Finding |
|---|---|---|
| `scope.md` "Out of scope: an administration UI or dashboard" | BR-09, US-009 | Lifted for this read-only page by the CR's Intent; not an id, so not in "Baseline items changed". Declared, no conflict. |
| `security-requirements.md` "One organizer role may export registrations" | SR-09 | The same role and credentials now also search; SR-09 states it. Additive, no conflict. |
| BR-08, SR-06 (Must not change) | AC-009-03, AC-009-05 | The search applies both unchanged (401 without credentials; plain HTTP refused off localhost). Consistent. |
| SR-03, SR-07, NFR-01 | the CR | Declared MODIFIED (D-19..D-21). |
| AC-001-*, AC-002-* (registration page) | AC-009-02 | Requires only that the page has no link to `/organizer`; the baseline page has none. Consistent. |
| AC-008-* (export) | Intent | The export stays the full list; no change. |

No undeclared conflict, so no blocking decision.

## Gaps

| # | Finding | Handling |
|---|---|---|
| G-08 | SR-03 names the organizer search as rate limited but gives no limit; the page calls it on every filter change. | Specification S7.3; D-23 (non-blocking, pending review). |
| G-09 | BR-10 says a row shows "the data its export row holds", then lists them without the registration id that the export has; AC-009-07 says "no other data". | D-24 (non-blocking, pending review): the API returns the id as the row key, the page does not show it. |
| G-10 | AC-009-26 updates the result when a filter changes, but does not say which search text applies when text was typed and not yet submitted. | D-25 (non-blocking, pending review). |
| G-11 | AC-009-03/04 need a 401 for wrong credentials and an error message on the page. The export's 401 carries `WWW-Authenticate: Basic`, which makes browsers open their own login dialog for a `fetch` call instead of letting the page show its message. | D-26 (non-blocking, pending review). |
| G-12 | BR-11 gives d/đ as an accent pair; letters with a stroke or other mark that Unicode does not decompose (ł, ø, ħ) are not named. | Specification S4.9 folds them like đ, by BR-11's "with or without any accent"; not visible in any criterion, no decision. |
| G-13 | The 100-character limit (BR-11) does not say how characters are counted. | Unicode code points after `strip()`, as the baseline's field lengths (S4.2); no decision. |
