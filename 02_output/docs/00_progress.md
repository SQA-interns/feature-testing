# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single-agent SDD run (`general/phases.md`), run label `tjan/single-agent/sdd/v002/run-2`, run id `tjan-single-agent-sdd-v002-run-2-opus-5-5-t1.8-20261027T0947Z-dda4`
- Current phase: 2 complete; phase 3 (test design) next
- Last gate result: phase 2 passed 2026-10-27: every AC, SR, SB, NFR, AR, BR, DoD-P maps to `docs/02_specification.md`; contracts validated (`out/logs/02_contract-validation.log`, `out/logs/02_contract-sql.log`); D-11..D-14 pending review
- Next step: `stat.mjs phase 3 start`; read `general/quality/test-strategy.md`, `general/quality/measurement-scope.md`, skill `general/skills/write-acceptance-tests`; write acceptance and e2e tests per user story (no production code), then the freeze commit with `docs/03_acceptance-manifest.sha256`
- Environment notes:
  - Node via nvm, as the operator does: `source ~/.nvm/nvm.sh; nvm use` in `02_output/frontend` (reads `.nvmrc`), or `nvm use 24.13.0` at the root. Use `;` after sourcing: nvm.sh returns 3 because the default alias `lts/*` does not resolve
  - Docker Desktop: the `docker` CLI and Testcontainers find its socket by themselves; no DOCKER_HOST or socket override needed
  - Dependency-Check: export `NVD_API_KEY` from `.env` inside the command (`working-rules.md`, Secrets)
  - `grep` is aliased to `ugrep` in the shell; use `/usr/bin/grep` where GNU options matter
- Commands (ES-05): backend build `./mvnw -B package`, test `./mvnw -B test`, check `./mvnw -B spotless:check pmd:check compile spotbugs:check`, run `./mvnw spring-boot:run`; frontend build `npm run build`, test `npm test`, check `npm run check`, run `npm run dev`
- Contract re-check: `python3 -I 02_output/tools/validate-contracts.py 02_output/docs/02_contracts <oas31.json>` (D-15)
- Waiting for the human on: nothing
