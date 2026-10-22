# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. Every line filled; Run ID and Starting commit empty as required; `stat.mjs init` recorded run id `tanej-single-agent-sdd-v001-run-2-search-opus-5-5-t1.7-ft-20261022T1806Z-c947` and commit `6917289`. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass (D-01 approved). Node 24.13.0 and npm 11.6.2 exact. Docker Engine 29.8.2 and JDK 21.0.11 are patch releases (PA-05, D-03). Docker Compose v5.6.0 (minor differs) and the JDK distribution (Oracle, not Temurin) approved for this run by D-01; both run. Every tooling entry runs at its pin (method below); cloc reports 1.98 for tag 2.10 (D-04). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker daemon reachable; every pinned image pulled from Docker Hub; Compose smoke test with Mailpit returned HTTP 200 on its API; Maven Central and the npm registry reachable. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters), `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty. `RECAPTCHA_*` (test-mode only) and `SMTP_*` (not needed) empty, as allowed. `.env` is git-ignored. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. All Maven dependencies and plugins resolve from Maven Central at the pinned versions (`mvnw dependency:resolve dependency:resolve-plugins`, `dependency:list`); all npm packages install at the pinned versions (`npm ls --depth=0`); all images pull. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass (D-02). Backend: two findings, both pre-approved and suppressed (PA-01, PA-02, D-06); re-scan clean. Frontend: vite 6.4.3 High (dev server only) fixed by moving to 6.4.4 (D-02); re-audit: 0 Critical, 0 High; Moderate advisories via Stryker (PA-03, D-07) and vitest (D-08). `npm audit --omit=dev`: 0. |
| Clean working tree on the starting commit | repository | Pass. Clean at start on branch `tanej/single-agent/sdd/v001/run-2-search`, commit `6917289`, apart from the untracked, ignored `.env` and the new `03_statistics/run-log.json`. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 41 files (everything under `01_input/` plus the protected root and statistics files of README sections 1 and 2), LF-normalised, `sha256sum` format; `sha256sum --check` passes. |

## Methods

| Check | First method | Second method |
|---|---|---|
| JDK | `java -version` → 21.0.11+9-LTS (Oracle) | Maven `mvnw -v`, build runs on it |
| Node, npm | `node --version` → v24.13.0, `npm --version` → 11.6.2 | `vite --version` prints node-v24.13.0 |
| Docker Engine, Compose | `docker version` → 29.8.2; `docker compose version` → v5.6.0 | Compose smoke test (`up -d`, HTTP 200, `down`) |
| Maven wrapper | `mvnw` script header 3.3.2; `mvnw -v` → Apache Maven 3.9.9 | full build with it |
| Backend plugins | `mvnw compile pmd:check pmd:cpd-check spotbugs:check jacoco:prepare-agent pitest:help` (PMD 7.7.0 engine) → BUILD SUCCESS; `spotless:check package` → BUILD SUCCESS; Dependency-Check 12.1.0 ran | plugin versions in the Maven log lines |
| Frontend tools | `vite`, `tsc`, `eslint`, `prettier`, `playwright`, `stryker`, `jscpd` `--version` print the pinned versions; `npm run check` and `npm run build` pass; Chromium for Playwright 1.63.0 (build 1243) present | `npm ls --depth=0` |
| Container tools | `semgrep --version` → 1.177.0; `gitleaks version` → v8.30.1; `cloc --version` → 1.98 | `docker image inspect` of the cloc tag |
| Images | `docker pull` of every pinned image | `postgres --version` 16.15, `java -version` 21.0.10 in the JRE image, `nginx -v` 1.30.5, `node --version` 24.13.0, `mailpit version` v1.31.1 |
| Secrets | `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` per key | `git check-ignore -v .env` (ignored); value length check for the organizer password without printing |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| Maven: spring-boot-starter-parent, -webmvc, -data-jpa, -validation, -mail, -actuator, -security, -flyway | 4.1.1 | yes | none |
| org.flywaydb:flyway-core, flyway-database-postgresql | 12.4.0 | yes | none |
| org.postgresql:postgresql | 42.7.13 | yes | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes | none |
| transitive org.eclipse.angus:angus-activation | 2.0.3 | yes | CVE-2025-7962 High → Low, false positive (PA-01, D-06) |
| transitive org.hibernate.validator:hibernate-validator | 9.1.3.Final | yes | CVE-2025-15104 Medium → Low, false positive (PA-02, D-06) |
| spring-boot-starter-test, -webmvc-test | 4.1.1 | yes | none |
| org.testcontainers:testcontainers-junit-jupiter, -postgresql | 2.0.5 | yes | none |
| com.tngtech.archunit:archunit-junit5 | 1.3.2 | yes | none |
| npm: react, react-dom, @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react / jest-dom | 16.3.3 / 6.9.1 | yes | none |
| jsdom / globals | 26.1.0 / 15.15.0 | yes | none |
| vite | 6.4.3 → 6.4.4 (D-02) | yes | 6.4.3: High, dev server only; 6.4.4: none |
| vitest, @vitest/coverage-v8 | 4.1.11 | yes | Moderate via @vitest/mocker (D-08) |
| typescript, eslint, @eslint/js, typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh, prettier | as pinned | yes | none |
| @playwright/test | 1.63.0 | yes | none |
| @stryker-mutator/core, vitest-runner | 10.0.0 | yes | Moderate via qs (PA-03, D-07) |
| jscpd | 5.4.0 | yes | none |
| @types/node (added, D-09) | 24.13.6 | yes | none |
| images: postgres, eclipse-temurin, node, nginx, axllent/mailpit, semgrep, gitleaks, cloc | as pinned | yes | not scanned (not in the dependency-scan tools' scope) |

Logs: `02_output/logs/00_backend-*.log`, `02_output/logs/00_frontend-*.log`, `02_output/logs/00_frontend-audit.json`.

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-22T18:16Z | D-01: approve Compose v5.6.0 and the Oracle JDK 21.0.11 build | approve installed (18:42Z) | not needed: both already ran |
| 2026-10-22T18:16Z | D-02: vite 6.4.3 High advisory, proposed move to 6.4.4 | move to 6.4.4 (18:42Z) | `npm audit`: 0 High; `npm run build`, `npm run check` pass |
