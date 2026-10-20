# Brownfield run

> Owner: brownfield colleague · Shared parts are fixed; the procedure below is a **proposal** for the brownfield owner to decide and complete

## Fixed: shared with greenfield (do not change)

| What | Where | Why it must be the same |
|---|---|---|
| Base template and project | `main` (template 1.7 plus the fixed API contract) | only "when the feature arrives" may differ |
| Feature text | `features/<feature>/feature-package.md` and the generated `features/<feature>/01_input/` | greenfield gets exactly these files; the brownfield change request must be exactly the same text |
| Model, effort, stack, pins, pre-approved findings, quality gate | `main` | held fixed in every run |
| Standard answers to tooling questions; the rule "no information beyond the package" | `Run_Guide.md` on `main` | same human behaviour on both sides |
| Hidden oracle, run with `--feature <feature>` | `oracle/` | one outside quality measure for all runs |
| Statistics and HTML report | `03_statistics/` on `main` | the same measurements, so runs are comparable |
| Naming | `README.md` of this branch | `<user>/single-agent/sdd/v001/run-<n>-<feature>`, Run type `brownfield` |

## Proposed procedure (to be decided by the brownfield owner)

0. **Have this branch in its own folder** (once), outside every run folder; it holds the feature files and the oracle:
   ```bash
   git clone -b experiment https://github.com/SQA-interns/feature-testing.git feature-testing-experiment
   ```
1. **Starting point**: the final commit of a finished normal run (`instructions/normal.md`). Clone that run's branch into a new folder, then create the brownfield branch from its final commit. Write down the SHA as Base run.
2. **Reset the statistics, keep the system**: remove the base run's `03_statistics/run-log.json`, `run-summary.md` content, `results/` and `data/` (they stay in git history), so the brownfield run measures only the change. `02_output/` (code, tests, docs, frozen-test manifest) stays as the base run left it.
3. **Apply the change to the inputs**: copy `features/<feature>/01_input/` over `01_input/`, exactly as greenfield does. The inputs then describe the system with the feature.
4. **`run-config.md`**: Run label `<user>_single-agent_sdd_v001_run-<n>-<feature>`; Run type `brownfield`; Feature `<feature>`; Base run `<base run label> @ <SHA>`. Commit ("change request <feature>") and push.
5. **Tell the agent it is a change**: template 1.7's phases assume an empty `02_output/`. Decide how the agent treats the existing system, for example a short `01_input/00_general/workflows/change.md` (or a fixed start prompt) that says:
   - the existing `02_output/` is a finished, verified system; the change is US-009 and the files that changed in `01_input/`;
   - which phases run for the change (proposal: 1 requirements check of US-009 only, 2 design update, 3 acceptance tests for AC-009-xx and E2E-06 only, 4 build, 5 unit tests, 6 verify the whole system, 7 release);
   - the existing frozen tests stay frozen and unchanged; new acceptance tests get their own manifest (for example `docs/03_acceptance-manifest-change.sha256`); the full suite, old and new, must pass;
   - the quality gate applies to the whole system after the change.
   Whatever is chosen must be the same in every brownfield run, and is a template change: record it in `DECISIONS.md`.
6. **After the run**: commit the statistics as the run guide says. Then run the oracle with `--feature <feature>` exactly as `oracle/README.md` shows (two backend starts: rate limit 10000, then 3; results in a folder outside the run folder), and follow its section "Oracle results into the statistics" (copy the three result files to `03_statistics/results/oracle/`, rebuild the report, commit, push).

## Brownfield-only measures worth recording

- regressions: old tests or oracle regression scenarios (R01–R15) that fail after the change;
- old frozen test files touched (expected 0) and the old manifest still matching;
- existing production files changed, and lines changed in them (from `git diff --stat <base SHA>..`);
- a new database migration added (yes or no).

## Open questions for the brownfield owner

1. Is step 2 (reset statistics, keep `02_output/`) right, or should the base run's documents be kept as context for the agent?
2. Which phases run for a change, and how are the new acceptance tests frozen (step 5)?
3. Which normal run is the base for each feature, and is the same base used for `search` and `group`?
