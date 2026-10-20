# Decisions of the feature experiment

> Branch `experiment` of SQA-interns/feature-testing · Shared by greenfield (tanej) and brownfield · Never copied into a run folder

One row per decision, newest last. "Status" is `agreed` (both sides use it) or `proposed` (waiting for review).

## Experiment design

| # | Date | Decision | Status |
|---|---|---|---|
| E-01 | 2026-10-20 | Same project (conference registration, template 1.7 from SQA-interns/Structured_Input_Files `e89480d`) for every run; `main` of this repo is that template plus the experiment's changes. | agreed |
| E-02 | 2026-10-20 | Two features, `search` (organizer search and filter) and `group` (group registration). Each feature is built both ways: greenfield (tanej, feature in the inputs from the start) and brownfield (colleague, feature added as a change to a finished normal run). | agreed |
| E-03 | 2026-10-20 | Per feature: one or more normal runs (no feature, baseline), one or more greenfield runs, one or more brownfield runs. A normal run serves as baseline for both features. | agreed |
| E-04 | 2026-10-20 | Branch names `<user>/single-agent/sdd/v001/run-<n>[-<feature>]`; the run number counts per feature (first runs: `run-1-search`, `run-1-group`); normal runs have no suffix. | agreed |
| E-05 | 2026-10-20 | Feature packages, oracle and instructions live on the orphan branch `experiment`, which is never cloned into a run folder (clone runs with `--single-branch`). | agreed |
| E-06 | 2026-10-20 | Each feature package is the only specification of its feature and is used word for word by greenfield and brownfield; `features/build-inputs.py` generates the finished input files from it and checks every block is used verbatim. | agreed |
| E-07 | 2026-10-20 | Both packages use the next free IDs of the base project (US-009, BR-09…, AC-009-…, E2E-06, DoD-P06): a run gets exactly one feature, so its inputs have no gaps and do not hint at the other feature. | agreed |
| E-08 | 2026-10-20 | A hidden oracle (feature scenarios plus regression scenarios of the base project) judges every run the same way; template 1.7 has none. | agreed |
| E-09 | 2026-10-20 | The oracle needs a known API, but template 1.7 lets the agent design the API. Option A chosen: a fixed API contract becomes a project input (`02_design/api-contract.md`) for the base project, and each feature package adds its endpoints. Rejected: B (oracle through the browser, fragile because every run's UI differs), C (map each run's API by hand, a human judgement per run). Consequence: new normal runs in this repo get the contract too, so they are compared with each other, not with earlier 1.7 runs. | agreed |

## Feature `search` (US-009 organizer search and filter)

| # | Decision | Status |
|---|---|---|
| S-01 | The search text matches any part of first name, last name, email, organization or study institution, without regard to upper or lower case; accents are not ignored ("Sustersic" does not find "Šušteršič"). | agreed |
| S-02 | Filters: registration type and one selected option, combined with the text (all must match); no date filter. | agreed |
| S-03 | Results newest first, 20 per page, with the total number of matches. | agreed |
| S-04 | Search text at most 100 characters; invalid type, unknown option, page below 1 or too long text give HTTP 400. | agreed |
| S-05 | Same organizer credentials and HTTPS-only rule as the export; how the page asks for them is the agent's design choice, as for the export. | agreed |
| S-06 | Out of scope: editing or deleting from the list, exporting the filtered list, saved searches. "No admin UI" in `scope.md` is reworded so the search page is the only organizer page. | agreed |

## Feature `group` (US-009 group registration)

| # | Decision | Status |
|---|---|---|
| G-01 | A contact person (not a payer: payment is out of scope in the base project) registers 2 to 10 members of one organization; members are external participants only, no student or mixed groups. | proposed |
| G-02 | The contact person gives each mandatory consent for the whole group; the contact person is a member only if also listed as one. | proposed |
| G-03 | All or nothing: if any member is invalid or any member's JSON copy cannot be written, nothing is stored and no email is sent (new AR-08). | proposed |
| G-04 | Each member is a normal external registration with its own database row and JSON copy, both linked to the group and naming the contact person. | proposed |
| G-05 | Emails: one confirmation per member, one group confirmation to the contact person, one organizer notification per group with one JSON attachment per member. | proposed |
| G-06 | Export: one row per member with the group id and contact person; individual registrations leave those cells empty. | proposed |
| G-07 | Two members with the same email (any upper or lower case) are rejected. | proposed |
