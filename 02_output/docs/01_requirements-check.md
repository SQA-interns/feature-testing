# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria | Count | Rules and other refs cited by the criteria | End-to-end scenarios |
|---|---|---|---|---|
| US-001 | AC-001-01 … AC-001-08 | 8 | BR-01, BR-02, BR-03, BR-04, BR-05, SR-01, SR-04, OQ-02, OQ-04 | E2E-01 |
| US-002 | AC-002-01 … AC-002-07 | 7 | BR-01, BR-02, BR-03, BR-04, BR-05, SR-01, OQ-01, OQ-02 | E2E-02 |
| US-003 | AC-003-01 … AC-003-03 | 3 | BR-04, AR-04 | none (acceptance only) |
| US-004 | AC-004-01 … AC-004-04 | 4 | BR-06, NFR-03, SB-07 | E2E-01, E2E-02, E2E-03 |
| US-005 | AC-005-01 … AC-005-04 | 4 | BR-03, BR-07, AR-05, NFR-01 | E2E-01, E2E-04 |
| US-006 | AC-006-01 … AC-006-03 | 3 | SR-05, OQ-03 | E2E-01 |
| US-007 | AC-007-01, AC-007-02 | 2 | BR-07, SR-07 | E2E-01 |
| US-008 | AC-008-01 … AC-008-04 | 4 | BR-08, SB-02, SR-06, NFR-01 | E2E-04, E2E-05 |
| US-009 | AC-009-01 … AC-009-13 | 13 | BR-03, BR-09, BR-10, SB-01, SB-02, SR-06, SR-09, NFR-01 | E2E-06 |
| **Total** | | **48** | | 6 scenarios |

Every story has at least one criterion; every criterion sits under exactly one story's section and the traceability table in `acceptance-criteria.md` agrees with the sections (48 = 8+7+3+4+4+3+2+4+13). Every `E2E-nn` names only criteria that exist.

## Business rules → criteria

| Rule | Criteria |
|---|---|
| BR-01 | AC-001-01, -02, -08; AC-002-01, -02, -07 |
| BR-02 | AC-001-02, -03; AC-002-02 |
| BR-03 | AC-001-04; AC-002-03; AC-005-04; AC-009-11 |
| BR-04 | AC-001-01, -05, -08; AC-002-01, -04, -07; AC-003-01 |
| BR-05 | AC-001-06, -08; AC-002-05, -07 |
| BR-06 | AC-004-01, -02, -04 |
| BR-07 | AC-005-01 … -03; AC-007-02 |
| BR-08 | AC-008-01, -02 |
| BR-09 | AC-009-01, -09, -12 |
| BR-10 | AC-009-01 … -08, -12 |

## Open questions

All six (OQ-01 … OQ-06) have an answer in `scope.md`; no new question arose in this phase.

## Gaps and observations

None of these is a gap or contradiction that needs a decision record; each is either covered elsewhere or a detail the specification fixes.

| # | Observation | Handling |
|---|---|---|
| G-01 | SR-03 (rate and size limits), NFR-02 (persistence across container recreation) and NFR-04 (health and readiness) have no criterion of their own. | Stated in `acceptance-criteria.md`; mapped in `docs/02_specification.md`, verified by runtime demonstration in phase 6. |
| G-02 | AC-006-03 cites OQ-03 twice. | Typographical only; no effect on the behaviour. |
| G-03 | AC-008-04 and AC-009-10 say "refused" over plain HTTP without a status code; AC-009-08 does not say how a page beyond the last page answers. | Fixed by `project/02_design/api-contract.md` (AR-09) in phase 2; any behaviour it leaves open becomes a decision record there. |
| G-04 | AC-009-08 rejects an option id that "is not a configured option": an inactive option is still configured. | Spec: the option filter accepts any configured option id, active or inactive, so registrations made before an option was deactivated stay findable. |
| G-05 | BR-10 "newest first" has no tie-breaker for equal registration times. | Spec: order by registration time, then by id, both descending, so paging is stable. |
| G-06 | `scope.md` priority 3 names US-007 and US-008 as organizer features but not US-009. | US-009 is an organizer feature too and is built after the participant flow (US-001 … US-006). |
