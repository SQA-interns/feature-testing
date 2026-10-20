# Quality requirements

> Owner: QA lead · Read in: phases 2, 6, 7 · Agent: read-only

Adds to `general/quality/` (definition of done, test strategy, severity scale); do not repeat those here.

## Non-functional requirements

| ID | Category (performance, availability, accessibility, localisation, usability, …) | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | Localisation | Names and other text with Slovenian characters (č, š, ž) survive the form, database, JSON copy, emails and Excel export unchanged. | an end-to-end test with such input |
| NFR-02 | Reliability | Stored registrations and JSON copies survive recreation of every container. | runtime demonstration: `docker compose down` then `up`, data still present |
| NFR-03 | Usability | The frontend validates fields before submission and shows the backend's field errors next to the fields. | component tests |
| NFR-04 | Availability | The backend reports health and readiness, and the containers use them in health checks. | runtime demonstration |

## Thresholds

These thresholds are the **quality gate** of the project: the same in every run, per component (backend, frontend), over the scope and with the tests defined in `general/quality/measurement-scope.md`. The agent works until each component meets them, then stops adding tests for that component (rules and stop rule: "Quality gate" in `measurement-scope.md`). The rows below are read by `tools/build-report.mjs`; keep the format `>= <number> %`.

| Measure | Threshold |
|---|---|
| Line coverage (all levels together) | >= 95 % |
| Mutation score (adjusted) | >= 90 % |
| Branch coverage | record only |

## Mutation scope

The paths or packages whose code is mutated, the same in every run of this project (default: all production code of the component). Selection rule: validation, security, persistence and business-rule code. Rules: `general/quality/measurement-scope.md`.

| Component | Paths or packages | Exclusions and reason |
|---|---|---|
| backend | `si.konferenca.registration.**` (all production code under the root package, including validation, security, persistence, business-rule, mail and export code) | The `@SpringBootApplication` entry-point class (no logic); generated code. Nothing else may be excluded. |
| frontend | `src/**/*.{ts,tsx,js,jsx,vue}` (all production source of the frontend) | Test files, type-declaration files (`*.d.ts`), the bootstrap file that only mounts the app, generated code. Nothing else may be excluded. |

The scope is all production code of both components, so mutation scores are on the same basis in every run. It is the root package and the source folder, not a list of sub-packages, because the agent chooses the sub-packages in phase 2. Each component's full automated suite runs against it (`measurement-scope.md`, "Mutation score").

## Additional done criteria

| ID | Criterion | Evidence |
|---|---|---|
| DoD-P01 | Against the running local stack: one external and one student registration succeed. | runtime demonstration log |
| DoD-P02 | Each registration is in PostgreSQL and has its JSON copy. | database query, file listing |
| DoD-P03 | The participant email and the organizer email (with the JSON attachment) arrive in Mailpit. | Mailpit API output |
| DoD-P04 | The organizer downloads a valid Excel workbook; the same request without organizer access is refused. | response codes, opened workbook |
| DoD-P05 | The production reCAPTCHA code path is tested against a mocked verification endpoint, including a rejected token. | test report |
| DoD-P06 | Against the running local stack: the organizer finds a registration by last name and by type on the search page; the same search without organizer access is refused. | runtime demonstration log |

## Overrides

None.
