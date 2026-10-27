# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single-agent SDD run (`general/phases.md`), run label `tjan/single-agent/sdd/v002/run-2`, run id `tjan-single-agent-sdd-v002-run-2-opus-5-5-t1.8-20261027T0947Z-dda4`
- Current phase: 4 (build): backend, frontend and stack implemented; gate blocked by D-16
- Last gate result: phase 3 passed 2026-10-27: 101 frozen tests (89 backend acceptance, 7 frontend acceptance, 5 e2e), all failing for behavioural reasons on the skeleton; freeze commit e933b1c; 35/35 AC traced
- Next step: apply the human's answer to D-16 (frozen harness change), re-run the backend suite (expect 89/89), then the phase 4 gate (all frozen tests, format/lint/type/static checks), phase 4 end
- Environment notes:
  - Node via nvm, as the operator does: `source ~/.nvm/nvm.sh; nvm use` in `02_output/frontend` (reads `.nvmrc`), or `nvm use 24.13.0` at the root. Use `;` after sourcing: nvm.sh returns 3 because the default alias `lts/*` does not resolve
  - Docker Desktop: the `docker` CLI and Testcontainers find its socket by themselves; no DOCKER_HOST or socket override needed
  - Dependency-Check: export `NVD_API_KEY` from `.env` inside the command (`working-rules.md`, Secrets)
  - `grep` is aliased to `ugrep` in the shell; use `/usr/bin/grep` where GNU options matter
- Commands (ES-05): backend build `./mvnw -B package`, test `./mvnw -B test`, check `./mvnw -B spotless:check pmd:check compile spotbugs:check`, run `./mvnw spring-boot:run`; frontend build `npm run build`, test `npm test`, check `npm run check`, run `npm run dev`
- Contract re-check: `python3 -I 02_output/tools/validate-contracts.py 02_output/docs/02_contracts <oas31.json>` (D-15)
- Local stack for e2e runs: `COMPOSE_PROJECT_NAME=registration-sdd-run2 FRONTEND_HOST_PORT=18080 MAILPIT_HOST_PORT=18025`; other projects on this machine use the project name `registration` (an Oct 9 volume `registration_json-copies` owned by uid 100) and ports 8080/8025, and are left alone
- Status: backend acceptance 85/89 (4 in RecaptchaVerificationAcceptanceTest fail because of D-16), frontend acceptance 7/7, e2e 5/5 (`out/logs/04_*`)
- Waiting for the human on: D-16
