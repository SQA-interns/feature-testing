# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single-agent SDD run (`general/phases.md`), run label `tjan/single-agent/sdd/v002/run-2`, run id `tjan-single-agent-sdd-v002-run-2-opus-5-5-t1.8-20261027T0947Z-dda4`
- Current phase: 4 complete; phase 5 (unit tests and quality gate) next
- Last gate result: phase 4 passed 2026-10-27: 101/101 frozen tests (backend 89, frontend 7, e2e 5 on 127.0.0.1:8080/8025); format, lint, type and static checks clean; frozen harness changed under D-16 and re-frozen (44fe78a)
- Next step: `stat.mjs phase 5 start`; ArchUnit rules (specification S2) and unit tests per area; JaCoCo / Vitest coverage to 95 % line; then one PIT and one Stryker full run (gate 90 % adjusted), at most three survivor rounds
- Environment notes:
  - Node via nvm, as the operator does: `source ~/.nvm/nvm.sh; nvm use` in `02_output/frontend` (reads `.nvmrc`), or `nvm use 24.13.0` at the root. Use `;` after sourcing: nvm.sh returns 3 because the default alias `lts/*` does not resolve
  - Docker Desktop: the `docker` CLI and Testcontainers find its socket by themselves; no DOCKER_HOST or socket override needed
  - Dependency-Check: export `NVD_API_KEY` from `.env` inside the command (`working-rules.md`, Secrets)
  - `grep` is aliased to `ugrep` in the shell; use `/usr/bin/grep` where GNU options matter
- Commands (ES-05): backend build `./mvnw -B package`, test `./mvnw -B test`, check `./mvnw -B spotless:check pmd:check compile spotbugs:check`, run `./mvnw spring-boot:run`; frontend build `npm run build`, test `npm test`, check `npm run check`, run `npm run dev`
- Contract re-check: `python3 -I 02_output/tools/validate-contracts.py 02_output/docs/02_contracts <oas31.json>` (D-15)
- Local stack: run as `COMPOSE_PROJECT_NAME=registration-sdd-run2` (an older run's volume `registration_json-copies` exists under the default name); default ports 8080/8025
- Waiting for the human on: nothing
