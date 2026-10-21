# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single agent, spec-driven (`general/phases.md`), run `tjan_single-agent_sdd_v001_run-1-search`, change run for US-009 on the base run `tanej_single-agent_sdd_v001_run-1` (D-18)
- Current phase: 5 (unit tests)
- Last gate result: phase 4 passed 2026-10-21: backend 281/281 (incl. SearchAcceptanceTest 30), frontend 58/58, e2e 6/6, check commands clean; US-009 built in 6 feat commits; both acceptance manifests match
- Change rules: the base frozen tests and `docs/03_acceptance-manifest.sha256` stay unchanged; US-009 acceptance and e2e tests are frozen in `docs/03_acceptance-manifest-us-009.sha256`
- Local stack: run with `COMPOSE_PROJECT_NAME=konferenca-us009` (D-24)
- Next step: phase 5 first full run, unit tests for the new code, coverage and mutation (backend PIT, frontend Stryker)
- Waiting for the human on: nothing
