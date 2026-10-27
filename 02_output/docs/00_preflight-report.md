# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit left empty and recorded by `stat.mjs init` (run id `tjan-single-agent-sdd-v002-run-2-opus-5-5-t1.8-20261027T0947Z-dda4`, commit `12e3255`) |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass with approved deviations. Node 24.13.0 / npm 11.6.2 (installed with nvm, selected with `nvm use`; `02_output/frontend/.nvmrc` names 24.13.0). JDK Ubuntu OpenJDK 21.0.12.1 approved (D-03). Docker Engine 29.3.1 and Compose 5.1.1 approved (D-02). Maven 3.9.9 via wrapper 3.3.2 (`./mvnw -v`). Tool containers run: gitleaks v8.30.1, semgrep 1.177.0, cloc tag 2.10 reports 1.98 (D-07). Playwright 1.63.0 with its bundled Chromium (revision 1243 already cached; nothing installed) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker Desktop daemon reachable (`docker info`); Docker Hub, Maven Central and the npm registry reachable. Mailpit and PostgreSQL run as pinned containers (images resolve). Docker Desktop (context `desktop-linux`, socket `$HOME/.docker/desktop/docker.sock`); Testcontainers 2.0.5 finds it with no overrides (probe outside the repository started `postgres:16.15-alpine` and its cleanup container) |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass after D-01. `sed -n 's/^KEY=//p' .env \| tr -d ...` \| `grep -q .` per key: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (length >= 16), `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty; second method: key names listed with values cut off. `.env` ignored by git (`git check-ignore`). Not required: `RECAPTCHA_*` (test-mode only), `SMTP_*` (not needed) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Maven: HTTP 200 on each `.pom` in Maven Central (incl. `apache-maven` 3.9.9, `pitest-junit5-plugin` 1.2.3). npm: `npm view <pkg>@<version> version`. Images: `docker manifest inspect` |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass after decisions. npm (`npm audit` on the committed lock file, `out/logs/00_frontend-audit.txt`): `vite` 6.4.3 High resolved by pinning 6.4.5 (D-04); 5 Moderate remain, accepted (D-05 PA-03, D-06); `--omit=dev`: 0. Maven (OWASP Dependency-Check 12.1.0, NVD analyser, 67 dependencies, `out/logs/00_dependency-check.json`): OSS Index disabled (D-08); CVE-2025-7962 on angus-activation (D-09, PA-01) and CVE-2025-15104 on hibernate-validator (D-10, PA-02) suppressed as Low; nothing else reported |
| Clean working tree on the starting commit | repository | Pass. `git status` clean apart from `03_statistics/run-log.json`; branch `tjan/single-agent/sdd/v002/run-2` at `12e3255` |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 51 files: every tracked file under `01_input/` and the protected root files of `README.md` sections 1 and 2, LF-normalised (`tr -d '\r' \| sha256sum`); no input modified or untracked (`git diff`, `git ls-files --others`) |

## Dependency results

Method: Maven, HTTP HEAD on the artifact's `.pom` in https://repo.maven.apache.org/maven2; npm, `npm view`; containers, `docker manifest inspect`. Vulnerabilities: npm, `npm audit` on a lock file generated from the pinned set (`npm install --package-lock-only --ignore-scripts`), output in `out/logs/00_npm-audit.json`; Maven, OWASP Dependency-Check 12.1.0 with the NVD analyser (D-08).

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| spring-boot-starter-parent, -webmvc, -data-jpa, -validation, -mail, -actuator, -security, -flyway, -test, -webmvc-test, spring-boot-maven-plugin | 4.1.1 | yes | none (NVD) |
| flyway-core, flyway-database-postgresql | 12.4.0 | yes | none (NVD) |
| postgresql (JDBC) | 42.7.13 | yes | none (NVD) |
| poi-ooxml | 5.5.1 | yes | none (NVD) |
| tomcat-embed-core | 11.0.26 | yes | none (NVD) |
| log4j-api | 2.26.1 | yes | none (NVD) |
| commons-lang3 | 3.20.0 | yes | none (NVD) |
| testcontainers-junit-jupiter, testcontainers-postgresql | 2.0.5 | yes | none (NVD) |
| archunit-junit5 | 1.3.2 | yes | none (NVD) |
| maven-wrapper (Maven 3.9.9) | 3.3.2 | yes | n/a (tool) |
| spotless-maven-plugin | 2.44.3 | yes | n/a (tool) |
| spotbugs-maven-plugin | 4.10.4.1 | yes | n/a (tool) |
| maven-pmd-plugin | 3.26.0 | yes | n/a (tool) |
| jacoco-maven-plugin | 0.8.12 | yes | n/a (tool) |
| pitest-maven (+ pitest-junit5-plugin 1.2.3) | 1.30.0 | yes | n/a (tool) |
| dependency-check-maven | 12.1.0 | yes | n/a (tool) |
| react, react-dom | 19.3.0 | yes | none |
| @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react | 16.3.3 | yes | none |
| @testing-library/jest-dom | 6.9.1 | yes | none |
| jsdom | 26.1.0 | yes | none |
| globals | 15.15.0 | yes | none |
| vite | 6.4.3 → 6.4.5 (D-04) | yes | 6.4.3 High; 6.4.5 none |
| typescript | 5.9.3 | yes | none |
| eslint, @eslint/js | 9.39.5 | yes | none |
| typescript-eslint | 8.70.1 | yes | none |
| eslint-plugin-react-hooks | 5.2.0 | yes | none |
| eslint-plugin-react-refresh | 0.4.26 | yes | none |
| prettier | 3.9.9 | yes | none |
| vitest, @vitest/coverage-v8 | 4.1.11 | yes | Medium (D-06) |
| @playwright/test | 1.63.0 | yes | none |
| @stryker-mutator/core, @stryker-mutator/vitest-runner | 10.0.0 | yes | Medium via `qs` (D-05, PA-03) |
| jscpd | 5.4.0 | yes | none |
| postgres | 16.15-alpine | yes | not scanned (image) |
| eclipse-temurin | 21.0.10_7-jre-alpine | yes | not scanned (image) |
| node | 24.13.0-alpine | yes | not scanned (image) |
| nginx | 1.30.5-alpine | yes | not scanned (image) |
| axllent/mailpit | v1.31.1 | yes | not scanned (image) |
| semgrep/semgrep | 1.177.0 | yes, runs | n/a (tool) |
| zricethezav/gitleaks | v8.30.1 | yes, runs | n/a (tool) |
| aldanial/cloc | 2.10 | yes, runs (reports 1.98, D-07) | n/a (tool) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-27T09:53Z | `.env` with five keys (D-01); Docker Engine/Compose versions (D-02); JDK vendor (D-03); `vite` High (D-04); optional `vitest` 4.1.12 (D-06) | D-01 .env filled; D-02, D-03 approved; D-04 option 1; D-06 keep 4.1.11 (2026-10-27T10:06Z) | all checks re-run and pass |

## Component bootstrap

| Component | Build file | Checks run | Result |
|---|---|---|---|
| backend (`02_output/backend`) | `pom.xml`, Maven wrapper 3.3.2 | `./mvnw -B spotless:check pmd:check verify spotbugs:check`; `dependency:list` compared with `tech-stack.md` | Pass; resolved versions match the pins (`out/logs/00_backend-dependency-list.txt`) |
| frontend (`02_output/frontend`) | `package.json`, `package-lock.json` | `npm run check` (prettier, eslint, tsc), `npm run build`, `npm test`; lock file compared with `package.json` | Pass; every top-level package in the lock file has the exact pinned version |
