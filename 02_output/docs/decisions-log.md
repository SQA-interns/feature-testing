# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Docker Engine 29.8.2 installed instead of 29.8.1
- Timestamp: 2026-10-20T18:25:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker version` reports Server 29.8.2; `tech-stack.md` pins `docker` 29.8.1. All pinned images pull and run (`out/logs/phase0-image-pull.txt`). Matches `pre-approved.md` PA-05 (patch release of the pinned minor version).
- Options: 1. apply PA-05: approve 29.8.2 for this run, `tech-stack.md` unchanged (default); 2. ask the human to install 29.8.1
- Human response: none (pre-approved, PA-05)
- Resolution: option 1

## D-02: Host JDK is Oracle JDK 21.0.11+9, not Eclipse Temurin 21.0.10+7
- Timestamp: 2026-10-20T18:25:00Z
- Phase: 0
- Type: blocking
- Trigger: `java -version` reports Oracle Java SE 21.0.11+9-LTS-211 (`JAVA_HOME` = `C:\Program Files\Java\jdk-21.0.11`); `tech-stack.md` pins platform `java` = Eclipse Temurin JDK 21.0.10+7. The patch difference alone matches PA-05, but PA-05 does not cover a different JDK vendor. The container runtime uses the pinned `eclipse-temurin:21.0.10_7-jre-alpine` image, which runs and reports Temurin-21.0.10+7.
- Options: 1. approve Oracle JDK 21.0.11 on the host for building and testing in this run; shipped images keep the pinned Temurin base; `tech-stack.md` unchanged (proposed default); 2. the human installs Eclipse Temurin JDK 21.0.10+7 (or another 21.0.x Temurin patch under PA-05) and points `JAVA_HOME` at it
- Human response: option 1 approved (2026-10-20T19:01Z)
- Resolution: option 1

## D-03: Docker Compose 5.6.0 installed instead of 5.5.1
- Timestamp: 2026-10-20T18:25:00Z
- Phase: 0
- Type: blocking
- Trigger: `docker compose version` reports v5.6.0 (bundled with Docker Desktop 29.8.2); `tech-stack.md` pins `compose` 5.5.1. This is a minor-version difference and Compose is not listed in PA-05.
- Options: 1. approve Compose 5.6.0 for this run; `tech-stack.md` unchanged; the compose file uses only features available in 5.5.1 (proposed default); 2. the human installs Docker Compose 5.5.1
- Human response: option 1 approved (2026-10-20T19:01Z)
- Resolution: option 1

## D-04: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-20T18:31:00Z
- Phase: 0
- Type: non-blocking
- Trigger: the first `dependency-check-maven` 12.1.0 run failed: Sonatype OSS Index answers HTTP 401 (it needs an account that `secrets.env.example` does not list) (`out/logs/phase0-backend-depcheck.log`).
- Options: 1. disable only the OSS Index analyser (`ossindexAnalyzerEnabled=false` in `backend/pom.xml`) and keep the NVD analyser with `NVD_API_KEY` (default, required by `general/skills/preflight` step 6); 2. ask for OSS Index credentials
- Human response: none (rule in `general/skills/preflight` step 6)
- Resolution: option 1; the re-run completed with the NVD analyser

## D-05: Pre-approved false positives CVE-2025-7962 and CVE-2025-15104 suppressed
- Timestamp: 2026-10-20T18:33:00Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High by `severity-scale.md`) on `angus-activation-2.0.3.jar` and CVE-2025-15104 (CVSS 5.3) on `hibernate-validator-9.1.3.Final.jar`. Evidence: the dependency tree ships `org.eclipse.angus:angus-mail:2.0.5` (PA-01: fixed from 2.0.4); CVE-2025-15104 concerns the Nu Html Checker, CPE `validator:validator` (PA-02).
- Options: 1. apply PA-01 and PA-02: classify Low, false positive; suppress each CVE for its one artifact in `backend/dependency-check-suppressions.xml` (default); 2. treat as open findings
- Human response: none (pre-approved, PA-01, PA-02)
- Resolution: option 1; re-run: 0 open, 2 suppressed (`out/logs/phase0-dependency-check-report.json`)

## D-06: npm audit Moderate advisories on qs via Stryker accepted
- Timestamp: 2026-10-20T18:34:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 Moderate advisories (`qs` 6.15.1 via `typed-rest-client` 2.3.1 via `@stryker-mutator/core` 10.0.0); `npm audit --omit=dev` reports 0 (`out/logs/phase0-frontend-audit.txt`). Matches PA-03.
- Options: 1. apply PA-03: accept, record as Medium, accepted (default); 2. override the transitive version
- Human response: none (pre-approved, PA-03)
- Resolution: option 1

## D-07: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-20T18:36:00Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run aldanial/cloc:2.10 --version` prints `1.98` (image created 2023-08-19). The pin resolves and runs; `tech-stack.md` rule: another reported version is non-blocking and the pin stays authoritative.
- Options: 1. keep the pinned tag `aldanial/cloc:2.10` and record the reported version next to every cloc figure (default); 2. ask for another tag
- Human response: none (`tech-stack.md` rule)
- Resolution: option 1

