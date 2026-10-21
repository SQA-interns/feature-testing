# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria (`acceptance-criteria.md`) | Count | Rules cited | End-to-end |
|---|---|---|---|---|
| US-001 | AC-001-01 .. AC-001-08 | 8 | BR-01..05, SR-01, SR-04, OQ-02, OQ-04 | E2E-01 |
| US-002 | AC-002-01 .. AC-002-07 | 7 | BR-01..05, SR-01, OQ-01, OQ-02 | E2E-02 |
| US-003 | AC-003-01 .. AC-003-03 | 3 | BR-04, AR-04 | — |
| US-004 | AC-004-01 .. AC-004-04 | 4 | BR-06, NFR-03, SB-07 | E2E-01, E2E-02, E2E-03 |
| US-005 | AC-005-01 .. AC-005-04 | 4 | BR-03, BR-07, AR-05, NFR-01 | E2E-01, E2E-04 |
| US-006 | AC-006-01 .. AC-006-03 | 3 | SR-05, OQ-03 | E2E-01 |
| US-007 | AC-007-01 .. AC-007-02 | 2 | BR-07, SR-07 | E2E-01 |
| US-008 | AC-008-01 .. AC-008-04 | 4 | BR-08, SB-02, SR-06, NFR-01 | E2E-04, E2E-05 |
| **Total** | | **35** (matches the stated total) | | 5 scenarios |

## Gate checks

| Check | Result |
|---|---|
| Every story has at least one AC | Pass: US-001..US-008 all listed above. |
| Every AC cites a story | Pass: every AC sits under its story heading and carries the story number in its ID; the traceability table in `acceptance-criteria.md` agrees. |
| Every open question has an answer in `scope.md` | Pass: OQ-01..OQ-06 answered. |
| Every BR is reached by at least one AC | Pass: BR-01..BR-08. |
| Requirements without their own AC | SR-03 (rate and size limits), NFR-02 (persistence across recreation), NFR-04 (health and readiness): covered in the specification and verified in phase 6, as `acceptance-criteria.md` states. SR-02, SR-08, SB-xx: specification and phase 6. |

## Gaps and contradictions

| # | Finding | Handling |
|---|---|---|
| G-1 | `business-rules.md` ends with a stray block after the glossary entry "Raw JSON copy": the answers to OQ-06, OQ-05, OQ-04, OQ-03, OQ-02 and OQ-01 repeated without IDs. The text equals the answers in `scope.md`; no contradiction. | D-10 (non-blocking): `scope.md` is the source for the answers; the stray text adds nothing. |
| G-2 | AC-006-03 cites `OQ-03, OQ-03` (duplicate reference). Cosmetic. | No decision; read as OQ-03. |
| G-3 | Behaviour for a consent id that is not in the configuration is not stated (BR-05 covers only missing mandatory consents). | D-11 (non-blocking, pending review): reject with an error for `consents` (the more conservative behaviour). |
| G-4 | Maximum lengths of text fields are not stated (BR-02, BR-03 only require non-empty, Unicode and a valid email). | D-12 (non-blocking, pending review): limit each text field to 200 characters and the email to 254 characters, rejected with a field error; Slovenian characters count as one character. |
| G-5 | A missing or unknown registration `type` is not covered by an AC. | D-13 (non-blocking, pending review): reject with HTTP 400 and an error for `type`; no row, no copy, no email. |
