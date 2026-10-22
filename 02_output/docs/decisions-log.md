# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host Docker Compose minor version and JDK distribution differ from the pins
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` pins Docker Compose 5.5.1 and Eclipse Temurin JDK 21.0.10+7. The host has Docker Compose v5.6.0 (`docker compose version`; a different minor version, outside PA-05) and Oracle Java SE 21.0.11+9-LTS (`java -version`; a patch release, but another JDK distribution than Temurin, which PA-05 does not name). Verified: a Compose smoke test (Mailpit container, HTTP 200 on its API) ran; the Maven wrapper build, PMD, CPD, SpotBugs, JaCoCo, PIT and Dependency-Check all ran on the host JDK. The container runtime image stays `eclipse-temurin:21.0.10_7-jre-alpine` (pulled, reports 21.0.10).
- Options: 1. Approve the installed Compose 5.6.0 and Oracle JDK 21.0.11 for this run; `tech-stack.md` stays unchanged (proposed default; everything runs). 2. Install Compose 5.5.1 and Temurin 21.0.10+7 on the host, then re-run the checks.
- Human response: option 1, approve installed (2026-10-22T18:42Z)
- Resolution: option 1

## D-02: vite 6.4.3 has a High advisory
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` (npm 11.6.2) reports vite <= 6.4.3 as High (GHSA-9jrq-w75r-8gcw cross-origin script execution through custom middleware, GHSA-rq7h-c2jc-7f22 `server.fs.allow` bypass, and launch-editor GHSA-698v-4r3x-3vcv, GHSA-r825-w8mm-5pxw, GHSA-587h-7wmj-h5pm). All concern the Vite dev server; `npm audit --omit=dev` reports 0 vulnerabilities (vite is not in the shipped bundle). Not covered by `pre-approved.md`. Evidence: `02_output/logs/00_frontend-audit.json`.
- Options: 1. Move `vite` to 6.4.4, the first fixed patch release (published 2026-10-06), same minor line, compatible with `@vitejs/plugin-react` 4.7.0 and vitest 4.1.11 (proposed default). 2. Keep 6.4.3 and lower the finding to Medium as dev-only tooling. 3. Move to 6.4.5 (published 2026-10-21, one day old).
- Human response: option 1, move to vite 6.4.4 (2026-10-22T18:42Z)
- Resolution: option 1; vite 6.4.4 replaces the 6.4.3 entry of `tech-stack.md` for this run; `npm audit` reports 0 High, 0 Critical; build and check pass

## D-03: Host JDK and Docker Engine patch versions differ (PA-05)
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: Host Docker Engine 29.8.2 (pinned 29.8.1) and JDK 21.0.11 (pinned 21.0.10+7) are patch releases of the pinned minor versions; all pinned images and tools run. Node 24.13.0 and npm 11.6.2 match exactly.
- Options: 1. Apply PA-05: approve the installed patch versions for this run, do not edit `tech-stack.md` (proposed default).
- Human response: none (pre-approved, PA-05)
- Resolution: option 1 (the JDK distribution question is D-01)

## D-04: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: `aldanial/cloc:2.10` resolves and runs, `cloc --version` prints 1.98 (checked twice: `docker run … --version` and image inspection, created 2023-08-19).
- Options: 1. Keep the pinned tag, which stays authoritative, and record the reported version (proposed default).
- Human response: none
- Resolution: option 1

## D-05: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: The Sonatype OSS Index analyser of `dependency-check-maven` 12.1.0 needs OSS Index credentials, which `secrets.env.example` does not list. The scan runs with the NVD analyser (`NVD_API_KEY`) and the remaining default analysers.
- Options: 1. Set `ossindexAnalyzerEnabled=false` in `pom.xml` and scan with the other analysers (proposed default).
- Human response: none
- Resolution: option 1

## D-06: CVE-2025-7962 and CVE-2025-15104 pre-approved (PA-01, PA-02)
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High) on `angus-activation-2.0.3.jar` and CVE-2025-15104 (CVSS 5.3, Medium) on `hibernate-validator-9.1.3.Final.jar`. Evidence matches: the shipped `angus-mail` is 2.0.5 (PA-01); the hibernate-validator match is the `validator:validator` CPE name collision (PA-02).
- Options: 1. Classify both Low, false positive; suppress each for that one artifact in `backend/dependency-check-suppressions.xml` (proposed default, the pre-approved action).
- Human response: none (pre-approved, PA-01, PA-02)
- Resolution: option 1; re-scan reports no vulnerability

## D-07: npm audit Moderate advisories through @stryker-mutator/core (PA-03)
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: `qs` 6.15.1 (three Moderate advisories) and `typed-rest-client` 2.3.1 are reached only through `@stryker-mutator/core` 10.0.0 (`npm ls qs`); `npm audit --omit=dev` reports none.
- Options: 1. Apply PA-03: accept, record as Medium, accepted (proposed default).
- Human response: none (pre-approved, PA-03)
- Resolution: option 1

## D-08: vitest 4.1.11 Moderate advisories (dev-only)
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports two Moderate advisories on `@vitest/mocker` (< 4.1.12), reached through `vitest` and `@vitest/coverage-v8` 4.1.11. Dev and test tooling only; `npm audit --omit=dev` reports none; not reached through Stryker, so PA-03 does not apply. Moderate is not blocking (`preflight` step 6). `Run_Guide`/tech-stack keep vitest on 4.1.11 for Stryker compatibility.
- Options: 1. Keep vitest and `@vitest/coverage-v8` 4.1.11 and record the advisories as Medium, accepted, dev-only (proposed default). 2. Move both to 4.1.12 (a version change, needs approval).
- Human response: none
- Resolution: option 1, pending review

## D-09: @types/node added as dev-only tooling
- Timestamp: 2026-10-22T18:16:42Z
- Phase: 0
- Type: non-blocking
- Trigger: The Playwright configuration and end-to-end tests read `process.env` (stack URLs, organizer credentials from `.env`); `tsc --noEmit` needs Node type declarations, which `tech-stack.md` does not list.
- Options: 1. Add `@types/node` 24.13.6 (MIT, published 2026-09-19, matches the Node 24.13 line), exact, dev-only, under the `tech-stack.md` rule (proposed default).
- Human response: none
- Resolution: option 1

## D-10: Status code for organizer requests refused over plain HTTP
- Timestamp: 2026-10-22T18:43:41Z
- Phase: 1
- Type: non-blocking
- Trigger: AC-008-04 and AC-009-10 (SR-06) require a valid-credential organizer request over plain HTTP from a non-localhost client to be refused, without a status code; `api-contract.md` fixes HTTP 401 only for missing or wrong credentials.
- Options: 1. Answer HTTP 403 with a body without registration data, before the credentials are evaluated, so a refused request never reveals whether the credentials were valid (proposed default, conservative). 2. Answer HTTP 401 as for wrong credentials.
- Human response: none
- Resolution: option 1, pending review

## D-11: Search text length limit is checked before trimming
- Timestamp: 2026-10-22T18:43:41Z
- Phase: 1
- Type: non-blocking
- Trigger: AC-009-08 rejects a search text longer than 100 characters; BR-10 ignores leading and trailing whitespace. Whether the limit applies to the raw or the trimmed text is not stated.
- Options: 1. Check the raw parameter length: more than 100 characters is rejected even if trimming would shorten it (proposed default, stricter). 2. Check the trimmed length.
- Human response: none
- Resolution: option 1, pending review
