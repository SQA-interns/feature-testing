# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria | Count | Rules covered |
|---|---|---|---|
| US-001 | AC-001-01 … AC-001-08 | 8 | BR-01, BR-02, BR-03, BR-04, BR-05, SR-01, SR-04, OQ-02, OQ-04 |
| US-002 | AC-002-01 … AC-002-07 | 7 | BR-01, BR-02, BR-03, BR-04, BR-05, SR-01, OQ-01, OQ-02 |
| US-003 | AC-003-01 … AC-003-03 | 3 | BR-04, AR-04 |
| US-004 | AC-004-01 … AC-004-04 | 4 | BR-06, NFR-03, SB-07 |
| US-005 | AC-005-01 … AC-005-04 | 4 | BR-03, BR-07, AR-05, NFR-01 |
| US-006 | AC-006-01 … AC-006-03 | 3 | SR-05, OQ-03 |
| US-007 | AC-007-01, AC-007-02 | 2 | BR-07, SR-07 |
| US-008 | AC-008-01 … AC-008-04 | 4 | BR-08, SB-02, SR-06, NFR-01 |
| US-009 | AC-009-01 … AC-009-13 | 13 | BR-03, BR-09, BR-10, SB-01, SB-02, SR-06, SR-09, NFR-01 |

Total 48 criteria, matching the count in `acceptance-criteria.md` (35 of the base run plus 13 for US-009, D-18). Every story has at least one criterion; every criterion sits under, and cites, exactly one story. E2E-01 … E2E-06 each cite only existing criteria; E2E-06 also cites DoD-P06.

## Open questions

OQ-01 … OQ-06 all have answers in `scope.md`. OQ-05 (repeat email allowed) and OQ-06 (retention) have no criterion of their own; the specification covers them (no uniqueness constraint on email; retention note in the release notes).

## Requirements without a criterion

Covered in the specification and verified in phase 6, as `acceptance-criteria.md` states: SR-03 (rate and size limits), NFR-02 (persistence across container recreation), NFR-04 (health and readiness). Also without a criterion and carried into the specification: SR-02 (test mode only by configuration), SR-08 (proxy sets the upstream `Host`), SB-10 (security headers), SB-11 (non-root containers), DoD-P05 (production reCAPTCHA path against a mocked endpoint). For US-009: SR-03 now names the search endpoint (rate limit), and DoD-P06 (search on the running stack, refused without organizer access) is a runtime demonstration in phase 6 besides E2E-06.

## Gaps and contradictions

| # | Observation | Handling |
|---|---|---|
| G-1 | No maximum length is given for any text field; SR-03 only limits the body size. | D-08 (non-blocking, pending review) |
| G-2 | "Valid email format" (BR-03) is not defined further. | D-09 (non-blocking, pending review) |
| G-3 | AC-001-05 lists unknown, inactive and duplicate options, but not an active option unavailable to `EXTERNAL`; OQ-01 states the backend rejects any option not available to the registration type. | No conflict: the OQ-01 answer applies to both types; the specification rejects it with an `options` error. |
| G-4 | `scope.md` begins with a stray line repeating the OQ answers before the title; AC-006-03 cites `OQ-03` twice. | Cosmetic; content agrees with the OQ table. No decision. |
| G-5 | AC-008-01 asks for consents in the export; AC-005-01 stores each consent with its time; the `api-contract.md` export columns carry consent ids only. | No conflict: the export follows the fixed contract (AR-09); consent times stay in the database and JSON copy. |
| G-6 | AC-009-08 rejects a search text "longer than 100 characters"; BR-10 ignores leading and trailing whitespace, so it is open whether the limit counts the raw or the trimmed text. | D-26 (non-blocking, pending review) |
| G-7 | `api-contract.md` lists the values of `type` (`EXTERNAL`, `STUDENT`) but not how a parameter that is present but blank (`type=`, `option=`, `page=`), a lower-case type, or a non-numeric page is handled. | D-27 (non-blocking, pending review) |
| G-8 | AC-009-08 rejects an option id that "is not a configured option"; inactive options are configured but no longer offered (BR-04), and stored registrations may hold them. | No conflict: read literally, every configured option id (active or inactive) is a valid filter; an unknown id is rejected. |
| G-9 | No criterion says what a page after the last one returns. | No conflict: BR-10 paging gives an empty `items` list with the real `total` (HTTP 200); only a page below 1 is rejected (AC-009-08). |
| G-10 | `GET /api/registrations` (search) and `POST /api/registrations` (registration) share one path; SR-03 limits "per client and endpoint". | Carried into phase 2: an endpoint is method plus path, so searching never uses up a participant's registration budget from the same address. |
