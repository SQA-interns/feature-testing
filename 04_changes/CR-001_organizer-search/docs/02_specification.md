# Specification index: CR-001

> Written in: phase 2 of the change run · Source: `cr/change.md`, `cr/acceptance-criteria.md` · Agent: writes

Per CR item, the section of the living specification `02_output/docs/02_specification.md` (S) or contract in `02_output/docs/02_contracts/` that realises it. Contracts validated with `02_output/tools/validate-contracts.py` (`out/logs/cr-001_02_contract-validation.log`).

| CR item | Realised in |
|---|---|
| US-009 | S3.4, S4.9, S6.3; `openapi.yaml` `searchRegistrations`; `organizer-search-ui.md` |
| AC-009-01, AC-009-02 | S6.3; `organizer-search-ui.md` "Page states" |
| AC-009-03 | S3.4, S7.2 (401 without challenge, D-26); `openapi.yaml` 401 |
| AC-009-04 | S6.3; `organizer-search-ui.md` "Page states" |
| AC-009-05 | S7.2 (HTTPS-only check covers the search); `openapi.yaml` 403 |
| AC-009-06 | S4.9 (newest first, count), S6.3 |
| AC-009-07 | S4.9 (row data), S6.3; `openapi.yaml` `SearchRegistration`; `organizer-search-ui.md` "Result" |
| AC-009-08..AC-009-15 | S4.9 (text match) |
| AC-009-16 | S3.4, S4.9 (validation); `openapi.yaml` 400 |
| AC-009-17 | S6.3; `organizer-search-ui.md` "Search and filters" |
| AC-009-18..AC-009-20 | S4.9 (filters) |
| AC-009-21 | S4.9 (counts list every option), S6.3; `organizer-search-ui.md` filter table |
| AC-009-22 | S4.9 (validation); `openapi.yaml` 400, `UNKNOWN_TYPE`, `WRONG_CATEGORY` |
| AC-009-23 | S6.3; `organizer-search-ui.md` "Result" |
| AC-009-24, AC-009-25 | S4.9 (counts), S6.3; `openapi.yaml` `CategoryCounts` |
| AC-009-26, AC-009-27 | S6.3 (D-25) |
| AC-009-28 | S9, S6.3 |
| AC-009-29 | S6.3 (React text only) |
| AC-009-30 | S6.3 (credentials in memory only) |
| E2E-06 | S6.3, S4.9 against the local stack (S8) |
| BR-09..BR-13 | S4.9, S6.3 |
| SR-03 (MODIFIED) | S7.1, S7.3 (organizer search group, D-23) |
| SR-07 (MODIFIED) | S4.9 (row data only) |
| SR-09 | S7.2, S6.3 |
| NFR-01 (MODIFIED) | S9, S6.3 |
| OQ-07 | S4.9 (text match) |
| OQ-08 | no date-range filter: S3.4 has none |
| OQ-09 | S7.2 (same account as the export) |

Persistent data: unchanged. The search reads the existing `registration` tables; no migration (ES-08).
