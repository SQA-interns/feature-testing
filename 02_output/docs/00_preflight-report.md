# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. Every line filled; Run ID and Starting commit left empty and recorded by `stat.mjs init` (run id `tanej-single-agent-sdd-v001-run-2-opus-5-5-t1.7-ft-20261021T1103Z-01db`, commit `3572c92`). |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass with decisions. JDK 21.0.11 and Docker Engine 29.8.2 are patch releases (D-01, PA-05); Docker Compose 5.6.0 approved by the human (D-08, D-09); cloc image `2.10` reports 1.98 (D-02). Every other platform and tool reports the pinned version (table below). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker Engine running (linux containers); Docker Hub, Maven Central and the npm registry reachable; every pinned image pulled; Mailpit started with `docker compose up --wait` (health check passed, API HTTP 200) and removed; Playwright Chromium revision 1243 present in the local cache; Testcontainers uses the same Docker Engine. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (length >= 16 checked without display), `ORGANIZER_EMAILS`, `NVD_API_KEY` are non-empty. `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` (test-mode only) and `SMTP_USERNAME`, `SMTP_PASSWORD` (not needed) are empty, as allowed. Method: `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` per key; second method: key list with masked values. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Maven: `mvnw dependency:tree` resolves every pinned artifact at its version (`out/logs/00_backend-dependency-tree.txt`); Maven plugins resolve (`help` goals, `dependency:get`). npm: `npm view <pkg>@<version>` and `npm install` with the committed lock (`npm ls --depth=0`). Images: `docker pull` (`out/logs/00_image-pull.log`). |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass with decisions. Backend: OWASP Dependency-Check 12.1.0 with NVD (OSS Index disabled, D-03) scanned 67 dependencies, `failBuildOnCVSS=7`, BUILD SUCCESS; CVE-2025-7962 (D-04, PA-01) and CVE-2025-15104 (D-05, PA-02) suppressed per artifact. Frontend: `npm audit` High on vite 6.4.3 (D-07) fixed by vite 6.4.4 (D-09); remaining Moderate advisories on `qs` reach only `@stryker-mutator/core` (D-06, PA-03); `npm audit --omit=dev`: 0 vulnerabilities. |
| Clean working tree on the starting commit | repository | Pass. Branch `tanej/single-agent/sdd/v001/run-2` at `3572c92` (`git rev-parse HEAD`), `git status --porcelain --ignored` showed only the ignored `.env` and the new `03_statistics/run-log.json`. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 41 files (`01_input/**`, `AGENTS.md`, `README.md`, `Run_Guide.md`, `03_statistics/usage.md`, `metrics.md`, `run-log.template.json`, `03_statistics/tools/*`), SHA-256 of LF-normalised content (`tr -d '\r' \| sha256sum`). |

## Platforms and tools

| id | Pinned | Reported | Method |
|---|---|---|---|
| java | 21.0.10+7 | 21.0.11+9 (Oracle HotSpot) | `java -version`; D-01 |
| node | 24.13.0 | v24.13.0 | `node --version` |
| npm | 11.6.2 | 11.6.2 | `npm --version` |
| docker | 29.8.1 | 29.8.2 | `docker version`; D-01 |
| compose | 5.5.1 | v5.6.0 | `docker compose version`, `--short`; D-08, D-09 |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | wrapper script 3.3.2, Apache Maven 3.9.9 | `mvnw -v`, `mvnw.cmd -v`; SHA-1 of the wrapper zip and SHA-256 of the Maven zip verified |
| spring-boot-maven-plugin | 4.1.1 | 4.1.1 | `package` |
| spotless-maven-plugin | 2.44.3 | 2.44.3 | `spotless:check` |
| spotbugs-maven-plugin | 4.10.4.1 | 4.10.4.1 | `spotbugs:check` (0 bugs) |
| maven-pmd-plugin | 3.26.0 | 3.26.0 (PMD 7.7.0) | `pmd:check pmd:cpd-check` |
| jacoco-maven-plugin | 0.8.12 | 0.8.12 | `jacoco:help` |
| pitest-maven | 1.30.0 (+ junit5 plugin 1.2.3) | 1.30.0, 1.2.3 | `pitest:help`, `dependency:get` |
| dependency-check-maven | 12.1.0 | 12.1.0 | `check` |
| vite | 6.4.3 → 6.4.4 (D-09) | 6.4.4 | `npm ls`, `npm run build` |
| typescript | 5.9.3 | 5.9.3 | `tsc --version` |
| eslint, @eslint/js | 9.39.5 | 9.39.5 | `eslint --version` |
| typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh | 8.70.1, 5.2.0, 0.4.26 | same | `npm ls --depth=0` |
| prettier | 3.9.9 | 3.9.9 | `prettier --version` |
| vitest, @vitest/coverage-v8 | 4.1.11 | 4.1.11 | `vitest --version`, `npm ls` |
| @playwright/test | 1.63.0 | 1.63.0, Chromium 1243 cached | `playwright --version`, `install --dry-run` |
| @stryker-mutator/core, vitest-runner | 10.0.0 | 10.0.0 | `stryker --version`, `npm ls` |
| jscpd | 5.4.0 | 5.4.0 | `jscpd --version` |
| npm audit | 11.6.2 | 11.6.2 | `npm audit` |
| semgrep/semgrep | 1.177.0 | 1.177.0 | `semgrep --version` in the image |
| zricethezav/gitleaks | v8.30.1 | v8.30.1 | `gitleaks version` in the image |
| aldanial/cloc | 2.10 | 1.98 | `cloc --version` in the image; D-02 |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| spring-boot-starter-parent and the eight Boot starters (webmvc, data-jpa, validation, mail, actuator, security, flyway; test, webmvc-test) | 4.1.1 | yes | none after suppressions; transitive `angus-activation` 2.0.3 CVE-2025-7962 Low false positive (D-04), `hibernate-validator` 9.1.3 CVE-2025-15104 Low false positive (D-05) |
| org.flywaydb:flyway-core, flyway-database-postgresql | 12.4.0 | yes | none |
| org.postgresql:postgresql | 42.7.13 | yes | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes | none |
| org.testcontainers:testcontainers-junit-jupiter, testcontainers-postgresql | 2.0.5 | yes | none |
| com.tngtech.archunit:archunit-junit5 | 1.3.2 | yes | none |
| react, react-dom, @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react, @testing-library/jest-dom | 16.3.3, 6.9.1 | yes | none |
| jsdom, globals | 26.1.0, 15.15.0 | yes | none |
| vite (tooling) | 6.4.3 | yes | High GHSA-587h-7wmj-h5pm (D-07) → replaced by 6.4.4: none (D-09) |
| @stryker-mutator/core (tooling) | 10.0.0 | yes | Moderate via `qs` 6.15.1 (D-06, PA-03, accepted) |
| postgres | 16.15-alpine | yes | not scanned (image) |
| eclipse-temurin | 21.0.10_7-jre-alpine | yes | not scanned (image) |
| node | 24.13.0-alpine | yes | not scanned (image) |
| nginx | 1.30.5-alpine | yes | not scanned (image) |
| axllent/mailpit | v1.31.1 | yes | not scanned (image) |

All licences are in the allowed list (`npm view <pkg> license`; Maven licences as listed in `tech-stack.md`).

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-21T11:19:49Z | D-07: vite 6.4.3 High advisory; D-08: Docker Compose 5.6.0 instead of 5.5.1 | 11:23:42Z: vite 6.4.4; approve Compose 5.6.0 (D-09) | vite 6.4.4 installed, `npm audit --audit-level=high` exit 0, check and build pass; Compose already verified |
