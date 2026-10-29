# Preflight report: CR-001

> Written in: phase 0 of the change run · Source: `project/00_setup/*`, `cr/` · Procedure: `general/skills/preflight`, `general/change-phases.md` phase 0 · Agent: writes

The CR adds no dependency or tool (no `cr/tech-stack.md`), so the checks run against the baseline `tech-stack.md`. Approved baseline deviations still hold: Docker Engine 29.3.1 / Compose 5.1.1 (D-02), Ubuntu OpenJDK 21.0.12.1 (D-03), `vite` 6.4.5 (D-04), cloc image 2.10 reporting 1.98 (D-07), OSS Index disabled (D-08).

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. Run label `tjan/single-agent/sdd/v002/run-2-search`; run id `tjan-single-agent-sdd-v002-run-2-search-opus-5-5-t1.8-20261028T2024Z-dd7d` from `stat.mjs init --force` in the interview |
| Platforms and tools at the listed versions | `tech-stack.md` | Pass with the approved deviations above. Node v24.13.0, npm 11.6.2 (nvm); OpenJDK 21.0.12.1; Maven 3.9.9 via `./mvnw -v`; Docker 29.3.1, Compose 5.1.1; Playwright 1.63.0; semgrep 1.177.0, gitleaks v8.30.1, cloc 1.98 run as containers |
| Local services running or reachable | `environments.md` | Pass. Docker daemon reachable; the local stack (`COMPOSE_PROJECT_NAME=registration-sdd-run2`) built and became healthy (`out/logs/cr-001_00_stack-up.log`); Testcontainers started PostgreSQL and Mailpit for the backend suite |
| Secrets present in `.env` | `secrets.env.example` | Pass. `sed -n 's/^KEY=//p' .env \| tr -d ... \| grep -q .` for `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY`; second method: key names listed with values cut off; `.env` ignored by git |
| Every listed dependency resolves | `tech-stack.md` | Pass. 60 entries: Maven `.pom` HTTP 200, `npm view`, `docker manifest inspect` (`out/logs/cr-001_00_resolve.txt`) |
| No Critical or High vulnerability | `tech-stack.md` | Pass. npm audit: 5 Moderate (accepted in D-05, D-06), 0 with `--omit=dev` (`cr-001_00_npm-audit.json`). OWASP Dependency-Check 12.1.0 online with NVD: 67 dependencies, 0 vulnerabilities after the baseline suppressions (D-09, D-10) (`cr-001_00_dependency-check.json`) |
| Clean working tree on the starting commit | repository | Pass. Branch `tjan/single-agent/sdd/v002/run-2-search` at `7095b20` (the CR commit on top of baseline `4d664d2`); only `03_statistics/run-log.json` modified |
| Baseline check (`change-interview` step 2) | `02_output/docs/`, `run-log.json` at `4d664d2` | Pass after D-22. Phase 7 complete; baseline run-log has `end`, `finalTestRun.failed` 0, gate met, no open Critical/High; D-01..D-18 all resolved or pending review; acceptance manifest (26 files) matches; input manifest matches except `run-config.md` (ignored by the check) and three statistics tools (D-22); run label differs from `tjan/single-agent/sdd/v002/run-2` |
| CR lint | `lint-change.mjs --signed` | Pass. 0 errors, 1 warning (30 criteria in one CR, approved in the interview); `input.sha256` matches both CR files |
| Baseline test suite | full suite at `7095b20` | Pass. 380 passed, 0 failed: backend 301 (acceptance 89, unit 201, integration 5, architecture 6; `cr-001_00_backend-baseline-run.log`), frontend 74 (acceptance 7, unit/component 67; `cr-001_00_frontend-baseline-run.log`), e2e 5 (`cr-001_00_e2e-baseline-run.log`) |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 53 files: the 51 of the baseline manifest (recomputed) plus `cr/change.md` and `cr/acceptance-criteria.md`, LF-normalised |
