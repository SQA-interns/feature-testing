# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host JDK is Oracle 21.0.11, not Temurin 21.0.10+7
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: blocking
- Trigger: `java -version` reports Oracle Java HotSpot 21.0.11+9-LTS (JAVA_HOME `C:\Program Files\Java\jdk-21.0.11`); `tech-stack.md` platform `java` pins Eclipse Temurin JDK 21.0.10+7. PA-05 approves a different patch version of the JDK, but the vendor also differs, so the evidence does not match PA-05 exactly. The backend runtime image `eclipse-temurin:21.0.10_7-jre-alpine` runs and reports Temurin-21.0.10+7; the host JDK only builds and tests.
- Options: 1. (default) Approve the installed Oracle JDK 21.0.11 for building and testing in this run (same Java 21 feature release; containers keep the pinned Temurin image); 2. the human installs Temurin JDK 21.0.10+7 and points JAVA_HOME at it.
- Human response: 2026-10-22T09:00Z: approve the installed Oracle JDK 21.0.11 (option 1).
- Resolution: option 1: Oracle JDK 21.0.11 approved for host builds and tests in this run; `tech-stack.md` unchanged, containers keep the Temurin image

## D-02: Docker Engine 29.8.2 instead of 29.8.1
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker version` reports Engine 29.8.2 (Docker Desktop); `tech-stack.md` pins 29.8.1. `project/00_setup/pre-approved.md` PA-05 matches: patch release of the pinned minor 29.8, all pinned images and tools run.
- Options: 1. (default) Apply PA-05: approve 29.8.2 for this run, do not edit `tech-stack.md`; 2. ask for a downgrade.
- Human response: none (pre-approved, PA-05)
- Resolution: option 1

## D-03: Docker Compose 5.6.0 instead of 5.5.1
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: blocking
- Trigger: `docker compose version` reports v5.6.0 (bundled with Docker Desktop); `tech-stack.md` platform `compose` pins 5.5.1. It is a different minor version and PA-05 does not cover Compose, so it is not pre-approved.
- Options: 1. (default) Approve Compose 5.6.0 for this run (the compose file will use only the Compose Specification features also in 5.5.1); 2. the human installs Compose 5.5.1.
- Human response: 2026-10-22T09:00Z: approve Compose 5.6.0 (option 1).
- Resolution: option 1: Compose 5.6.0 approved for this run

## D-04: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run --rm aldanial/cloc:2.10 --version` prints 1.98. The pin resolves and the tool runs; `tech-stack.md` rules make this non-blocking and the listed pin stays authoritative.
- Options: 1. (default) Keep the pinned tag `aldanial/cloc:2.10`; 2. none needed.
- Human response: none
- Resolution: option 1

