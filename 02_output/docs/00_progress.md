# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: single-agent SDD run (`general/phases.md`), run label `tjan/single-agent/sdd/v002/run-2`, run id `tjan-single-agent-sdd-v002-run-2-opus-5-5-t1.8-20261027T0947Z-dda4`
- Current phase: 6 complete; phase 7 (release) next
- Last gate result: phase 5 passed 2026-10-27: first full run 101/101 before any unit test; backend 301/301 (line 99.44 %, PIT 92.71 % raw = adjusted), frontend 74/74 (line 100 %, Stryker 90.30 % raw = adjusted), no survivor round; checks clean; frozen manifest matches. Figures in `docs/03_test-strategy.md`; e2e outside the coverage and mutation runs (D-17). No production code changed in phase 5, so phase 6 re-measures nothing unless a fix changes code
- Last phase 6 result: gate passed after D-18 (option 1): 380/380 tests, manifests match, no open Critical/High, runtime demo and scanners in `docs/06_verification-report.md`
- Next step: `stat.mjs phase 7 start`; `out/README.md`, backend and frontend READMEs, `docs/release-notes.md` (manual tests, OQ-06 retention, HTTP Basic with TLS, dev-only advisories); clean-checkout check
- Environment notes:
  - Node via nvm, as the operator does: `source ~/.nvm/nvm.sh; nvm use` in `02_output/frontend` (reads `.nvmrc`), or `nvm use 24.13.0` at the root. Use `;` after sourcing: nvm.sh returns 3 because the default alias `lts/*` does not resolve
  - Docker Desktop: the `docker` CLI and Testcontainers find its socket by themselves; no DOCKER_HOST or socket override needed
  - Dependency-Check: export `NVD_API_KEY` from `.env` inside the command (`working-rules.md`, Secrets)
  - `grep` is aliased to `ugrep` in the shell; use `/usr/bin/grep` where GNU options matter
- Phase 6: report `docs/06_verification-report.md`; runtime demo script was run from the scratchpad (logs `06_runtime-demo.log`, `06_sr02-startup-refusal.log`); Semgrep `docker run semgrep/semgrep:1.177.0 semgrep scan --config p/default`, Gitleaks `docker run zricethezav/gitleaks:v8.30.1 git /repo --redact`
- Measurement: backend `./mvnw -B clean verify` (JaCoCo report in `target/site/jacoco`), `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage`; frontend `npm run coverage`, `npm run mutation`
- Commands (ES-05): backend build `./mvnw -B package`, test `./mvnw -B test`, check `./mvnw -B spotless:check pmd:check compile spotbugs:check`, run `./mvnw spring-boot:run`; frontend build `npm run build`, test `npm test`, check `npm run check`, run `npm run dev`
- Contract re-check: `python3 -I 02_output/tools/validate-contracts.py 02_output/docs/02_contracts <oas31.json>` (D-15)
- Local stack: run as `COMPOSE_PROJECT_NAME=registration-sdd-run2` (an older run's volume `registration_json-copies` exists under the default name); default ports 8080/8025
- Waiting for the human on: nothing
