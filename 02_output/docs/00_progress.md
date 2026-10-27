# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single-agent SDD run (`general/phases.md`), run label `tjan/single-agent/sdd/v002/run-2`, run id `tjan-single-agent-sdd-v002-run-2-opus-5-5-t1.8-20261027T0947Z-dda4`
- Current phase: 0 complete; phase 1 (requirements) next
- Last gate result: phase 0 passed 2026-10-27 (`docs/00_preflight-report.md`); D-01..D-10 all resolved
- Next step: `stat.mjs phase 1 start`, then write `docs/01_requirements-check.md` from `project/01_requirements/*`
- Environment notes:
  - Node 24.13.0 is not on the default PATH: prefix commands with `PATH=$HOME/.nvm/versions/node/v24.13.0/bin:$PATH`
  - Testcontainers (Docker Desktop): `DOCKER_HOST=unix://$HOME/.docker/desktop/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
  - Dependency-Check: export `NVD_API_KEY` from `.env` inside the command (`working-rules.md`, Secrets)
  - `grep` is aliased to `ugrep` in the shell; use `/usr/bin/grep` where GNU options matter
- Commands (ES-05): backend build `./mvnw -B package`, test `./mvnw -B test`, check `./mvnw -B spotless:check pmd:check compile spotbugs:check`, run `./mvnw spring-boot:run`; frontend build `npm run build`, test `npm test`, check `npm run check`, run `npm run dev`
- Waiting for the human on: nothing
