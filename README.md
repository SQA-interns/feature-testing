# Project template

> Owner: Team lead · Agent: read-only

Template version: **1.7-ft**: 1.7 plus the feature experiment (SQA-interns/feature-testing): a fixed API contract (`02_design/api-contract.md`, AR-09), run type, feature and base run in `run-config.md` and `run-log.json` (`experiment`), and the hidden oracle's pass rates in the report (`results/oracle/`). Before that, **1.7** (1.6 without any usage panel or /cost step: every number is calculated from the session transcript and git, hooks build the report when the session ends, and nothing is typed or copied by hand; the report shows approval prompts and no longer compares against a harness total. The phase documents are committed once per phase, a targeted survivor round can decide the gate without a confirming full run, the verify skill has a traceability helper (`trace-ac.mjs`) and warns against the offline dependency scan, the agent no longer audits commit subjects, and the report fills the Run, Time, Commits and Cost sections of `run-summary.md`. 1.6 was 1.5 plus stricter commit rules (whole subject line at most 72 characters, one category per commit, layered build and test commits, commit before the next change), a post-run report with separate tables, run toggles and the environment of each run. 1.5 was 1.4 plus a fixed quality gate (mutation >= 90 %, line >= 95 %) instead of record-only thresholds, a staged phase 5 with at most three survivor rounds, and acceptance criteria, end-to-end scenarios and answers to the open questions as inputs, so that runs are comparable and cost to reach the gate is what differs. 1.4 was 1.3 plus: coverage and mutation measured in phase 5 and run once on final code, pre-approved findings, acceptance-test self-check, nginx Host and rate-limit requirements; vitest 4.1.11 pin; report fixes: short run name in charts, distinct commit colour, cost check from panel numbers entered in usage.md. 1.3 was 1.2 plus richer statistics: clock tool and unique run id, waits and agent time, deviations, environment, commits with cost, measurement scope, transcript backup, post-run report). Increase it whenever a file in section 2 changes; each run records it in `run-config.md`.

Humans start here; agents start at `AGENTS.md`. To execute a run, follow `Run_Guide.md`.

## Folders

| Folder | Content | Phase |
|---|---|---|
| `01_input/00_general/` | Rules, phases, skills, quality and security baselines for every project | all |
| `01_input/01_project/00_setup/` | Run configuration, secrets list, tech stack, environments | 0 |
| `01_input/01_project/01_requirements/` | Stories, business rules, scope, answered questions, acceptance criteria, end-to-end scenarios | 1 |
| `01_input/01_project/02_design/` | Architecture, security requirements, quality requirements | 2 |
| `02_output/` | Everything the agent produces (docs, code, logs) | 0-7 |
| `03_statistics/` | Experiment measurement (not part of the project) | all |

Project folder numbers match the phase that first uses them; later phases (3-7) use inputs from these folders and the general rules.

## 1. Fill in (human, for each project or run)

| File | What goes in it |
|---|---|
| `01_input/01_project/00_setup/run-config.md` | model, effort, run label, operator, machine, plan, permission mode, run type, feature, base run (every run); the run id is generated |
| `01_input/01_project/00_setup/secrets.env.example` | every secret the project needs (names only); copy to `.env` and fill the values there |
| `01_input/01_project/00_setup/tech-stack.md` | every platform, dependency and tool with exact versions |
| `01_input/01_project/00_setup/environments.md` | environments, settings, external services |
| `01_input/01_project/00_setup/pre-approved.md` | findings the human already decided (the agent records them without asking) |
| `01_input/01_project/01_requirements/user-stories.md` | user stories |
| `01_input/01_project/01_requirements/business-rules.md` | business rules, data fields, glossary |
| `01_input/01_project/01_requirements/scope.md` | in/out of scope, priorities, open questions with their answers |
| `01_input/01_project/01_requirements/acceptance-criteria.md` | the acceptance criteria, an input: the agent does not derive them |
| `01_input/01_project/01_requirements/e2e-scenarios.md` | the end-to-end scenarios, one test each, an input |
| `01_input/01_project/02_design/architecture.md` | components, constraints, interfaces |
| `01_input/01_project/02_design/security-requirements.md` | security level, authentication, personal data, project requirements |
| `01_input/01_project/02_design/quality-requirements.md` | non-functional requirements, thresholds, mutation scope, extra done criteria |
| `01_input/01_project/02_design/api-contract.md` | the fixed REST API (paths, fields, status codes, local test settings) that external checks rely on |
| `03_statistics/usage.md` | prices before the run (one row per model id, shape as in the file); nothing else |

Order: fill `01_requirements/` and `02_design/`, then `00_setup/`, then start the agent. It checks everything else in phase 0 and asks for whatever is missing.

## 2. Do not touch (change only when revising the template itself)

- `AGENTS.md`, `README.md`, `Run_Guide.md`
- everything in `01_input/00_general/`:
  - `working-rules.md`, `phases.md`, `engineering-standards.md`
  - `quality/definition-of-done.md`, `quality/severity-scale.md`, `quality/test-strategy.md`, `quality/measurement-scope.md`
  - `security/security-baseline.md`
  - `skills/preflight/SKILL.md`, `skills/write-acceptance-tests/SKILL.md`, `skills/verify-release/SKILL.md`
- `03_statistics/metrics.md`, `03_statistics/run-log.template.json`, `03_statistics/tools/` (the post-run measuring, report and transcript-export scripts)

## 3. Written by the agent (read, do not edit)

- `02_output/README.md` and one README per component
- `02_output/docs/`: `00_preflight-report.md`, `00_input-manifest.sha256` (frozen once written), `00_progress.md`, `01_requirements-check.md`, `02_specification.md`, `02_contracts/`, `03_acceptance-manifest.sha256`, `03_test-strategy.md`, `06_verification-report.md`, `decisions-log.md`, `release-notes.md`
- `02_output/logs/`, all source code and tests in `02_output/`
- repository files required by ES-03 at the repository root: `.gitattributes`, `.gitignore`
- `03_statistics/run-log.json` (through `tools/stat.mjs` and by hand), `03_statistics/run-summary.md`
- written by the post-run session: `03_statistics/results/` (report, measurements, usage summary, commits, readable transcript) and `03_statistics/data/` (transcript backup, committed with the run)

Any change to sections 1 and 2 during a run is detected in phase 6 by comparing `00_input-manifest.sha256`.

## Rules for editing this template

- Start every Markdown file with `> Owner: … · Read in: … · Agent: …` (after the front matter in a `SKILL.md`; agent-written skeletons use `> Written in: … · Source: … · Agent: writes`) and add it to the right list above.
- State each rule once; link to it elsewhere. Write constraints ("Do not …"); procedures go in skills.
- One topic per file, no fixed length: split a file when it covers two topics or a reader needs only half of it. Versions appear only in `tech-stack.md`.
