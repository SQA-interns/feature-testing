# Measurement scope

> Owner: QA lead · Read in: phases 3, 5, 6 · Agent: read-only

Defines what test counts, coverage, mutation score and code size mean, so that figures from different runs compare. Record every figure in `run-log.json` → `codeMetrics` and in `docs/06_verification-report.md` with its scope, level and tool. The project selects paths and exclusions in `project/02_design/quality-requirements.md` ("Mutation scope"); this file never names them.

## Constraints

- Do not change a tool's scope, filters, exclusions or thresholds to improve a number. A change is a decision record.
- Do not report a figure without its scope, level and tool.
- Production code is the source a component ships. It excludes tests, generated code, the application entry point and anything listed under "Measurement exclusions" in `docs/03_test-strategy.md` with a reason. Decide the exclusions in phase 3, before any test is written; do not extend them later.
- Test code never counts towards coverage or mutation.

## Test counts

Per component and level (`unit`, `integration`, `acceptance`, `architecture`, `e2e`), passed and failed, from the runner's own report.

## Coverage

Line and branch, per component, as three figures: `unit` (unit tests only), `nonUnit` (integration, acceptance and end-to-end tests that exercise the component) and `all` (one merged report). The headline figure is `all`.

## Mutation score

- **Scope:** all production code of the component, narrowed only to the paths in `project/02_design/quality-requirements.md` "Mutation scope", which are the same in every run of the project (default: all production code). Do not narrow it by running only some tests or by excluding packages that are not listed there.
- **Tests:** the component's full automated suite, at every level it has, so the score does not depend on which level the agent ran. If that is infeasible, record the tests actually run and the reason in a decision record.
- **Record:** `{score, scoreAdjusted, scoreCovered, killed, equivalent, total, covered, scope, tests, tool, excluded}`. `score` (raw) = killed (timed-out counts as killed) ÷ all mutants the tool generated; a mutant no test reaches counts as survived. `scoreAdjusted` = (killed + equivalent) ÷ total, where `equivalent` is the number of survivors justified as equivalent. `scoreCovered` = killed ÷ mutants a test reaches.
- Do not remove equivalent mutants by hand. List every survivor you count as equivalent in `docs/03_test-strategy.md` with the class and method, the mutation and the reason it cannot change behaviour; a survivor without that entry is not equivalent. Always record the raw score next to the adjusted score.

## Code size

`cloc` (same version in every run): production = non-test source and resources, test = test sources including end-to-end tests. Generated code and lock files are excluded.

## Mutation run discipline

- Measure coverage and mutation score in phase 5, so that surviving mutants are handled before verification. Phase 6 repeats a measurement only for a component whose code or tests changed since, and only once, after the last fix.
- Do not start a full mutation run on code that is not final. One full run per component per measurement; the only other runs are the targeted survivor rounds and, only when the rule below requires it, one confirming full run.
- Check once, before the full run, that the tool works: a mutant in code the tests plainly cover must be killed. A score far below the line coverage (for example 25 % at 100 % coverage) means the tool is not activating mutants (known case: Stryker with the wrong Vitest major version); stop and raise a decision, do not record the figure.
- Start a long run in the background and continue with other checks of the phase; read its log only for the summary lines. Do not hold the session in a blocking wait on it.

## Quality gate

The gate is the same in every run of the project: the thresholds in `project/02_design/quality-requirements.md` ("Thresholds"), per component, over the scope and with the tests defined above. It makes runs comparable: every run reaches the same quality, and what differs is the cost of reaching it.

- **Gate:** line coverage (`all`) at or above the line threshold, and `scoreAdjusted` at or above the mutation threshold. Branch coverage is recorded only.
- **Order in phase 5:** write the unit and integration tests area by area and measure line coverage after each area (fast) until the line threshold is met; then run mutation testing **once** per component (full run); then, if `scoreAdjusted` is below the threshold, up to **three** survivor rounds: each adds tests aimed at the survivors in the code that matters most (validation, security, persistence, business rules first) and re-runs only the mutated classes. After each round recompute `scoreAdjusted` for the component from the last full run plus the re-run classes (the classes that were not re-run keep their killed and total counts from the full run) and write the per-class numbers into `docs/03_test-strategy.md`. If the recomputed score is at least 1.0 point above the threshold the gate is met and no full run follows. Only a recomputed score less than 1.0 point above the threshold, or a change to a class outside the re-run set, needs the one confirming full run; its result then replaces the recomputed one.
- **Stop at the gate:** once a component meets both thresholds, add no further test and start no further mutation run for it. Do not add tests for coverage or score beyond the gate.
- **Shortfall:** after the third round a component still below a threshold is recorded as a shortfall (which threshold, by how many points) in a non-blocking decision and as a finding, and the run continues. Do not change the scope, the tests run or the exclusions to reach a threshold; that is a decision record at most.
- **Record in `run-log.json`:** `codeMetrics.gate` = `{ thresholds: {lineCoverage, mutationScore}, perComponent: { <name>: { lineMet, mutationMet, lineShortfall, mutationShortfall } }, mutationRounds: { <name>: n }, testsAtFirstMutation: { <name>: n }, met: true|false }`; `mutationRounds` is 0 when the first full run already met the gate.
