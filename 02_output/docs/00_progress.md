# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single agent, spec-driven (`general/phases.md`), run `tjan_single-agent_sdd_v001_run-1-search`, change run for US-009 on the base run `tanej_single-agent_sdd_v001_run-1` (D-18)
- Current phase: 3 (test design)
- Last gate result: phase 2 passed 2026-10-21: spec 3.4a and 3.6, openapi `GET /api/registrations`, interfaces "Organizer search page"; every AC-009, SR-09, BR-09, BR-10, DoD-P06 mapped; openapi.yaml parses (PyYAML) and all $refs resolve
- Change rules: the base frozen tests and `docs/03_acceptance-manifest.sha256` stay unchanged; US-009 acceptance and e2e tests are frozen in `docs/03_acceptance-manifest-us-009.sha256`
- Local stack: run with `COMPOSE_PROJECT_NAME=konferenca-us009` (D-24)
- Next step: write US-009 acceptance tests (backend SearchAcceptanceTest, frontend organizer acceptance) and E2E-06, then freeze them in `docs/03_acceptance-manifest-us-009.sha256`
- Waiting for the human on: nothing
