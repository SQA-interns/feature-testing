# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `tanej-single-agent-sdd-v001-run-1-opus-5-5-t1.7-ft-20261020T1823Z-8493`, starting commit `a728ac1`, checked 2026-10-20.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit empty as required; `stat.mjs init` recorded them. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass with decisions. Node 24.13.0 and npm 11.6.2 exact. Docker Engine 29.8.2 (PA-05, D-01). JDK is Oracle 21.0.11+9 instead of Temurin 21.0.10+7 (D-02, blocking). Compose 5.6.0 instead of 5.5.1 (D-03, blocking). Every tooling entry runs at its pinned version; cloc image 2.10 reports 1.98 (D-07). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker Desktop running; Docker Hub, Maven Central and npm registry reachable; all eight pinned images pulled and started; Playwright Chromium 1243 present. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (length >= 16), `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty. `RECAPTCHA_*` (test-mode only) and `SMTP_*` (not needed) empty, as allowed. `.env` is ignored by git. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Every Maven POM answers 200 on Maven Central; every npm version resolves with its licence; the resolved backend tree and `npm ls` match the pins. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass with decisions. Dependency-Check: 0 open, 2 suppressed (PA-01, PA-02, D-05) after disabling OSS Index (D-04). `npm audit --omit=dev`: 0; full `npm audit`: 2 Moderate via Stryker (PA-03, D-06). |
| Clean working tree on the starting commit | repository | Pass. Clean at `a728ac1` apart from `.env` (ignored) and `03_statistics/run-log.json`. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 41 files: all of `01_input/` plus the protected root and `03_statistics/` files of `README.md` sections 1 and 2, LF-normalised. |

## Methods

| Check | First method | Second method |
|---|---|---|
| JDK | `java -version` | `JAVA_HOME` and `which -a java` (only Oracle 21.0.11 and an Oracle 8 JRE installed) |
| Docker / Compose | `docker version`, `docker compose version` | `docker info` (Docker Desktop 29.8.2), `docker compose version --short` |
| Node / npm | `node --version`, `npm --version` | `vite --version` (prints node-v24.13.0) |
| Maven tooling | `./mvnw -v` (Maven 3.9.9), `dependency:resolve-plugins` | executed on the skeleton: `package spotless:check pmd:check pmd:cpd-check spotbugs:check`, `pitest:mutationCoverage`, JaCoCo `prepare-agent` (`out/logs/phase0-backend-build-check.log`, `phase0-backend-pitest.log`) |
| npm tooling | `npx <tool> --version` for vite, tsc, eslint, prettier, vitest, playwright, stryker, jscpd | `npm ls --depth=0`; `npm run build` and `npm run check` on the skeleton |
| Container tools | `docker pull` (`out/logs/phase0-image-pull.txt`) | each image's own version command |
| Secrets | `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` per key | value length test for `ORGANIZER_PASSWORD`; values never displayed |
| Dependencies | HEAD on each POM URL, `npm view <pkg>@<ver> version license` (`out/logs/phase0-dependency-resolve.txt`) | `dependency:tree` (`out/logs/phase0-backend-dependency-tree.txt`), `npm install` |

## Dependency results

Backend versions as resolved (`dependency:tree`); vulnerabilities from Dependency-Check 12.1.0 with NVD (`out/logs/phase0-dependency-check-report.json`). Frontend from `npm audit`.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| spring-boot-starter-parent and the eight runtime starters | 4.1.1 | yes | none |
| spring-boot-starter-test, spring-boot-starter-webmvc-test | 4.1.1 | yes | none (JUnit Jupiter 6.0.3, Mockito 5.23.0) |
| flyway-core, flyway-database-postgresql | 12.4.0 | yes | none |
| postgresql | 42.7.13 | yes | none |
| poi-ooxml | 5.5.1 | yes | none |
| tomcat-embed-core | 11.0.26 | yes | none |
| log4j-api | 2.26.1 | yes | none |
| commons-lang3 | 3.20.0 | yes | none |
| testcontainers-junit-jupiter, testcontainers-postgresql | 2.0.5 | yes | none |
| archunit-junit5 | 1.3.2 | yes | none |
| angus-activation (transitive, mail) | 2.0.3 | yes | CVE-2025-7962 High, false positive, Low (PA-01, D-05) |
| hibernate-validator (transitive, validation) | 9.1.3.Final | yes | CVE-2025-15104 Medium, false positive, Low (PA-02, D-05) |
| react, react-dom, @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react 4.7.0, @testing-library/react 16.3.3, @testing-library/jest-dom 6.9.1, jsdom 26.1.0, globals 15.15.0 | as listed | yes | none |
| npm tooling (vite, typescript, eslint, prettier, vitest, coverage-v8, playwright, stryker, jscpd) | as listed | yes | Moderate `qs` via `@stryker-mutator/core` (PA-03, D-06) |
| container images (postgres, eclipse-temurin, node, nginx, mailpit, semgrep, gitleaks, cloc) | as listed | yes | not scanned (images are scanned in phase 6 only if a tool for it is listed) |

## Component commands (ES-05)

`architecture.md` leaves the commands empty; these are the commands bootstrapped in phase 0.

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -DskipTests package` | `./mvnw verify` | `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-20 | D-02: JDK vendor and version differ from the pin | pending | |
| 2026-10-20 | D-03: Docker Compose minor version differs from the pin | pending | |
