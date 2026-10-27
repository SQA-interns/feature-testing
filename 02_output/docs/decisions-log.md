# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: `.env` missing, required secrets absent
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 4. `.env` does not exist at the repository root (checked with `ls -la` and `test -e .env`). Keys marked "provided" in `project/00_setup/secrets.env.example` therefore have no value: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY`. Without `NVD_API_KEY` the backend dependency scan (OWASP Dependency-Check, preflight step 6) cannot run.
- Options: 1. (proposed) Human copies `secrets.env.example` to `.env` and fills the five keys above (the reCAPTCHA and SMTP keys may stay empty: test-mode only / not needed); 2. none: the run cannot pass phase 0 without them.
- Human response: "updated .env" (2026-10-27T10:06:23Z); re-check: all five keys non-empty, organizer password at least 16 characters, `.env` ignored by git
- Resolution: option 1

## D-02: Docker Engine and Compose below the pinned minor versions
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 2. `tech-stack.md` pins Docker Engine 29.8.1 and Docker Compose 5.5.1. Installed (Docker Desktop, context `desktop-linux`): Engine server 29.3.1 (`docker version`, `docker info --format '{{.ServerVersion}}'`), Compose 5.1.1 (`docker compose version`, `docker compose version --short`). These are different minor versions, so `pre-approved.md` PA-05 (patch only, and it does not list Compose) does not apply. Every pinned image resolves and the tool containers (gitleaks, semgrep, cloc) run on the installed versions.
- Options: 1. (proposed) Approve Engine 29.3.1 and Compose 5.1.1 for this run; `tech-stack.md` stays authoritative; 2. Human upgrades Docker Desktop to a release with Engine 29.8.1 and Compose 5.5.1.
- Human response: "lets use these docker versions we have now" (2026-10-27T10:06:23Z)
- Resolution: option 1: Engine 29.3.1 and Compose 5.1.1 approved for this run

## D-03: Host JDK is Ubuntu OpenJDK 21.0.12.1, not Eclipse Temurin 21.0.10+7
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 2. `java -version` and `java -XshowSettings:properties` report `21.0.12.1+1-1-24.04.4-Ubuntu`, vendor Ubuntu; `javac -version` reports 21.0.12.1. The patch difference matches PA-05, but the vendor differs from the pin (Eclipse Temurin), which PA-05 does not cover. No Temurin JDK is installed under `/usr/lib/jvm`. The runtime image `eclipse-temurin:21.0.10_7-jre-alpine` is unaffected.
- Options: 1. (proposed) Approve the installed Ubuntu OpenJDK 21.0.12.1 for host builds and tests in this run (PA-05 for the patch, this record for the vendor); 2. Human installs Eclipse Temurin JDK 21.0.10+7.
- Human response: "same for jdk" (2026-10-27T10:06:23Z)
- Resolution: option 1: Ubuntu OpenJDK 21.0.12.1 approved for host builds in this run

## D-04: High vulnerability in pinned `vite` 6.4.3
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 6. `npm audit` (npm 11.6.2) over the pinned frontend set reports `vite` 6.4.3 as High: GHSA-9jrq-w75r-8gcw (cross-origin script execution through custom middleware), GHSA-rq7h-c2jc-7f22 (files outside `server.fs.allow` served), and launch-editor advisories GHSA-698v-4r3x-3vcv, GHSA-r825-w8mm-5pxw, GHSA-587h-7wmj-h5pm. `npm audit --omit=dev` reports 0. Not covered by `pre-approved.md`. Full output: `out/logs/00_npm-audit.json`. Re-audit with `vite` 6.4.5 (same minor, patch release) reports no High.
- Options: 1. (proposed) Approve pinning `vite` 6.4.5 instead of 6.4.3 for this run; 2. Keep 6.4.3 and lower to Medium with the evidence that vite is build and dev-server tooling only and the production bundle is served by nginx (lowering a High needs this approval).
- Human response: "do what you would recommend" (2026-10-27T10:06:23Z)
- Resolution: option 1: `vite` 6.4.5 replaces 6.4.3 for this run

## D-05: Moderate `qs` advisories through `@stryker-mutator/core` (PA-03)
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports `qs` 6.15.1 (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) and `typed-rest-client` 2.3.1 as Moderate; the lock file shows `typed-rest-client` required only by `@stryker-mutator/core` 10.0.0. `npm audit --omit=dev` reports none. Matches `pre-approved.md` PA-03.
- Options: 1. (applied) Accept per PA-03, recorded as Medium, accepted.
- Human response: none (pre-approved, PA-03)
- Resolution: option 1

## D-06: Moderate advisories in `vitest` / `@vitest/mocker` / `@vitest/coverage-v8` 4.1.11
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports `@vitest/mocker` 4.1.11 Moderate (GHSA-qxg6-89mg-rqv8, GHSA-xp26-3454-mjgp), carried to `vitest` and `@vitest/coverage-v8` 4.1.11. Test tooling only; `npm audit --omit=dev` reports none. Not covered by PA-03 (different artifact), but Moderate does not block preflight. 4.1.12 fixes it (verified by re-audit).
- Options: 1. (default) Keep the 4.1.11 pins and record as Medium, accepted (dev-only, no production exploit path); 2. Human approves 4.1.12 for both packages.
- Human response: "use that vitest, even if it has a vulnerability" (2026-10-27T10:06:23Z)
- Resolution: option 1: keep `vitest` and `@vitest/coverage-v8` 4.1.11; Medium, accepted

## D-07: `aldanial/cloc:2.10` image reports cloc 1.98
- Timestamp: 2026-10-27T09:53:04Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run --rm aldanial/cloc:2.10 --version` prints 1.98. The pin resolves and runs; `tech-stack.md` rules make a differing reported version non-blocking.
- Options: 1. (applied) Keep the pinned tag `2.10` as authoritative and record the reported version.
- Human response: none
- Resolution: option 1

