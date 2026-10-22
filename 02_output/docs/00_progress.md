# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven, acceptance tests first (`general/phases.md`)
- Current phase: 4 (build)
- Last gate result: phase 3 passed (2026-10-22): 48 AC covered by 123 acceptance tests (105 backend, 18 frontend) and 6 e2e tests, all failing on the skeleton for behavioural reasons; frozen by 1ddcadd (`docs/03_acceptance-manifest.sha256`, 28 files)
- Next step: phase 4: implement per story and layer until all frozen tests pass; local stack: `docker compose --env-file ../.env up -d --build` in 02_output
- Waiting for the human on: nothing
