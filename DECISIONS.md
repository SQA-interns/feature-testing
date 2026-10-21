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
| E-10 | 2026-10-20 | The base contract (`main`, commit `a3f308f`) fixes form configuration, registration and export, the local URLs (backend 8080, Mailpit 8025) and three test settings passed through Compose: `CAPTCHA_TEST_MODE` (token `test-pass`), `RATE_LIMIT_PER_MINUTE`, `ORGANIZER_HTTPS_ONLY`. Each feature package adds its endpoint (`search`: `GET /api/registrations`; `group`: `POST /api/group-registrations` and three export columns). | agreed |
| E-11 | 2026-10-20 | The contract states that a request over the rate limit is answered with HTTP 429 (`main` `63c14db`), so the oracle can check SR-03. | agreed |
| E-12 | 2026-10-20 | Oracle (`oracle/`): regression R01–R15 for every run, `search` S01–S10, `group` G01–G11; API, Mailpit and export only; letters-only generated names; two phases (main with a high rate limit, ratelimit with 3 per minute); results outside the repository; `summarize.py` gives a regression and a feature pass rate. UI-only criteria (search page, group form) and the JSON copy files are not checked by the oracle; the run's own frozen tests and phase 6 cover them. | agreed |## Feature `search` (US-009 organizer search and filter)
| E-13 | 2026-10-20 | Statistics, additive only (`main` `c12aee2`): `run-config.md` gets Run type, Feature and Base run; `stat.mjs` copies them into `run-log.json` as `experiment`; the report groups runs by run type and feature and shows two new metrics from `results/oracle/oracle-summary.json` (base-project and feature pass rates). Runs without these fields count as normal runs without oracle data, so earlier 1.7 runs stay readable. | agreed |
| E-14 | 2026-10-20 | Template version `1.7-ft` (1.7 plus the feature experiment's changes), so it is not confused with a later 1.8 of Structured_Input_Files. | agreed |
| E-15 | 2026-10-20 | Baseline: at least one new normal run on template 1.7-ft (tanej runs `run-1` first; it is also the base for brownfield). The earlier 1.7 runs (tanej v001, kyuhi's last two in Structured_Input_Files) are an extra reference only: their APIs do not follow the fixed contract, so the oracle cannot test them and a brownfield run starting from them would first have to rebuild the API; their inputs also differ by the contract; kyuhi's ran on another machine. | agreed |
| E-16 | 2026-10-21 | A brownfield run starts in a **new session** from the base run's final commit (for both features: tanej run-1, `84f9bde`); it never continues or forks the normal run's chat. Reasons: brownfield means someone who did not build the system gets code, tests, documents and a change request (as in FeatureBench, FEA-Bench, PACE-Bench); the statistics are measured from the session transcript, so a continued or forked chat would mix the baseline's tokens, cost and time into the brownfield run; it keeps brownfield comparable with greenfield, which also starts with a fresh agent; and the baseline's large context would make every brownfield call more expensive. A "warm" variant (the building agent adds the feature) would be a separate run type, not part of this experiment. | agreed |

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
| G-01 | A contact person (not a payer: payment is out of scope in the base project) registers 2 to 10 members of one organization; members are external participants only, no student or mixed groups. | agreed |
| G-02 | The contact person gives each mandatory consent for the whole group; the contact person is a member only if also listed as one. | agreed |
| G-03 | All or nothing: if any member is invalid or any member's JSON copy cannot be written, nothing is stored and no email is sent (new AR-08). | agreed |
| G-04 | Each member is a normal external registration with its own database row and JSON copy, both linked to the group and naming the contact person. | agreed |
| G-05 | Emails: one confirmation per member, one group confirmation to the contact person, one organizer notification per group with one JSON attachment per member. | agreed |
| G-06 | Export: one row per member with the group id and contact person; individual registrations leave those cells empty. | agreed |
| G-07 | Two members with the same email (any upper or lower case) are rejected. | agreed |