## D-08: Maximum length of text fields
- Timestamp: 2026-10-20T19:05:00Z
- Phase: 1
- Type: non-blocking
- Trigger: G-1 in `docs/01_requirements-check.md`: BR-01/BR-02 define required fields but no maximum length; SR-03 limits only the body size. A participant would notice a rejection of a long value.
- Options: 1. limit every text field after trimming to 200 characters and the email to 254 (RFC 5321), rejected with a field error (proposed default, conservative: bounded storage, export and email content); 2. no per-field limit, only the body-size limit
- Human response: none
- Resolution: pending review (option 1 applied)

## D-09: Definition of a valid email format
- Timestamp: 2026-10-20T19:05:00Z
- Phase: 1
- Type: non-blocking
- Trigger: G-2 in `docs/01_requirements-check.md`: BR-03 requires "a valid email format" without defining it. The address is used as an SMTP recipient (US-006), so a lax rule lets undeliverable or header-breaking values through.
- Options: 1. after trimming: one `@`, a non-empty local part without whitespace or control characters, a domain of dot-separated labels with at least one dot, at most 254 characters (proposed default, conservative); 2. Bean Validation `@Email` only (accepts `a@b`)
- Human response: none
- Resolution: pending review (option 1 applied)

## D-10: Default rate limit of 60 requests per minute per client and endpoint
- Timestamp: 2026-10-20T19:07:09Z
- Phase: 2
- Type: non-blocking
- Trigger: `api-contract.md` leaves the `RATE_LIMIT_PER_MINUTE` default to the agent (SR-03). Too low a value locks out legitimate users behind one shared address (for example a class of students on one network, a High example in `severity-scale.md`); too high weakens SB-06.
- Options: 1. 60 per minute per client address and endpoint, fixed window, 429 with `Retry-After` (proposed default); 2. 10 per minute (stricter, risks locking out shared networks); 3. 300 per minute
- Human response: none
- Resolution: pending review (option 1 applied)

## D-11: Local compose stack runs with ORGANIZER_HTTPS_ONLY=false by default
- Timestamp: 2026-10-20T19:07:09Z
- Phase: 2
- Type: non-blocking
- Trigger: SR-06 and `environments.md` allow HTTPS-only to be off only on local plain HTTP. With Docker Desktop port publishing the backend sees the Docker gateway address, not a loopback address, so with the setting on, the local export (E2E-05, DoD-P04) would always be refused. The backend's own default stays `true` and production refuses to start with `false`.
- Options: 1. `docker-compose.yml` passes `${ORGANIZER_HTTPS_ONLY:-false}` (local only, ports bound to 127.0.0.1), overridable at `docker compose up` (proposed default); 2. default `true` in compose too, export refused locally
- Human response: none
- Resolution: pending review (option 1 applied)

## D-12: Add eclipse-temurin:21.0.10_7-jdk-alpine as the backend image build stage
- Timestamp: 2026-10-20T19:07:09Z
- Phase: 2
- Type: non-blocking
- Trigger: `tech-stack.md` lists only the JRE image for the backend. Without a JDK build stage, `docker compose up --build` from a clean checkout needs a host build first (DoD-08). `tech-stack.md` allows an unlisted tool with an exact version and a non-blocking record; it is the same Temurin release as the pinned platform and runtime image (GPL-2.0-with-classpath-exception, build stage only, not shipped).
- Options: 1. multi-stage Dockerfile with `eclipse-temurin:21.0.10_7-jdk-alpine` (pulls and reports Temurin-21.0.10+7) as build stage (proposed default); 2. copy a host-built jar into the JRE image
- Human response: none
- Resolution: option 1 (scanned in phase 6 like any other image)

## D-13: Add @types/node 24.19.2 for the end-to-end test helpers
- Timestamp: 2026-10-20T19:41:28Z
- Phase: 3
- Type: non-blocking
- Trigger: the Playwright e2e helpers use Node APIs (`child_process`, `zlib`, `Buffer`, `process.env`); `tsc --noEmit` needs their type declarations, and `tech-stack.md` lists none. `tech-stack.md` allows dev-only tooling with an exact version and a non-blocking record.
- Options: 1. add `@types/node` 24.19.2 (MIT, matches Node 24, published 2026-10-09) as an exact dev dependency (default); 2. exclude `e2e/` from the type check
- Human response: none
- Resolution: option 1 (dev only, not in the bundle; `npm audit --omit=dev` still 0)
