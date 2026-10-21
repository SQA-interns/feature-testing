# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single agent, spec-driven (`general/phases.md`), run `tjan_single-agent_sdd_v001_run-1-search`, change run for US-009 on the base run `tanej_single-agent_sdd_v001_run-1` (D-18)
- Current phase: 4 (build)
- Last gate result: phase 3 passed 2026-10-21: 34 US-009 tests (backend 30, frontend 3, e2e 1) fail for behavioural reasons except AC-009-09 (base security already answers 401); frozen in `docs/03_acceptance-manifest-us-009.sha256` (commit 943abd7); base manifest unchanged and verified
- Change rules: the base frozen tests and `docs/03_acceptance-manifest.sha256` stay unchanged; US-009 acceptance and e2e tests are frozen in `docs/03_acceptance-manifest-us-009.sha256`
- Local stack: run with `COMPOSE_PROJECT_NAME=konferenca-us009` (D-24)
- Next step: build US-009 layer by layer (domain and use case, persistence, web/API, UI)
- Waiting for the human on: nothing
