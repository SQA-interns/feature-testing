# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `tjan-single-agent-sdd-v001-run-1-search-opus-5-5-t1.7-ft-20261021T1907Z-36e8`, starting commit `06619d7`, checked 2026-10-21 on a Linux host (Ubuntu 24.04, Docker Desktop). This is a change run (D-18): run type `brownfield`, feature `search`, base run `tanej_single-agent_sdd_v001_run-1 @ 84f9bde`. The base run's report (Windows host, 2026-10-20) is in git history; this report replaces it for this run.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit empty as required; `stat.mjs init` recorded them. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass with decisions. Node 24.13.0 and npm 11.6.2 exact. JDK is Ubuntu OpenJDK 21.0.12.1 instead of Temurin 21.0.10+7 (D-19, approved). Docker Engine 29.3.1 instead of 29.8.1 (D-20, approved). Compose 5.1.1 instead of 5.5.1 (D-21, approved). Every tooling entry runs at its pinned version; the cloc image 2.10 reports 1.98 (D-07, unchanged). The Maven wrapper lacked the executable bit (D-23, fixed). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker Desktop running; Docker Hub, Maven Central and npm registry reachable; all nine pinned images pulled and run; Playwright Chromium 1243 present. The local stack runs as compose project `konferenca-us009` because old volumes of the default project hold another database password (D-24). |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (length >= 16), `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty. `RECAPTCHA_*` (test-mode only) empty, as allowed; `SMTP_*` (not needed) present and unused locally. `.env` is ignored by git. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. `dependency:tree` resolves every backend pin (`out/logs/phase0-change-dependency-tree.txt`); `npm ci` installs the committed lock file and `npm ls --depth=0` matches every pin. No dependency or lock file changed since the base run. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass with decisions. Dependency-Check 12.1.0 online (NVD): 0 open, 2 suppressed (PA-01, PA-02, D-05), with smaller NVD pages (D-25). `npm audit --omit=dev`: 0. Full `npm audit`: 2 Moderate `qs` via Stryker (PA-03, D-06) and 1 High on `vite` 6.4.3 (dev server only), lowered to Medium by the human (D-22). |
| Every component builds | `general/phases.md` gate | Pass. Backend `compile spotless:check pmd:check pmd:cpd-check spotbugs:check` clean, `./mvnw verify` 251/251 (`out/logs/phase0-change-backend-check.log`, `phase0-change-backend-verify.log`); frontend `npm run check`, `npm run build`, `npm test` 55/55 (`phase0-change-frontend-check.log`); local stack builds and the 5 base e2e tests pass (`phase0-change-e2e-baseline.log`). |
| Clean working tree on the starting commit | repository | Pass. Clean at `06619d7` apart from `.env` (ignored) and `03_statistics/run-log.json`. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. Same 41 files as the base run, LF-normalised; exactly the 10 files changed for US-009 (and `run-config.md`) have new hashes (D-18). |

## Methods

| Check | First method | Second method |
|---|---|---|
| JDK | `java -version` | `java -XshowSettings:properties` (vendor Ubuntu), `ls /usr/lib/jvm` (no Temurin installed) |
| Docker / Compose | `docker version`, `docker compose version` | `docker info` (server 29.3.1), `docker compose version --short` (5.1.1) |
| Node / npm | `node --version`, `npm --version` | `vite --version` (prints node-v24.13.0) |
| Maven tooling | `./mvnw -v` (Maven 3.9.9) | executed: `compile spotless:check pmd:check pmd:cpd-check spotbugs:check`, `verify` with JaCoCo |
| npm tooling | `npx <tool> --version` for vite, tsc, eslint, prettier, vitest, playwright, stryker, jscpd | `npm ls --depth=0`; `npm run check`, `npm run build`, `npm test` |
| Container tools | `docker pull` (`out/logs/phase0-change-image-pull.txt`) | each image's own version command (Temurin-21.0.10+7, nginx 1.30.5, PostgreSQL 16.15, Mailpit v1.31.1, Semgrep 1.177.0, Gitleaks v8.30.1, cloc 1.98) |
| Secrets | `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` per key | key names listed with values masked; length test for `ORGANIZER_PASSWORD`; values never displayed |
| Dependencies | `dependency:tree`, `npm ci` | `npm ls --depth=0` |

## Dependency results

Unchanged pins since the base run; vulnerabilities re-scanned online with Dependency-Check 12.1.0 (NVD, `NVD_API_KEY`) and `npm audit`.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| backend runtime and test dependencies (all `tech-stack.md` Maven entries) | as listed | yes | none open; 2 suppressed below (D-25: NVD download paging) |
| angus-activation (transitive, mail) | 2.0.3 | yes | CVE-2025-7962, false positive, Low (PA-01, D-05) |
| hibernate-validator (transitive, validation) | 9.1.3.Final | yes | CVE-2025-15104, false positive, Low (PA-02, D-05) |
| react, react-dom and the other frontend runtime and test packages | as listed | yes | none (`npm audit --omit=dev`: 0) |
| `vite` | 6.4.3 | yes | High by npm (dev server, launch-editor), lowered to Medium by the human (D-22) |
| `@stryker-mutator/core` (via `typed-rest-client`, `qs`) | 10.0.0 | yes | Moderate (PA-03, D-06) |
| container images | as listed | yes | not scanned (no image scanner listed) |

## Component commands (ES-05)

Unchanged from the base run.

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -DskipTests package` | `./mvnw verify` | `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-21 | D-19: JDK vendor and version differ from the pin | option 1: Ubuntu OpenJDK 21.0.12.1 approved for host build and test | `java -version` 21.0.12.1, pass |
| 2026-10-21 | D-20, D-21: Docker Engine and Compose minor versions differ from the pins | option 1: Docker 29.3.1 and Compose 5.1.1 approved | `docker version`, `docker compose version`, pass |
| 2026-10-21 | D-22: High npm advisory on the dev-only `vite` 6.4.3 | option 1: lowered to Medium, 6.4.3 kept | `npm audit --omit=dev` 0, pass |
| 2026-10-21 | D-24 (information): old compose volumes | leave them; this run uses project `konferenca-us009` | stack ready, base e2e 5/5 |
