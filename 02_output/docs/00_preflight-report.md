# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass: every line filled, Run ID and Starting commit empty; `stat.mjs init` recorded run id and start commit `0d62004` |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass after decisions: JDK Oracle 21.0.11 approved (D-01), Docker Engine 29.8.2 (D-02, PA-05), Compose 5.6.0 approved (D-03), cloc tag reports 1.98 (D-04); everything else matches |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass: Docker Desktop running; Docker Hub, Maven Central and npm registry reachable; PostgreSQL, Mailpit, Temurin, node, nginx images pull and run |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty; `RECAPTCHA_*` test-mode only, `SMTP_*` not needed |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass: all 31 Maven coordinates, 23 npm packages and 8 container images resolve; resolved Maven tree and `npm ls` match the pins |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass after decisions: `vite` 6.4.3 High fixed by approved 6.4.4 (D-05), re-audit 0 High; Moderate dev-only advisories (D-06, D-07); Maven: none after pre-approved suppressions (D-09 PA-01, D-10 PA-02) |
| Clean working tree on the starting commit | repository | Pass: clean on `0d62004` at start (`.env` ignored, `run-log.json` created by `stat.mjs`) |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass: 41 files under `01_input/` plus protected root and `03_statistics/` files, LF-normalised SHA-256 |

## Method

| Check | Method (first / second) |
|---|---|
| Platforms | `java -version`, `node --version`, `npm --version`, `docker version`, `docker compose version` / `JAVA_HOME` and `which -a java`, `docker info`, `./mvnw -v` (Maven 3.9.9 on Java 21.0.11 Oracle) |
| Tooling | backend: `./mvnw -v`, `spotless:check`, `pmd:check pmd:cpd-check spotbugs:check jacoco:help`, `pitest-maven:mutationCoverage`, `dependency-check:check` on the skeleton; frontend: each tool's `--version` plus `npm run check`, `npm run build`, `vitest run`, `jscpd src`; Playwright Chromium 1243 already in the user cache (`playwright install --dry-run`); containers: `docker run <image> --version` |
| Secrets | `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` per key; no value displayed |
| Resolve | Maven Central POM URL returns 200 per coordinate, then `dependency:tree` (`out/logs/00_backend-dependency-tree.txt`); `npm view <pkg>@<ver> version license`, then `npm install` and `npm ls`; `docker manifest inspect`, then `docker run` |
| Vulnerabilities | `npm audit --json` (`out/logs/00_frontend-npm-audit.json`) and `npm audit --omit=dev`; OWASP Dependency-Check 12.1.0 with NVD API key, OSS Index disabled (D-08), NVD page size 100 because 2000-row pages timed out (`out/logs/00_dependency-check.log`, `out/logs/00_dependency-check-report.json`); re-run with suppressions and `-DfailBuildOnCVSS=7` passes (`out/logs/00_dependency-check-suppressed.log`) |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| backend Maven set (21 dependencies, 10 tooling plugins) | as `tech-stack.md` | yes | none after suppressions; raw: CVE-2025-7962 on angus-activation 2.0.3 (PA-01, D-09), CVE-2025-15104 on hibernate-validator 9.1.3.Final (PA-02, D-10) |
| vite | 6.4.4 (approved in D-05; listed 6.4.3) | yes | none (6.4.3: High GHSA-587h-7wmj-h5pm) |
| vitest, @vitest/coverage-v8 | 4.1.11 | yes | Moderate via `@vitest/mocker` (D-06) |
| @stryker-mutator/core | 10.0.0 | yes | Moderate via `qs` (PA-03, D-07) |
| other npm packages (19) | as `tech-stack.md` | yes | none |
| container images (8) | as `tech-stack.md` | yes | not scanned in phase 0 |
| `npm audit --omit=dev` (shipped frontend) | | | none |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-22 phase 0 | JDK vendor/version (D-01), Compose version (D-03), vite High advisory (D-05); optional vitest 4.1.12 (D-06) | D-01 option 1, D-03 option 1, D-05 vite 6.4.4, D-06 keep 4.1.11 (2026-10-22T09:00Z) | `npm audit` with vite 6.4.4: 0 High; `npm run check`, build, vitest pass |