## D-05: npm audit reports a High advisory on vite 6.4.3
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` on the pinned frontend set reports vite <=6.4.3 as High (GHSA-587h-7wmj-h5pm, launch-editor command execution on Windows; also Moderate GHSA-698v-4r3x-3vcv, GHSA-r825-w8mm-5pxw, GHSA-rq7h-c2jc-7f22 and Low GHSA-9jrq-w75r-8gcw, all in the dev server). vite is a direct dev dependency (tooling `vite`); `npm audit --omit=dev` reports 0 vulnerabilities, so nothing ships in the bundle. Fix available in vite 6.4.5 (patch). Not covered by PA-03 (that is Moderate through `@stryker-mutator/core` only). Evidence: `out/logs/00_frontend-npm-audit.json`.
- Options: 1. (default) Approve vite 6.4.5 in place of 6.4.3 (patch upgrade, same major; fixes all five advisories); 2. keep 6.4.3 and lower the finding to Medium as dev-only (the dev server is used only on 127.0.0.1 during development and is not deployed).
- Human response: 2026-10-22T09:00Z: upgrade to vite 6.4.4, the first patch that fixes GHSA-587h-7wmj-h5pm, not 6.4.5; if 6.4.4 does not clear the High advisory, stop and ask before choosing another version.
- Resolution: vite 6.4.4 replaces 6.4.3 for this run (commit `1bb17ad`); `npm audit` after the change: 0 High, no vite advisory left (`out/logs/00_frontend-npm-audit-vite644.json`); check, build and tests pass

## D-06: npm audit Moderate advisories on vitest 4.1.11 (dev only)
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports Moderate GHSA-qxg6-89mg-rqv8 and GHSA-xp26-3454-mjgp in `@vitest/mocker` <4.1.12, reached through `vitest` and `@vitest/coverage-v8` 4.1.11 (test tooling). `npm audit --omit=dev` reports none. Moderate is not blocking by `general/skills/preflight` step 6; it is not PA-03 (different path).
- Options: 1. (default) Record as Medium, accepted for dev tooling, keep the pinned 4.1.11; 2. upgrade vitest and @vitest/coverage-v8 to 4.1.12 if the human approves it (asked together with D-05).
- Human response: 2026-10-22T09:00Z: keep 4.1.11, accept Medium (option 1).
- Resolution: option 1: Medium, accepted for dev tooling; vitest and @vitest/coverage-v8 stay 4.1.11

## D-07: npm audit Moderate on qs through @stryker-mutator/core
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports Moderate advisories on `qs` 6.15.1 via `typed-rest-client` 2.3.1, reached only through `@stryker-mutator/core` 10.0.0; `npm audit --omit=dev` reports none. Matches `pre-approved.md` PA-03.
- Options: 1. (default) Apply PA-03: accept, record as Medium, accepted.
- Human response: none (pre-approved, PA-03)
- Resolution: option 1

## D-08: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-22T08:21:21Z
- Phase: 0
- Type: non-blocking
- Trigger: the OSS Index analyser of `org.owasp:dependency-check-maven` needs Sonatype credentials that `secrets.env.example` does not list (`general/skills/preflight` step 6). The NVD analyser runs with `NVD_API_KEY`.
- Options: 1. (default) Disable OSS Index (`ossindexAnalyzerEnabled=false` in `backend/pom.xml`) and scan with the NVD-based analysers.
- Human response: none
- Resolution: option 1

## D-09: CVE-2025-7962 on angus-activation (pre-approved)
- Timestamp: 2026-10-22T08:56:36Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High by `severity-scale.md`) on `org.eclipse.angus:angus-activation` 2.0.3. The CVE affects Jakarta Mail before 2.0.2; the resolved tree ships `angus-mail` 2.0.5 (`out/logs/00_backend-dependency-tree.txt`). Matches `pre-approved.md` PA-01.
- Options: 1. (default) Apply PA-01: classify Low, false positive; suppress for that one artifact only (`backend/dependency-check-suppressions.xml`).
- Human response: none (pre-approved, PA-01)
- Resolution: option 1

## D-10: CVE-2025-15104 on hibernate-validator (pre-approved)
- Timestamp: 2026-10-22T08:56:36Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-15104 (CVSS 5.3) on `hibernate-validator` 9.1.3.Final; the CVE concerns the Nu Html Checker (validator.nu), a CPE name collision. Matches `pre-approved.md` PA-02.
- Options: 1. (default) Apply PA-02: classify Low, false positive; suppress for that one artifact only.
- Human response: none (pre-approved, PA-02)
- Resolution: option 1

## D-11: Status code for organizer requests refused by HTTPS-only access
- Timestamp: 2026-10-22T09:09:55Z
- Phase: 2
- Type: non-blocking
- Trigger: AC-008-04 and AC-009-10 require that, with `ORGANIZER_HTTPS_ONLY` on, valid credentials over plain HTTP from a non-local client are "refused" with no data; `api-contract.md` fixes 401 only for missing or wrong credentials and gives no code for this case.
- Options: 1. (default) 403 with error field `protocol`, checked before the credentials, so the credentials are never evaluated over plain HTTP; 2. 401 as for wrong credentials (hides the reason, but suggests the credentials were wrong).
- Human response: none
- Resolution: pending review (option 1 applied in `docs/02_specification.md` 8.2)

## D-12: Specification and contracts committed in three commits
- Timestamp: 2026-10-22T09:09:55Z
- Phase: 2
- Type: non-blocking
- Trigger: `general/phases.md` asks for the specification with all its contracts in one commit; together they exceed the 400-line commit limit of `general/working-rules.md`, which takes precedence (`AGENTS.md`).
- Options: 1. (default) Commit the specification, the JSON contracts, and the Markdown contracts with the validator separately, each under 400 lines.
- Human response: none
- Resolution: option 1

## D-13: SpotBugs false positives excluded by class
- Timestamp: 2026-10-22T10:51:09Z
- Phase: 4
- Type: non-blocking
- Trigger: `spotbugs:check` (effort Max, threshold Low) reports EI_EXPOSE_REP2 for constructor-injected collaborators (ports, EntityManager) in `RegisterParticipant`, `ExportRegistrations`, `SearchRegistrations`, `JpaRegistrationStore`; EI_EXPOSE_REP for the wrapped input stream of `RequestSizeFilter$Limited`; THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION for `SecurityConfiguration` (`HttpSecurity.build()` declares `throws Exception`). The genuine findings (records holding mutable lists) were fixed in code.
- Options: 1. (default) Exclude exactly these patterns for exactly these classes in `backend/spotbugs-exclude.xml`; every other finding still fails the check; 2. copy or wrap injected collaborators (no effect on behaviour, hides the dependency).
- Human response: none
- Resolution: option 1
