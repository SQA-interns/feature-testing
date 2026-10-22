# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes

Story → criteria coverage and the gaps found in the inputs. Nothing is copied from the inputs; IDs refer to them.

## Story → criteria

| Story | Criteria (in `acceptance-criteria.md`) | Count | E2E scenario | Result |
|---|---|---|---|---|
| US-001 | AC-001-01..08 | 8 | E2E-01 | ≥ 1 AC |
| US-002 | AC-002-01..07 | 7 | E2E-02 | ≥ 1 AC |
| US-003 | AC-003-01..03 | 3 | – (API-level only) | ≥ 1 AC |
| US-004 | AC-004-01..04 | 4 | E2E-01, E2E-02, E2E-03 | ≥ 1 AC |
| US-005 | AC-005-01..04 | 4 | E2E-01, E2E-04 | ≥ 1 AC |
| US-006 | AC-006-01..03 | 3 | E2E-01 | ≥ 1 AC |
| US-007 | AC-007-01..02 | 2 | E2E-01 | ≥ 1 AC |
| US-008 | AC-008-01..04 | 4 | E2E-04, E2E-05 | ≥ 1 AC |
| US-009 | AC-009-01..13 | 13 | E2E-06 | ≥ 1 AC |

Total 48 criteria, matching the stated total. Every criterion sits under exactly one story section and the traceability table maps it back, so every AC cites a story. Every rule BR-01..BR-10 is cited by at least one AC. Every e2e scenario cites only existing criteria and DoD-P items.

## Open questions

OQ-01..OQ-06 all have an answer in `scope.md`; they are product owner decisions and get no decision record.

## Requirements without their own criterion

SR-03 (rate and size limits), NFR-02 (persistence across container recreation) and NFR-04 (health and readiness) are covered in the specification and verified by runtime demonstration in phase 6, as the acceptance criteria file states. SR-02, SR-05..SR-09, SB-xx, AR-xx are mapped in phase 2.

## Gaps and contradictions

| # | Finding | Effect | Handling |
|---|---|---|---|
| G-1 | `scope.md` starts with a stray line before its title that repeats the answers of OQ-06..OQ-01 in reverse order. | Formatting only; the text matches the Open questions table. | Ignored; the table is authoritative. No decision needed. |
| G-2 | AC-006-03 cites `OQ-03` twice. | Typo, no behavioural effect. | None. |
| G-3 | `scope.md` priority 3 names US-007 and US-008 as organizer features but not US-009. | US-009 is an organizer feature too; ordering only. | Treated as an organizer feature (after the participant flow). No decision needed. |
| G-4 | AC-008-04 and AC-009-10 say the plain-HTTP organizer request is "refused" without a status code; `api-contract.md` fixes 401 only for missing or wrong credentials. | Two behaviours a client would notice (401 or 403). | D-10 (non-blocking, conservative default). |
| G-5 | AC-009-08 limits the search text to 100 characters, BR-10 says leading and trailing whitespace is ignored; whether the limit applies before or after trimming is not stated. | Visible only for a text over 100 characters that is at most 100 after trimming. | D-11 (non-blocking, conservative default). |
| G-6 | AC-009-08 rejects an option id that "is not a configured option"; an inactive option is still configured. | An inactive option id is a valid search filter (registrations may hold options deactivated later). | Follow the criterion literally: active and inactive configured ids are accepted, unknown ids are rejected. No decision needed. |
