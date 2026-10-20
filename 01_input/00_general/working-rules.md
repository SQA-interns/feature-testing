# Working rules

> Owner: Team lead · Read in: every phase · Agent: read-only

## Ask the human (blocking)

Record a decision in `docs/decisions-log.md` (format under "Decision record"), ask, and wait when:

- anything checked in phase 0 is missing or wrong and only a human can fix it;
- a technology, version or service from `project/00_setup/tech-stack.md` does not work or needs changing;
- a frozen acceptance test appears wrong (only a human may update the manifest afterwards);
- a Critical or High finding would be downgraded or accepted.

Ask all open questions of a phase in one message. While waiting, finish work that does not depend on the answer.

## Decide and record (non-blocking)

When a requirement allows two behaviours a user would notice, choose the more conservative one, record it as "pending review", and continue.

Also non-blocking, record and continue: a finding listed in project/00_setup/pre-approved.md whose evidence matches (cite its ID, apply its action, do not ask); a pinned tool that runs but reports another version; a scanner analyser disabled for lack of credentials; dev-only tooling added under the `tech-stack.md` rule.

## Decision record

Append each decision to `docs/decisions-log.md` in this format:

```
## D-nn: <short title>
- Timestamp:
- Phase:
- Type: blocking | non-blocking
- Trigger: <what happened; cite IDs and files>
- Options: <numbered alternatives; mark the proposed default>
- Human response: <answer and time, or "none">
- Resolution: <chosen option> | pending review
```

- Never delete or rewrite a record; add a follow-up record instead.
- The phase 7 gate fails while any record lacks a resolution.

## Identifiers

| Prefix | Meaning | Defined in |
|---|---|---|
| `US-nnn` / `AC-nnn-nn` | User story / acceptance criterion | `project/01_requirements/user-stories.md` / `project/01_requirements/acceptance-criteria.md` |
| `BR-nn` / `OQ-nn` | Business rule / open question | `project/01_requirements/` |
| `AR-nn` | Architecture constraint | `project/02_design/architecture.md` |
| `SB-nn` / `SR-nn` | Security baseline / project security requirement | `general/security/` / `project/02_design/security-requirements.md` |
| `ES-nn` | Engineering standard | `general/engineering-standards.md` |
| `NFR-nn` | Non-functional requirement | `project/02_design/quality-requirements.md` |
| `DoD-nn` / `DoD-Pnn` | Done criterion, general / project | `general/quality/` / `project/02_design/quality-requirements.md` |
| `F-nn` / `D-nn` | Finding / decision | `docs/06_verification-report.md` / `docs/decisions-log.md` |

Tests, commits and findings reference these IDs.

## Commits

Commit as a human developer would: small, finished steps, never half a project. The report measures every point below on every commit, so each is a check, not advice.

- One commit = one logical change that can be reverted alone. Commit it when its checks pass, **before** starting the next change. Never collect several changes in the working tree and split them afterwards with commits in a row (commits less than 60 seconds apart are counted as a burst).
- Size: at most 15 files and 400 changed lines, not counting logs, lock files and generated files. Check with `git diff --cached --shortstat` before every commit and split when over, for example by layer (domain and use case, then persistence and adapters, then web/API, then UI), or by test class group. "State why it cannot be split" is only for a single generated file.
- One category per commit: code, tests, documents, or build/dependency files. Never mix them, and never add a document change (decision log, progress, README) to a code, test or build commit. A component skeleton is two commits: the build file with its pinned dependencies (`build`), then the source files (`chore`). A test harness or a docker/compose file is its own commit.
- Each fix (`F-nn`) is its own commit.
- The build passes at every commit and no previously passing test fails. Frozen acceptance and end-to-end tests of behaviour not yet built may fail.
- Message: `<type>: <summary> (IDs)`; type `feat`, `test`, `fix`, `refactor`, `docs`, `build` or `chore`; imperative; the **whole subject line including the IDs is at most 72 characters**. Name at most three IDs or one range (`AC-003-01..03`) in the subject; put the rest in the message body. Example: `feat: store registrations in one transaction (US-001, AC-001-01..07)`.
- Units per phase: `general/phases.md`. Working tree clean at every gate, apart from `03_statistics/` (the clock tool changes `run-log.json` all the time; do not commit it phase by phase).
- Do not squash, rebase or rewrite history.

## Progress

Update `docs/00_progress.md` at every gate and before any stop, so a fresh session can resume.

## Output

- Write command output longer than 50 lines to `out/logs/`; report only a summary and the path.
- Use only the severity levels in `general/quality/severity-scale.md`.

## Secrets (`.env`)

You never need to see a secret value; the tools that run need it, not you.

- Do not read `.env` with a file tool, and do not print, copy or log its values: no `cat`, `echo`, `env`, `printenv` or `docker compose config` on it, in commands, files, logs, commit messages or the conversation.
- Pass values to tools without showing them: `docker compose --env-file .env`, or `export NAME="$(sed -n 's/^NAME=//p' .env | tr -d '\r')"` inside the command that needs it.
- Check presence and emptiness only, as in `general/skills/preflight`.
- Before saving output from a command that may print an environment, filter it so no value is written.
- The phase 6 secret-leak check (`general/skills/verify-release`) must find nothing.

## Statistics

Record with `03_statistics/tools/stat.mjs`, which writes the real clock: `init` once in phase 0; `phase <n> start` and `phase <n> end` at every phase boundary (a start closes the previous phase, `phase 7 end` closes the run); `wait start <cause> "<reason>" [decisionId]` and `wait end` around every wait for the human, including stops and restarts. Edit `03_statistics/run-log.json` directly for test runs, findings, fix loops, decisions, deviations and quality results, as defined in `03_statistics/metrics.md`, and write `03_statistics/run-summary.md` in phase 7. Do nothing else in `03_statistics/`; in particular do not run `tools/build-report.mjs`, which belongs to the post-run session.
