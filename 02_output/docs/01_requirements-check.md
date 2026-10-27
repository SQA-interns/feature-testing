# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria (`acceptance-criteria.md`) | Count | E2E scenarios | Rules referenced |
|---|---|---|---|---|
| US-001 | AC-001-01..08 | 8 | E2E-01 | BR-01..05, SR-01, SR-04, OQ-02, OQ-04 |
| US-002 | AC-002-01..07 | 7 | E2E-02 | BR-01..05, SR-01, OQ-01, OQ-02 |
| US-003 | AC-003-01..03 | 3 | none | BR-04, AR-04 |
| US-004 | AC-004-01..04 | 4 | E2E-01, E2E-02, E2E-03 | BR-06, NFR-03, SB-07 |
| US-005 | AC-005-01..04 | 4 | E2E-01, E2E-04 | BR-03, BR-07, AR-05, NFR-01 |
| US-006 | AC-006-01..03 | 3 | E2E-01 | SR-05, OQ-03 |
| US-007 | AC-007-01..02 | 2 | E2E-01 | BR-07, SR-07 |
| US-008 | AC-008-01..04 | 4 | E2E-04, E2E-05 | BR-08, SB-02, SR-06, NFR-01 |

Total 35 criteria, matching the input's own count. Every story has at least one criterion; every criterion sits under, and therefore cites, exactly one story. Every ID a criterion or scenario references (BR, SR, SB, AR, NFR, OQ, DoD-P) exists in the inputs.

Requirements with no criterion of their own, as the input states: SR-03, NFR-02, NFR-04 (specification and phase 6). Also without a criterion and carried to the specification: SR-02, SR-07 (export side), SR-08, DoD-P05.

## Open questions

OQ-01..OQ-06 all have an answer in `scope.md`; no decision is recorded for them (`scope.md`).

## Gaps and contradictions

| # | Finding | Handling |
|---|---|---|
| G-01 | OQ-01 says an option not available to the registration type is rejected for both types, but only the student case has a criterion (AC-002-04). | No conflict: the specification applies OQ-01 to both types; tests follow the criteria list. |
| G-02 | AC-006-03 cites `OQ-03` twice in its Refs. | Typo with no behavioural effect; no decision. |
| G-03 | AC-008-04 says the export over plain HTTP from a non-localhost client is "refused" but gives no status or whether to redirect. | D-11 (non-blocking, pending review). |
| G-04 | E2E-01 needs at least one active option per category and E2E-02 needs options not shown to students, but the inputs give no shipped options configuration (only the consent, OQ-02). | D-12 (non-blocking, pending review). |
| G-05 | The language of the form, confirmation and emails is not stated; the only given wording (OQ-02 consent) is English. | D-13 (non-blocking, pending review). |
| G-06 | No maximum length is given for any text field, yet SR-03 requires a body size limit and the database needs column sizes. | D-14 (non-blocking, pending review). |
| G-07 | The status for AC-005-03 ("an error, not 201") and the field key of the reCAPTCHA error (AC-001-07, AC-002-06) are not given. | Not visible to a user beyond AC-004-04's general message; fixed in the specification and contracts, no decision. |