## D-08: OSS Index analyser of Dependency-Check disabled
- Timestamp: 2026-10-27T10:13:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Preflight step 6. `dependency-check-maven` 12.1.0 failed: Sonatype OSS Index returned 401 Unauthorized. OSS Index credentials are not listed in `secrets.env.example`. The NVD analyser (with `NVD_API_KEY`) ran.
- Options: 1. (applied) Set `ossindexAnalyzerEnabled=false` in `02_output/backend/pom.xml` and re-run; the scan completes with the NVD analyser.
- Human response: none
- Resolution: option 1

## D-09: CVE-2025-7962 on `angus-activation` 2.0.3 (PA-01)
- Timestamp: 2026-10-27T10:13:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check (NVD) reports CVE-2025-7962, CVSS 7.5 (High by `severity-scale.md`), on `org.eclipse.angus:angus-activation` 2.0.3. The resolved `angus-mail` is 2.0.5 (`out/logs/00_backend-dependency-list.txt`), so the evidence of `pre-approved.md` PA-01 matches.
- Options: 1. (applied) Classify Low, false positive; suppress for that one artifact only (`02_output/backend/dependency-check-suppressions.xml`).
- Human response: none (pre-approved, PA-01)
- Resolution: option 1

## D-10: CVE-2025-15104 on `hibernate-validator` 9.1.3.Final (PA-02)
- Timestamp: 2026-10-27T10:13:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check (NVD) reports CVE-2025-15104, CVSS 5.3, on `hibernate-validator`; a CPE name collision with the Nu Html Checker, matching `pre-approved.md` PA-02.
- Options: 1. (applied) Classify Low, false positive; suppress for that one artifact only.
- Human response: none (pre-approved, PA-02)
- Resolution: option 1

## D-11: Response to organizer credentials over plain HTTP (AC-008-04, SR-06)
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: AC-008-04 requires refusal but does not say how; SR-06 says credentials are never accepted over plain HTTP except on localhost.
- Options: 1. (chosen, conservative) Answer HTTP 403 with a general error and no data, without evaluating the credentials and without a redirect; 2. Redirect (308) to HTTPS, which invites clients to resend the credentials.
- Human response: none
- Resolution: pending review

## D-12: Shipped conference options configuration
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: G-04 in `docs/01_requirements-check.md`; the inputs define the option fields (BR-04, OQ-01) but no shipped option set, while E2E-01 and E2E-02 depend on one.
- Options: 1. (chosen) Ship an example configuration with at least one active option per category available to both types, at least one active option available only to external participants, and one inactive option; the operator replaces it through configuration (AR-04); 2. Ship an empty option set.
- Human response: none
- Resolution: pending review

## D-13: User-facing language
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: G-05; no language is stated for the form, confirmation, errors or emails. The only given wording, the OQ-02 consent text, is English.
- Options: 1. (chosen) English throughout, matching the given consent wording; Slovenian characters are supported in all data (NFR-01); 2. Slovenian user interface, which would need wording nobody has approved.
- Human response: none
- Resolution: pending review

## D-14: Maximum lengths of text fields
- Timestamp: 2026-10-27T10:15:06Z
- Phase: 1
- Type: non-blocking
- Trigger: G-06; no field length is given, but SR-03 needs a body size limit and the database needs column sizes.
- Options: 1. (chosen) 200 characters per name, institution and programme field, 254 for email (RFC 5321 path limit), 50 for student ID, counted after trimming; longer input is rejected with a field error (HTTP 400); request body limit 16 KB; 2. No field limits, body limit only.
- Human response: none
- Resolution: pending review
